package it.ale_gnu.influx_client

import it.ale_gnu.influx_client.influx.InfluxClient
import it.ale_gnu.influx_client.influx.new_api.NewInfluxClient
import it.ale_gnu.influx_client.portainer.PortainerClient

object Main extends App {

  val influxClient: InfluxClient = NewInfluxClient()

  Thread.sleep(13000) // gives time to it.ale_gnu.influx_client.portainer and influxDb to finish setup

  influxClient.runExamples()
  println("Finished running it.ale_gnu.influx_client.influx queries and tests")

  PortainerClient().useAPIs()
  println("Finished using it.ale_gnu.influx_client.portainer APIs")

  System.exit(0)
}
