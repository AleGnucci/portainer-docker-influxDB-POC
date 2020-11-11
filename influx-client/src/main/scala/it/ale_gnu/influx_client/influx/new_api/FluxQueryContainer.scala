package it.ale_gnu.influx_client.influx.new_api

private[new_api] object FluxQueryContainer { //TODO: use InfluxDB 1.8 instead of 2.0 to see if the bugs remain

  def getQueries(bucket: String, organization: String): Seq[String] = {
    val queryStart = s"""from(bucket:"$bucket") |> range(start: -1m)"""
    val temperatures = s"""$queryStart |> filter(fn: (r) => r._measurement == "temperature")"""
    val getOutput = """yield(name: "output")""" //needed only with multiple sources (using join, union, etc)

    Seq(
      //query 0 with to
      //saves the result of a simple query to a new bucket
      s"""$queryStart |> to(bucket:"new-bucket", org:"$organization")""",

      //TODO: find out why this query returns more than 2 points
      //query 1 with limit
      //gets 2 points from the bucket filled by the previous query
      s"""from(bucket:"new-bucket") |> range(start: -1m) |> limit(n: 2)""",

      //query 2 with filter and union
      //the result is the union of the temperatures from the south with the ones from the north
      s"""bucket1 = $temperatures |> filter(fn: (r) => r.location == "north")
         bucket2 = $temperatures |> filter(fn: (r) => r.location == "south")
         union(tables: [bucket1, bucket2]) |> $getOutput""",

      //query 3 with join between two different measurements and math across measurements
      //joins the temperatures with the humidity values, based on time and location and calculates temperature+humidity
      s"""bucket1 = $temperatures
          bucket2 = $queryStart |> filter(fn: (r) => r._measurement == "humidity")
          join(tables: {b1: bucket1, b2: bucket2}, on: ["_time", "location"])
          |> map(fn: (r) => ({ r with measurement_sum: r._value_b1 + r._value_b2 })) |>  $getOutput""",

      //TODO: find out why this returns 7 groups instead of 2
      //query 4 with window
      //groups the points in time windows of 1 second and calculates the mean temperature for each one
      s"""$temperatures |> window(every: 1000ms) |> mean(column: "_value")""",

      //query 5 with group and mean
      //calculates the mean temperature for each location
      s"""$temperatures |> group(columns: ["location"]) |> mean(column: "_value")""",

      //TODO: check why this returns so many rows, with most values set to null
      //query 6 with aggregateWindow
      //groups the temperatures in 1s windows and calculates the mean for each window
      /* aggregateWindow currently seems to be slow, memory hungry and can cause an OOM error in it.ale_gnu.influx_client.influx's process:
         https://community.influxdata.com/t/aggregatewindow-extremely-slow-and-memory-hungry/11635  */
      s"""$temperatures |> aggregateWindow(column: "_value", every: 1000ms, fn: mean) |> limit(n: 10)""",

      //query 7 (DatePart-like)
      //returns all the data with time values in the [9, 18] time range
      s"""$queryStart |> hourSelection(start: 9, stop: 18)""",

      //query 8 with pivot
      //shows the temperatures with the location as row key and time as column key
      s"""$temperatures |> pivot(rowKey:["location"], columnKey: ["_time"], valueColumn: "_value")""",

      //query 9 with a custom function, map and reduce
      //converts the temperatures to fahrenheit and sums them (without using sum())
      //_value is necessary when using map
      s"""celsiusToFahrenheit = (c) => (c * 9.0/5.0) + 32.0
        $temperatures  |> map(fn: (r) => ({ _value: celsiusToFahrenheit(c: r._value) }))
        |> reduce(fn: (r, accumulator) => ({ sum: r._value + accumulator.sum }), identity: {sum: 0.0})""",

      //TODO: find out why this sometimes returns some useless rows with null values
      //query 10 with covariance
      //calculates the covariance between the temperatures t and t*2
      s"""$temperatures |> map(fn: (r) => ({r with times_two: r._value * 2.0})
                        |> covariance(columns: ["times_two", "_value"])""")
  }

}
