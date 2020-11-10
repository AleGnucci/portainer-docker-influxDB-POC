package influx

object FluxQueryContainer {

  def getQueries(bucket: String, organization: String): Seq[String] = {
    val queryStart = s"""from(bucket:"$bucket") |> range(start: -1m)"""
    val temperatures = s"""$queryStart |> filter(fn: (r) => r._measurement == "temperature")"""
    val getOutput = """yield(name: "output")""" //needed only with multiple sources (using join, union, etc)

    Seq(
      //query 0 with to
      //saves the result of a simple query to a new bucket
      s"""$queryStart |> to(bucket:"new-bucket", org:"$organization")""",

      //query 1 with limit
      //gets 10 points from the bucket created by the previous query
      s"""from(bucket:"new-bucket") |> range(start: -1m) |> limit(n: 10)""",

      //query 2 with filter and union
      //the result is the union of the temperatures from the south with the ones from the north
      s"""bucket1 = $temperatures |> filter(fn: (r) => r.location == "north")
         bucket2 = $temperatures |> filter(fn: (r) => r.location == "south")
         union(tables: [bucket1, bucket2]) |> $getOutput""",

      //query 3 with join between two different measurements and math across measurements
      //joins the temperatures with the humidity values, based on time and location
      s"""bucket1 = $temperatures
          bucket2 = $queryStart |> filter(fn: (r) => r._measurement == "humidity")
          join(tables: {d1: bucket1, d2: bucket2}, on: ["_time", "location"]) |> $getOutput""",

      //query 4 with window
      //gets 50 points from the bucket, separating them in time windows of 5 milliseconds
      s"""$queryStart |> window(every: 5ms) |> limit(n: 50)""",

      //query 5 with group and mean
      //calculates the mean temperature for each location
      s"""$temperatures |> group(columns: ["location"]) |> mean(column: "value")""",

      //query 6 with aggregateWindow
      //groups the temperatures in 5ms windows and calculates the mean for each window
      /* aggregateWindow currently seems to be slow, memory hungry and can cause an OOM error in influx's process:
         https://community.influxdata.com/t/aggregatewindow-extremely-slow-and-memory-hungry/11635  */
      s"""$temperatures |> aggregateWindow(column: "temperature", every: 5ms, fn: mean)""",

      //query 7 (DatePart-like)
      //returns all the data with time values in the [9, 18] time range
      s"""$queryStart |> hourSelection(start: 9, stop: 18) |> limit(n: 10)""",

      //query 8 with pivot
      //shows the temperatures with the location as row key and time as column key
      s"""$temperatures |> pivot(rowKey:["location"], columnKey: ["_time"], valueColumn: "value")""",

      //query 9 with a custom function, map and reduce
      //converts the temperatures to fahrenheit and sums them (without using sum())
      //_value is necessary when using map
      s"""celsiusToFahrenheit = (c) => (c * 9/5) + 32
        $temperatures  |> map(fn: (r) => ({ _value: celsiusToFahrenheit(c: r.value) }))
        |> reduce(fn: (r, accumulator) => ({ sum: r._value + accumulator.sum }), identity: {sum: 0})""",

      //query 10 with covariance
      //calculates the covariance between time and value
      s"""$temperatures |> covariance(columns: ["_time", "value"]) """)
  }

}
