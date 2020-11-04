import java.util.concurrent.TimeUnit

import org.influxdb.dto.{Point, Query}
import org.influxdb.{BatchOptions, InfluxDBFactory}

class OldInfluxClient {

  def runQueries(): Unit = {
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
    influxDB.write(Point.measurement("h2o_feet")
      .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
      .tag("location", "santa_monica")
      .addField("level description", "below 3 feet")
      .addField("water_level", 2.064d)
      .build());

    influxDB.write(Point.measurement("h2o_feet")
      .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
      .tag("location", "coyote_creek")
      .addField("level description", "between 6 and 9 feet")
      .addField("water_level", 8.12d)
      .build());

    // Wait a few seconds in order to let the InfluxDB client
    // write your points asynchronously
    Thread.sleep(5_000L);

    // Query your data using InfluxQL.
    val queryResult = influxDB.query(new Query("SELECT * FROM h2o_feet"));

    System.out.println(queryResult);
    // It will print something like:
    // QueryResult [results=[Result [series=[Series [name=h2o_feet, tags=null,
    //      columns=[time, level description, location, water_level],
    //      values=[
    //         [2020-03-22T20:50:12.929Z, below 3 feet, santa_monica, 2.064],
    //         [2020-03-22T20:50:12.929Z, between 6 and 9 feet, coyote_creek, 8.12]
    //      ]]], error=null]], error=null]

    // Query using Flux


    // Close it if your application is terminating or you are not using it anymore.
    influxDB.close();
  }
}

object OldInfluxClient {
  def apply(): OldInfluxClient = new OldInfluxClient()
}
