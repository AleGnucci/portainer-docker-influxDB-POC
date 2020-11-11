package it.ale_gnu.influx_client.influx.new_api

import java.time.temporal.ChronoUnit

import akka.actor.ActorSystem
import com.influxdb.client.InfluxDBClientFactory
import com.influxdb.client.scala.InfluxDBClientScalaFactory
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions.measurement
import it.ale_gnu.influx_client.influx.new_api.api.EnhancedV2API.Implicits._
import it.ale_gnu.influx_client.influx.new_api.api.EnhancedV2API._
import org.influxdb.InfluxDBFactory
import org.influxdb.dto.Query
import ClientHelper._
import it.ale_gnu.influx_client.influx.InfluxClient

/** Unfinished class, can be used to test the 2.x Influx APIs. Supports both InfluxDB 2.0 and 1.8. */
class NewInfluxClient extends InfluxClient {

  private val (url, token, bucket, org) = ("http://influx:8086", "my-token", "my-bucket", "my-org")
  private val outputBucket = "new-bucket"

  def runExamples(): Unit = {
    //initial setup
    val influxClient = InfluxDBClientFactory.create(url, token.toCharArray, org, bucket)
    val influxScalaClient = InfluxDBClientScalaFactory.create(url, token.toCharArray, org, bucket)
    val oldInfluxClient = InfluxDBFactory.connect(url, "root", "root") //for 1.8 compatibility
    implicit val (queryApi, writeApi) = (influxScalaClient.getQueryScalaApi(), influxClient.getWriteApi)
    val (tasksApi, bucketsApi) = (influxClient.getTasksApi, influxClient.getBucketsApi)
    implicit val actorSystem = ActorSystem("main-system")
    implicit val context = actorSystem.dispatcher
    val usingInfluxV2 = isUsingInfluxV2(influxClient)

    //prepares some example queries (some of these also write to the output bucket)
    //more examples in https://github.com/influxdata/influxdb-client-java/tree/master/examples/src/main/java/example
    val queries = FluxQueryContainer.getQueries(bucket, org)

    if(usingInfluxV2) { //functionalities and API not supported by Influx v1.8
      //obtaining the organization id by name, for the following operations
      val organizationId =  influxClient.getOrganizationsApi.getOrganizationId(org)

      //creates an output bucket
      bucketsApi.createBucket(outputBucket, organizationId)

      //creates a Task. Tasks replace InfluxDB v1.x continuous queries.
      //more examples in client/src/test/java/com/influxdb/client/ITTasksApi.java from https://github.com/influxdata
      //runs the first query in FluxQueryContainer every 5 seconds
      tasksApi.createTaskEvery("exampleTask", queries.head, "5s", organizationId)

      //waits for the task to run once
      Thread.sleep(6000)
    } else {
      //creates two buckets to run the queries using the old API (can't use the new API for this in 1.8 yet)
      oldInfluxClient.query(new Query(s"""CREATE DATABASE "$bucket""""))
      oldInfluxClient.query(new Query(s"""CREATE DATABASE "$outputBucket""""))
    }

    //writes sample data, waits for completion and checks by counting the data
    writeRandomPoints(bucket, org)
    waitAndCheckWrittenData(bucket)

    //runs the queries as Flux strings (without dsl), waiting for them to finish and print their results
    //Flux in InfluxDB 1.x is read-only, so some queries can't be executed in 1.8
    EnhancedFlux runAndPrintAll (if(usingInfluxV2) queries else queries.drop(2))

    //runs an example query using flux-dsl
    //more examples in https://github.com/influxdata/influxdb-client-java/tree/master/flux-dsl
    //simply retrieves 10 temperature points which are not older than 1 day
    println("Flux-dsl query result:")
    val constructedQuery = Flux.from(bucket)
      .range(-1, ChronoUnit.DAYS)
      .filter(measurement().equal("temperature"))
      .sample(10)
    EnhancedFlux runAndPrint constructedQuery

    {influxClient.close(); influxScalaClient.close()}
  }

}

object NewInfluxClient {
  def apply(): NewInfluxClient = new NewInfluxClient()
}
