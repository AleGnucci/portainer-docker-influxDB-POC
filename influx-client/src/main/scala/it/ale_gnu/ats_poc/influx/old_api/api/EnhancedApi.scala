package it.ale_gnu.ats_poc.influx.old_api.api

import org.influxdb.InfluxDB
import org.influxdb.dto.{Query, QueryResult}

object EnhancedApi {

  def printQueryResult(query: String)(implicit influxDB: InfluxDB): Unit = println(runQuery(query))
  def runQuery(query: String)(implicit influxDB: InfluxDB): QueryResult = influxDB.query(new Query(query))

}
