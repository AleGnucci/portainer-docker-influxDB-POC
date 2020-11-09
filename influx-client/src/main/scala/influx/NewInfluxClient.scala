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

    // Write data
    writeRandomPoints(bucket, org)

    //prepare some example queries
    val queries = FluxQueryContainer.getQueries(bucket)

    //run the queries as Flux strings (without dsl)
    EnhancedFlux runAndPrintAll queries //infix notation with conversion from List to varargs

    //run an example query using flux-dsl
    //simply retrieves 10 temperature points which are not older than 1 day
    println("Flux-dsl query result:" + Flux.from(bucket)
      .filter(measurement().equal("temperature"))
      .range(-1, ChronoUnit.DAYS)
      .sample(10).toString())

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
