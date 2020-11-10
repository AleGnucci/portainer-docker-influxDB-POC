package influx.API

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import com.influxdb.client.domain.WritePrecision.MS
import com.influxdb.client.scala.QueryScalaApi
import com.influxdb.client.{OrganizationsApi, WriteApi}
import com.influxdb.query.FluxRecord
import influx.API.EnhancedV2API.Implicits._
import utils.Utils.Implicits.EnhancedFuture
import zio._
import zio.console.putStrLn

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

object EnhancedV2API {

  object EnhancedFlux {

    def runAndPrintAll(queries: Seq[String])(implicit queryApi: QueryScalaApi, system: ActorSystem): Unit =
      runAndPrintAllAsync(queries).awaitForTenSeconds

    def runAndPrint(query: String)(implicit queryApi: QueryScalaApi, system: ActorSystem): Unit =
      runAndPrintAsync(query).awaitForTenSeconds

    def runAndPrintAllAsync(queries: Seq[String])
                           (implicit queryApi: QueryScalaApi, system: ActorSystem): Future[Unit] = {
      val codeToRun = ZIO.foreach(queries) {query => //sequentially runs all the queries
        putStrLn(s"query ${queries.indexOf(query)}:") *> ZIO.fromFuture(_ => runAndPrintAsync(query))
      }.unit
      Runtime.default.unsafeRunToFuture(codeToRun)
    }

    def runAndPrintAsync(query: String)(implicit queryApi: QueryScalaApi, system: ActorSystem): Future[Unit] =
      queryApi.printQueryResultAsync(query)

    def runAsync(query: String)(implicit queryApi: QueryScalaApi): Source[FluxRecord, NotUsed] = queryApi.query(query)
  }

  object Implicits {

    implicit class CustomQueryApi(val queryApi: QueryScalaApi) {
      def printQueryResultAsync(query: String)(implicit system: ActorSystem): Future[Unit] = { //extension method
        implicit val executionContext: ExecutionContext = system.dispatcher
        queryApi.query(query).runForeach(record =>
          println(record.getTime + ": " + record.getValueByKey("value"))).map(_ => ())
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
