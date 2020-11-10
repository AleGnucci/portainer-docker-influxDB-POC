package influx

import java.time.temporal.ChronoUnit

import akka.actor.ActorSystem
import com.influxdb.client.scala.{InfluxDBClientScalaFactory, QueryScalaApi}
import com.influxdb.client.{InfluxDBClientFactory, WriteApi}
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions.measurement
import influx.API.EnhancedV2API.Implicits._
import influx.API.EnhancedV2API._
import utils.Utils.Implicits.EnhancedFuture
import utils.Utils.getRandom

import scala.util.Random

/** Unfinished class, can be used to test the 2.x Influx APIs. */
class NewInfluxClient extends InfluxClient {

  private val (url, token, bucket, org) = ("http://influx:8086", "my-token", "my-bucket", "my-org")

  def runExamples(): Unit = {
    //initial setup
    val influxClient = InfluxDBClientFactory.create(url, token.toCharArray, org, bucket)
    val influxScalaClient = InfluxDBClientScalaFactory.create(url, token.toCharArray, org, bucket)
    val organizationId =  influxClient.getOrganizationsApi.getOrganizationId(org)
    implicit val (queryApi, writeApi) = (influxScalaClient.getQueryScalaApi(), influxClient.getWriteApi)
    val (tasksApi, bucketsApi) = (influxClient.getTasksApi, influxClient.getBucketsApi)
    implicit val actorSystem = ActorSystem("main-system")
    implicit val context = actorSystem.dispatcher

    //create output bucket
    bucketsApi.createBucket("new-bucket", organizationId)

    // Writes sample data and checks if it succeeds
    writeRandomPointsAndPrintResult(bucket, org)

    //prepares some example queries (some of these also write to the output bucket)
    //more examples in https://github.com/influxdata/influxdb-client-java/tree/master/examples/src/main/java/example
    val queries = FluxQueryContainer.getQueries(bucket, org)

    //runs the queries as Flux strings (without dsl), waiting for them to finish and print their results
    EnhancedFlux runAndPrintAll queries

    //runs an example query using flux-dsl
    //more examples in https://github.com/influxdata/influxdb-client-java/tree/master/flux-dsl
    //simply retrieves 10 temperature points which are not older than 1 day
    println("Flux-dsl query result:")
    val constructedQuery = Flux.from(bucket)
      .range(-1, ChronoUnit.DAYS)
      .filter(measurement().equal("temperature"))
      .sample(10).toString
    EnhancedFlux runAndPrint constructedQuery

    //creates a Task. Tasks replace InfluxDB v1.x continuous queries.
    //more examples in ITTasksApi.java from https://github.com/influxdata
    //runs the first query in FluxQueryContainer every 5 seconds
    tasksApi.createTaskEvery("exampleTask", queries.head, "5s", organizationId)

    //wait for the task to run once
    Thread.sleep(6000)

    {influxClient.close(); influxScalaClient.close()}
  }

  private def writeRandomPointsAndPrintResult(bucket: String, organization: String)
                                             (implicit queryApi: QueryScalaApi, writeApi: WriteApi,
                                              system: ActorSystem): Unit = {
    def getRandomLocation = if (Random.nextBoolean()) "north" else "south"
    def getRandomMeasurement = if (Random.nextBoolean()) "temperature" else "humidity"

    val pointProducer = () => {
      Thread.sleep(getRandom(0, 5).toLong) //this makes the point timestamps different from one another
      s"$getRandomMeasurement,location=$getRandomLocation value=${getRandom(0, 100)}"
    }
    writeApi.writePointsByProducer(bucket, organization, pointProducer, 100)
    println(s"Amount of successfully written points to $bucket: ")
    (EnhancedFlux runAndPrintAsync Flux.from(bucket).range(-1, ChronoUnit.DAYS).count().toString)
      .awaitForTenSeconds
  }

}

object NewInfluxClient {
  def apply(): NewInfluxClient = new NewInfluxClient()
}
