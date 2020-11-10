package influx

import java.time.temporal.ChronoUnit

import com.influxdb.client.{InfluxDBClientFactory, QueryApi, WriteApi}
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
    val organizationId =  influxDBClient.getOrganizationsApi.getOrganizationId(org)
    implicit val (queryApi, writeApi) = (influxDBClient.getQueryApi, influxDBClient.getWriteApi)
    val (tasksApi, bucketsApi) = (influxDBClient.getTasksApi, influxDBClient.getBucketsApi)

    //create output bucket
    bucketsApi.createBucket("new-bucket", organizationId)

    // Writes sample data and checks if it succeeds
    writeRandomPointsAndPrintResult(bucket, org)

    //prepares some example queries (some of these also write to the output bucket)
    //more examples in https://github.com/influxdata/influxdb-client-java/tree/master/examples/src/main/java/example
    val queries = FluxQueryContainer.getQueries(bucket, org)

    //runs the queries as Flux strings (without dsl)
    EnhancedFlux runAndPrintAll queries

    //runs an example query using flux-dsl
    //more examples in https://github.com/influxdata/influxdb-client-java/tree/master/flux-dsl
    //simply retrieves 10 temperature points which are not older than 1 day
    val constructedQuery = Flux.from(bucket)
      .range(-1, ChronoUnit.DAYS)
      .filter(measurement().equal("temperature"))
      .sample(10).toString
    println("Flux-dsl query result:")
    EnhancedFlux runAndPrint constructedQuery

    //creates a Task. Tasks replace InfluxDB v1.x continuous queries.
    //more examples in ITTasksApi.java from https://github.com/influxdata
    //runs the first query in FluxQueryContainer every 10 seconds
    tasksApi.createTaskEvery("exampleTask", queries.head, "10s", organizationId)

    //TODO: wait on all the results before exiting

    influxDBClient.close()
  }

  private def writeRandomPointsAndPrintResult(bucket: String, organization: String)
                                             (implicit queryApi: QueryApi, writeApi: WriteApi): Unit = {
    def getRandomLocation = if (Random.nextBoolean()) "north" else "south"
    def getRandomMeasurement = if (Random.nextBoolean()) "temperature" else "humidity"

    val pointProducer = () => {
      Thread.sleep(getRandom(0, 5).toLong) //this makes the point timestamps different from one another
      s"$getRandomMeasurement,location=$getRandomLocation value=${getRandom(0, 100)}"
    }
    writeApi.writePointsByProducer(bucket, organization, pointProducer, 100)
    println(s"Amount of successfully written points to $bucket: ")
    EnhancedFlux runAndPrint Flux.from(bucket).range(-1, ChronoUnit.DAYS).count().toString
  }

}

object NewInfluxClient {
  def apply(): NewInfluxClient = new NewInfluxClient()
}
