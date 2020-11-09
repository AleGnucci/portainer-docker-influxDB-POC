name := "influx-client"

version := "0.1"

scalaVersion := "2.13.3"

libraryDependencies += "org.influxdb" % "influxdb-java" % "2.20" //for 1.x APIs
libraryDependencies += "com.influxdb" % "influxdb-client-java" % "1.13.0" //for 2.x APIs (Flux)
libraryDependencies += "com.influxdb" % "flux-dsl" % "1.13.0" //optional Flux dsl
libraryDependencies += "com.softwaremill.sttp.client3" %% "core" % "3.0.0-RC7" //http client
libraryDependencies += "com.google.code.gson" % "gson" % "2.8.6" //json parsing

assemblyJarName in assembly := "ats-poc.jar"

unmanagedSourceDirectories in Compile += file("influx-client/src")