import influx.{InfluxClient, NewInfluxClient}
import portainer.PortainerClient

object Main extends App {

  val influxClient: InfluxClient = NewInfluxClient()

  Thread.sleep(12000) // gives time to portainer and influxDb to finish setup

  influxClient.runExamples()
  println("Finished running influx queries and tests")

  PortainerClient().useAPIs()
  println("Finished using portainer APIs")

  System.exit(0)
}
