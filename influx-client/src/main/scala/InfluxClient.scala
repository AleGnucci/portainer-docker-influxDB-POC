import com.influxdb.client.InfluxDBClientFactory
import com.influxdb.client.QueryApi
import com.influxdb.client.WriteApi
import com.influxdb.client.domain.WritePrecision.MS

import Utils._

import scala.jdk.CollectionConverters._

/** Unfinished class, can be used to test the 2.x Influx APIs. */
class InfluxClient {

  def runQueries(): Unit = {
    //initial setup
    val (token, bucket, org) = ("my-token".toCharArray, "my-bucket", "my-org")
    val influxDBClient = InfluxDBClientFactory.create("http://influx:8086", token, org, bucket)
    val queryApi: QueryApi = influxDBClient.getQueryApi
    val writeApi: WriteApi = influxDBClient.getWriteApi

    // Write data
    writeRandomPoints(bucket, org, writeApi, "north")
    writeRandomPoints(bucket, org, writeApi, "south")
    writeRandomPoints(bucket, org, writeApi, "east")
    writeRandomPoints(bucket, org, writeApi, "west")

    // Query data
    val query = s"""from(bucket:"$bucket") |> range(start: 0) |> limit(n: 10)"""
    val tables = queryApi.query(query).asScala
    for (fluxTable <- tables) {
      val records = fluxTable.getRecords.asScala
      for (fluxRecord <- records) {
        System.out.println(fluxRecord.getTime + ": " + fluxRecord.getValueByKey("value"))
      }
    }

    influxDBClient.close()
  }

  private def writeRandomPoints(bucket: String, organization: String, writeApi: WriteApi, location: String): Unit = {
    for (_ <- 1 to 100) {
      Thread.sleep(getRandom(0, 5).toLong) //this makes the point timestamps different form one another
      writeApi.writeRecord(bucket, organization, MS, s"temperature,$location=north value=${getRandom(0, 100)}")
    }
  }
}

object InfluxClient {
  def apply(): InfluxClient = new InfluxClient()
}
