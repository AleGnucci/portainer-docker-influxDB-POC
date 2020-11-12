package it.ale_gnu.ats_poc

import it.ale_gnu.ats_poc.influx.InfluxClient
import it.ale_gnu.ats_poc.influx.new_api.NewInfluxClient
import it.ale_gnu.ats_poc.portainer.PortainerClient

object Main extends App {

  val influxClient: InfluxClient = NewInfluxClient()

  Thread.sleep(13000) // gives time to it.ale_gnu.influx_client.portainer and influxDb to finish setup

  influxClient.runExamples()
  println("Finished running influx queries and tests")

  PortainerClient().useAPIs()
  println("Finished using portainer APIs")

  System.exit(0)
}
