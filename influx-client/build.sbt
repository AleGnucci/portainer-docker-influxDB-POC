name := "influx-client"

version := "0.1"

scalaVersion := "2.13.3"

/* using also java version of influxdb-client-java because the scala version lacks write support:
   https://github.com/influxdata/influxdb-client-java/issues/125 */
libraryDependencies += "org.influxdb" % "influxdb-java" % "2.20" //for 1.x APIs
libraryDependencies += "com.influxdb" % "influxdb-client-scala" % "1.13.0" //for 2.x APIs (Flux)
libraryDependencies += "com.influxdb" % "influxdb-client-java" % "1.13.0" //for 2.x APIs (Flux)
libraryDependencies += "com.influxdb" % "flux-dsl" % "1.13.0" //optional Flux dsl
libraryDependencies += "com.softwaremill.sttp.client3" %% "core" % "3.0.0-RC7" //http client
libraryDependencies += "com.google.code.gson" % "gson" % "2.8.6" //json parsing
libraryDependencies += "dev.zio" %% "zio" % "1.0.3" //better implementation of Future

assemblyJarName in assembly := "ats-poc.jar"

unmanagedSourceDirectories in Compile += file("influx-client/src")