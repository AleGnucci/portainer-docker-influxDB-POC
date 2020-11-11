package it.ale_gnu.influx_client.influx.new_api.api

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.influxdb.client.domain.WritePrecision.MS
import com.influxdb.client.scala.QueryScalaApi
import com.influxdb.client.{OrganizationsApi, WriteApi}
import com.influxdb.query.FluxRecord
import com.influxdb.query.dsl.Flux
import EnhancedV2API.Implicits._
import it.ale_gnu.influx_client.utils.Utils.Implicits.EnhancedFuture
import zio._
import zio.console.putStrLn

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

private[new_api] object EnhancedV2API {

  object EnhancedFlux {

    def runAndPrintAll(queries: Seq[String])(implicit queryApi: QueryScalaApi, system: ActorSystem): Unit =
      runAndPrintAllAsync(queries).awaitForTenSeconds

    def runAndPrint(query: Flux)(implicit queryApi: QueryScalaApi, system: ActorSystem): Unit =
      runAndPrint(query.toString)

    def runAndPrint(query: String)(implicit queryApi: QueryScalaApi, system: ActorSystem): Unit =
      runAndPrintAsync(query).awaitForTenSeconds

    def runAndPrintAllAsync(queries: Seq[String])
                           (implicit queryApi: QueryScalaApi, system: ActorSystem): Future[Unit] = {
      val codeToRun = ZIO.foreach(queries) {query => //sequentially runs all the queries (using "traverse" operation)
        putStrLn(s"query ${queries.indexOf(query)}:") *> ZIO.fromFuture(_ => runAndPrintAsync(query))
      }.unit
      Runtime.default.unsafeRunToFuture(codeToRun)
    }

    def runAndPrintAsync(query: String)(implicit queryApi: QueryScalaApi, system: ActorSystem): Future[Unit] =
      queryApi.printQueryResultAsync(query)

    def run(query: Flux)(implicit queryApi: QueryScalaApi,system: ActorSystem): Seq[FluxRecord] = run(query.toString)

    def run(query: String)(implicit queryApi: QueryScalaApi,system: ActorSystem): Seq[FluxRecord] = {
      implicit val materializer: Materializer = Materializer(system)
      runAsync(query).runWith(Sink.collection[FluxRecord, Seq[FluxRecord]]).awaitForTenSeconds
    }

    def runAsync(query: Flux)(implicit queryApi: QueryScalaApi): Source[FluxRecord, NotUsed] = runAsync(query.toString)

    def runAsync(query: String)(implicit queryApi: QueryScalaApi): Source[FluxRecord, NotUsed] = queryApi.query(query)
  }

  object Implicits {

    implicit class CustomQueryApi(val queryApi: QueryScalaApi) {
      def printQueryResultAsync(query: String, hideAdditionalInfo: Boolean = true)
                               (implicit system: ActorSystem): Future[Unit] = { //extension method
        implicit val executionContext: ExecutionContext = system.dispatcher
        queryApi.query(query).runForeach(record => {
          val values = Map.from(record.getValues.asScala)
          val filteredValues = if(hideAdditionalInfo) values - "_start" - "_stop" - "result" else values
          println(filteredValues.mkString(", "))
        }).map(_ => ())
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
