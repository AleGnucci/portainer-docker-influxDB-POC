package influx

import java.time.temporal.ChronoUnit

import com.influxdb.client.{InfluxDBClientFactory, WriteApi}
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions.measurement
import influx.API.EnhancedV2API._
import influx.API.EnhancedV2API.Implicits._
import utils.Utils.getRandom

import scala.util.Random

/** Unfinished class, can be used to test the 2.x Influx APIs. */
class NewInfluxClient extends InfluxClient {

  def runQueries(): Unit = {
    //initial setup
    val (token, bucket, org) = ("my-token", "my-bucket", "my-org")
    val influxDBClient = InfluxDBClientFactory.create("http://influx:8086", token.toCharArray, org, bucket)
    implicit val (queryApi, writeApi) = (influxDBClient.getQueryApi, influxDBClient.getWriteApi)
    val tasksApi = influxDBClient.getTasksApi

    // Writes sample data
    writeRandomPoints(bucket, org)

    //prepares some example queries
    //more examples in https://github.com/influxdata/influxdb-client-java/tree/master/examples/src/main/java/example
    val queries = FluxQueryContainer.getQueries(bucket)

    //runs the queries as Flux strings (without dsl)
    EnhancedFlux runAndPrintAll queries

    //runs an example query using flux-dsl
    //more examples in https://github.com/influxdata/influxdb-client-java/tree/master/flux-dsl
    //simply retrieves 10 temperature points which are not older than 1 day
    println("Flux-dsl query result:" + Flux.from(bucket)
      .filter(measurement().equal("temperature"))
      .range(-1, ChronoUnit.DAYS)
      .sample(10).toString())

    //creates a Task. Tasks replace InfluxDB v1.x continuous queries.
    //more examples in ITTasksApi.java from https://github.com/influxdata
    //runs the first query in FluxQueryContainer every 10 seconds
    tasksApi.createTaskEvery("exampleTask", queries.head, "10s", org)

    influxDBClient.close()
  }

  private def writeRandomPoints(bucket: String, organization: String)(implicit writeApi: WriteApi): Unit = {
    def getRandomLocation: String = if (Random.nextBoolean()) "north" else "south"

    def getRandomMeasurement: String = if (Random.nextBoolean()) "temperature" else "humidity"

    val pointProducer = () => {
      Thread.sleep(getRandom(0, 5).toLong) //this makes the point timestamps different from one another
      s"$getRandomMeasurement,location=$getRandomLocation value=${getRandom(0, 100)}"
    }
    writeApi.writePointsByProducer(bucket, organization, pointProducer, 100)
  }

}

object NewInfluxClient {
  def apply(): NewInfluxClient = new NewInfluxClient()
}
