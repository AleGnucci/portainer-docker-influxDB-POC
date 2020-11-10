package influx.API

import com.influxdb.client.domain.WritePrecision.MS
import com.influxdb.client.{OrganizationsApi, QueryApi, WriteApi}
import influx.API.EnhancedV2API.Implicits._

import scala.jdk.CollectionConverters._

object EnhancedV2API {

  object EnhancedFlux {
    def runAndPrintAll(queries: Seq[String])(implicit queryApi: QueryApi): Unit = {
      for (index <- queries.indices) { println(s"query $index:"); runAndPrint(queries(index)) }
    }

    def runAndPrint(query: String)(implicit queryApi: QueryApi): Unit = queryApi.printQueryResult(query)

    def run(query: String)(implicit queryApi: QueryApi): Unit = queryApi.query(query)
  }

  object Implicits {

    implicit class CustomQueryApi(val queryApi: QueryApi) { //TODO: use akka streams and make this async
      def printQueryResult(query: String): Unit = //extension method for QueryApi class
        for (fluxTable <- queryApi.query(query).asScala) {
          //println(s"Table with group key ${fluxTable.getGroupKey} :")
          for (fluxRecord <- fluxTable.getRecords.asScala) {
            println(fluxRecord.getTime + ": " + fluxRecord.getValueByKey("value"))
          }
        }
    }

    implicit class CustomWriteApi(val writeApi: WriteApi) {
      // writes random points using line protocol
      def writePointsByProducer(bucket: String, organization: String,
                                linePointProducer: () => String, count: Int): Unit =
        for (_ <- 1 to count) writeApi.writeRecord(bucket, organization, MS, linePointProducer())
    }

    implicit class CustomOrganizationsApi(val organizationsApi: OrganizationsApi) {
      def getOrganizationId(organizationName: String): String = {
        organizationsApi.findOrganizations().asScala.find(_.getName == organizationName)
          .getOrElse({throw new IllegalStateException(s"Organization $organizationName does not exist")}).getId
      }
    }

  }

}
