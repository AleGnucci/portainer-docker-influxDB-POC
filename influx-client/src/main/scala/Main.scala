
object Main extends App {

  OldInfluxClient().runQueries()
  println("Finished running influx queries")

  PortainerClient().useAPIs()
  println("Finished using portainer APIs")

}
