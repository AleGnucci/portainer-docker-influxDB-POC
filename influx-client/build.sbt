name := "influx-client"

version := "0.1"

scalaVersion := "2.13.3"

libraryDependencies += "org.influxdb" % "influxdb-java" % "2.20"
libraryDependencies += "com.influxdb" % "influxdb-client-java" % "1.13.0"
libraryDependencies += "com.softwaremill.sttp.client3" %% "core" % "3.0.0-RC7"
libraryDependencies += "com.google.code.gson" % "gson" % "2.8.6"

assemblyJarName in assembly := "ats-poc.jar"

unmanagedSourceDirectories in Compile += file("influx-client/src")