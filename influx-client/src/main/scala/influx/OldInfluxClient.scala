package influx

import java.util.concurrent.TimeUnit

import org.influxdb.dto.{Point, Query, QueryResult}
import org.influxdb.{BatchOptions, InfluxDB, InfluxDBFactory}
import utils.Utils.getRandom

import scala.util.Random

/** Class to test the InfluxDb 1.x APIs */
class OldInfluxClient extends InfluxClient  {

  def runExamples(): Unit = {
    val (serverURL, username, password) = ("http://influx:8086", "root", "root");
    val influxDB = InfluxDBFactory.connect(serverURL, username, password);

    // Create a database...
    val databaseName = "NOAA_water_database";
    influxDB.query(new Query("CREATE DATABASE " + databaseName));
    influxDB.setDatabase(databaseName);

    // ... and a retention policy
    val retentionPolicyName = "one_day_only";
    influxDB.query(new Query("CREATE RETENTION POLICY " + retentionPolicyName
      + " ON " + databaseName + " DURATION 1d REPLICATION 1 DEFAULT"));
    influxDB.setRetentionPolicy(retentionPolicyName);

    // Enable batch writes to get better performance.
    influxDB.enableBatch(BatchOptions.DEFAULTS);

    // Write points to InfluxDB.
    writeRandomPoints(influxDB)

    // query setup
    def printQueryResult(query: String): Unit = System.out.println(runQuery(query))
    def runQuery(query: String): QueryResult = influxDB.query(new Query(query))

    //continuous query that saves to "mean_water_level"
    runQuery(s"""CREATE CONTINUOUS QUERY "cq_mean_water_level" ON "$databaseName"
            BEGIN
            SELECT mean("water_level") INTO mean_water_level FROM h2o_feet GROUP BY time(5s)
            END""")

    Thread.sleep(6000); // Wait to let the InfluxDB client write the points asynchronously

    println("Simple query:")
    printQueryResult("SELECT * FROM h2o_feet LIMIT 10")

    println("subquery example:")
    printQueryResult("SELECT location FROM (SELECT * FROM h2o_feet LIMIT 10)")

    println("query with function nesting:")
    printQueryResult("SELECT COUNT(DISTINCT(level_description)) FROM h2o_feet")

    println("more complex query:") //writes the result in "result_measurement"
    runQuery("SELECT water_level INTO result_measurement FROM h2o_feet WHERE location='santa_monica'" +
      " GROUP BY level_description ORDER BY time LIMIT 10 OFFSET 1 SLIMIT 1")
    printQueryResult("SELECT * FROM result_measurement ") //reading from "result_measurement"

    println("continuous query's result:")
    printQueryResult("SELECT * FROM mean_water_level ")

    influxDB.close();
  }

  private def writeRandomPoints(influxDB: InfluxDB): Unit = {
    for (_ <- 1 to 100) {
      Thread.sleep(getRandom(0, 5).toLong) //this makes the point timestamps different form one another
      val waterLevel = getRandom(0, 9)
      influxDB.write(Point.measurement("h2o_feet")
        .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
        .tag("location", if(Random.nextBoolean()) "santa_monica" else "coyote_creek")
        .addField("level_description", if(waterLevel < 3) "below 3 feet" else "between 6 and 9 feet")
        .addField("water_level", waterLevel)
        .build());
    }
  }
}

object OldInfluxClient {
  def apply(): OldInfluxClient = new OldInfluxClient()
}
