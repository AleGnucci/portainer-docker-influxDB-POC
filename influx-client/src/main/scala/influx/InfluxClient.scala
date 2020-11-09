package influx

trait InfluxClient {

  def runQueries(): Unit

}
