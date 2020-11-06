
object Main extends App {

  Thread.sleep(10000) // gives time to portainer and influxDb to finish setup

  InfluxClient().runQueries()
  println("Finished running influx queries")

  PortainerClient().useAPIs()
  println("Finished using portainer APIs")

}
