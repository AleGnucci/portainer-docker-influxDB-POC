package influx

object FluxQueryContainer {

  def getQueries(bucket: String): Seq[String] = {
    val queryStart = s"""from(bucket:"$bucket") |> range(start: 0)"""
    val temperatures = s"""$queryStart |> filter(fn: (r) => r._measurement == "temperature")"""
    Seq(
      // simple query (using the extension method "printQueryResult")
      //gets 10 points from the bucket
      s"""$queryStart |> limit(n: 10)""",

      //query with filter and union
      //the result is the union of the temperatures from the south with the ones from the north
      s"""bucket1 = $temperatures |> filter(fn: (r) => r.location == "north")
         bucket2 = $temperatures |> filter(fn: (r) => r.location == "south")
         union(tables: [bucket1, bucket2])""",

      //query with join between two different measurements and math across measurements
      //joins the temperatures with the humidity values, based on time and location
      s"""bucket1 = $temperatures
          bucket2 = $queryStart |> filter(fn: (r) => r._measurement == "humidity")
          join(tables: {d1: bucket1, d2: bucket2}, on: ["_time", "location"])""",

      //query with window
      //gets 50 points from the bucket, separating them in time windows of 5 milliseconds
      s"""$queryStart |> window(every: 5ms) |> limit(n: 50)""",

      //query with group and mean
      s"""$temperatures |> group(columns: ["location"]) |> mean(column: "value")""",

      //query with aggregateWindow
      //groups the temperatures in 5ms windows and calculates the mean for each window
      /* aggregateWindow currently seems to be slow and memory hungry:
         https://community.influxdata.com/t/aggregatewindow-extremely-slow-and-memory-hungry/11635  */
      s"""$temperatures |> aggregateWindow(column: "temperature", every: 5ms, fn: mean)""", //FIXME: causes OOM in influx

      //DatePart-like query
      //returns all the data with time values in the [9, 18] time range
      s"""$queryStart |> hourSelection(start: 9, stop: 18) |> limit(n: 10)""",

      //query with pivot
      //shows the temperatures with the location as row key and time as column key
      s"""$temperatures |> pivot(rowKey:["location"], columnKey: ["_time"], valueColumn: "value")""",

      //query with a custom function, map and reduce
      //converts the temperatures to fahrenheit and sums them (without using sum())
      s"""celsiusToFahrenheit = (c) => (c × 9/5) + 32
        $temperatures  |> map(fn: (r) => ({ value: celsiusToFahrenheit(r.value) }))
        |> reduce(fn: (r, accumulator) => ({ sum: r._value + accumulator.sum }), identity: {sum: 0.0})""",

      //query with covariance
      //calculates the covariance between time and value
      s"""$temperatures |> covariance(columns: ["_time", "value"]) """)
  }

}
