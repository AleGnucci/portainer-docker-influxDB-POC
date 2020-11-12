package it.ale_gnu.ats_poc.influx.new_api

private[new_api] object FluxQueryContainer {

  def getQueries(bucket: String, organization: String): Seq[String] = {
    val queryStart = s"""from(bucket:"$bucket") |> range(start: -1m)"""
    val temperatures = s"""$queryStart |> filter(fn: (r) => r._measurement == "temperature")"""
    val getOutput = """yield(name: "output")""" //needed only with multiple sources (using join, union, etc)

    Seq(
      //query 0 with to
      //saves the result of a simple query to a new bucket (only for Influx 2.0)
      s"""$queryStart |> to(bucket:"new-bucket", org:"$organization")""",

      //query 1 with limit
      //gets 2 points from the bucket filled by the previous query (only for Influx 2.0)
      s"""from(bucket:"new-bucket") |> range(start: -1m) |> limit(n: 2)""",

      /* ### Queries that run on both influx 1.8 and 2.0: ### */

      //query 2 (simple query)
      //shows all the inserted data. It's useful when using Influx 1.8, since query 0 and a are not supported in 1.8
      s"""$queryStart""",

      //query 3 with filter and union
      //the result is the union of the temperatures from the south with the ones from the north
      s"""bucket1 = $temperatures |> filter(fn: (r) => r.location == "north")
         bucket2 = $temperatures |> filter(fn: (r) => r.location == "south")
         union(tables: [bucket1, bucket2]) |> $getOutput""",

      //query 4 with join between two different measurements and math across measurements
      //joins the temperatures with the humidity values, based on time and location and calculates temperature+humidity
      s"""bucket1 = $temperatures
          bucket2 = $queryStart |> filter(fn: (r) => r._measurement == "humidity")
          join(tables: {b1: bucket1, b2: bucket2}, on: ["_time", "location"])
          |> map(fn: (r) => ({ r with measurement_sum: r._value_b1 + r._value_b2 })) |>  $getOutput""",

      //query 5 with window and mean
      //groups the points in time windows of 1 second and calculates the mean temperature for each one
      s"""$temperatures |> window(every: 1000ms) |> mean(column: "_value")""",

      //query 6 with group and mean
      //calculates the mean temperature for each location
      s"""$temperatures |> group(columns: ["location"]) |> mean(column: "_value")""",

      //query 7 with aggregateWindow
      //same as query 4, but using aggregateWindow
      /* aggregateWindow currently seems to be slow, memory hungry and can cause an OOM error in influx's process:
         https://community.influxdata.com/t/aggregatewindow-extremely-slow-and-memory-hungry/11635  */
      s"""$temperatures |> aggregateWindow(column: "_value", every: 1000ms, fn: mean)
         |> filter(fn: (r) => exists r._value)""",

      //query 8 (DatePart-like)
      //returns all the data with time values in the [9, 18] time range
      s"""$queryStart |> hourSelection(start: 9, stop: 18)""",

      //query 9 with pivot
      //shows the temperatures with the location as row key and time as column key
      s"""$temperatures |> pivot(rowKey:["location"], columnKey: ["_time"], valueColumn: "_value")""",

      //query 10 with a custom function, map and reduce
      //converts the temperatures to fahrenheit and sums them (without using sum())
      //_value is necessary when using map
      s"""celsiusToFahrenheit = (c) => (c * 9.0/5.0) + 32.0
        $temperatures  |> map(fn: (r) => ({ _value: celsiusToFahrenheit(c: r._value) }))
        |> reduce(fn: (r, accumulator) => ({ sum: r._value + accumulator.sum }), identity: {sum: 0.0})""",

      //query 11 with keep and covariance
      //calculates the covariance (normalized to be the Pearson R coefficient) between the temperatures t and t*2
      s"""$temperatures |> map(fn: (r) => ({r with times_two: r._value * 2.0}))
                        |> keep(columns: ["times_two", "_value"])
                        |> covariance(columns: ["times_two", "_value"], pearsonr: true)""")
  }

}
