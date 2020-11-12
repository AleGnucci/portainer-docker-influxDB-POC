package it.ale_gnu.ats_poc.influx.new_api

import java.time.temporal.ChronoUnit

import akka.actor.ActorSystem
import com.influxdb.client.{InfluxDBClient, WriteApi}
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.scala.QueryScalaApi
import com.influxdb.client.write.Point
import com.influxdb.query.dsl.Flux
import it.ale_gnu.ats_poc.influx.new_api.api.EnhancedV2Api.Implicits.CustomWriteApi
import it.ale_gnu.ats_poc.influx.new_api.api.EnhancedV2Api.EnhancedFlux
import it.ale_gnu.ats_poc.utils.Utils.Implicits.EnhancedFuture
import it.ale_gnu.ats_poc.utils.Utils.getRandom
import java.time.Instant

import scala.util.{Random, Try}

private[new_api] object ClientHelper {

  def writeRandomPoints(bucket: String, organization: String)
                               (implicit queryApi: QueryScalaApi, writeApi: WriteApi, system: ActorSystem): Unit = {
    def getRandomMeasurement = if (Random.nextBoolean()) "temperature" else "humidity"
    def getRandomLocation = if (Random.nextBoolean()) getEastOrWest else getNorthOrSouth
    def getEastOrWest = if(Random.nextBoolean()) "east" else "west"
    def getNorthOrSouth = if(Random.nextBoolean()) "north" else "south"

    val pointProducer = () => {
      Thread.sleep(250) //this makes the point timestamps different from one another
      Point.measurement(getRandomMeasurement)
        .addTag("location", getRandomLocation).addField("value", getRandom(0, 100))
        .time(Instant.now.toEpochMilli, WritePrecision.MS)
    }
    writeApi.writePointsByProducer(bucket, organization, pointProducer, 8)
  }

  def waitAndCheckWrittenData(bucket: String)(implicit queryApi: QueryScalaApi, writeApi: WriteApi,
                                                      system: ActorSystem): Unit = {
    writeApi.flush() //flush all pending writes to HTTP
    Thread.sleep(6000) //wait for influx to finish writing before checking
    val query = Flux.from(bucket).range(-1, ChronoUnit.DAYS)
    val pointCount = EnhancedFlux.runAsync(query)
      .runFold(0)((accumulator, _) => accumulator + 1)
      .awaitForTenSeconds
    println(s"Amount of points successfully written to $bucket: $pointCount")
  }

  def isUsingInfluxV2(influxClient: InfluxDBClient): Boolean =
    Try(influxClient.getBucketsApi.findBuckets()).isSuccess

}
