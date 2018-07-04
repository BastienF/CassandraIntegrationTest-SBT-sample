import sbt.Keys._
import sbt._

import scala.language.postfixOps
import scala.sys.process._

name := "CassandraIntegrationTest-SBT-sample"

version := "0.1"

scalaVersion := "2.12.6"

Defaults.itSettings

val keyspace: String = "sample_keyspace"

libraryDependencies ++= Seq(
  "com.datastax.cassandra" % "cassandra-driver-core" % "3.3.0",
  "com.typesafe" % "config" % "1.3.1",
  "org.scalatest" % "scalatest_2.12" % "3.0.4",
  "org.mockito" % "mockito-core" % "2.12.0")

lazy val `CassandraIntegrationTest-SBT-sample` = (project in file(".")).configs(ITest).settings(inConfig(ITest)(Defaults.testSettings): _*)

lazy val ITest = config("it") extend Test

scalaSource in ITest := baseDirectory.value / "src" / "testIt" / "scala"
resourceDirectory in ITest := baseDirectory.value / "src" / "testIt" / "resources"

lazy val cleanItTests = taskKey[Unit]("clean integration test containers")

lazy val startDseIt = TaskKey[Unit]("startDSEInt", "Start a local Docker DSE Cassandra instance for integration tests")

val dockerScriptsDir = """./automation/docker/cassandra"""

(startDseIt in ITest) := {
  val containerUuid = System.getProperty("containerUuid", "it")
  println("containerUuid : " + containerUuid)
  val cmdDeploy = s"""$dockerScriptsDir/deploy.sh --uuid $containerUuid --port 9043""".!
  if (cmdDeploy != 0) throw new Exception(s"$dockerScriptsDir/deploy.sh failed with exit code $cmdDeploy")
  val cmdTruncateTable = s"""$dockerScriptsDir/truncate_all_tables.sh --docker-hostname test_dse_$containerUuid --keyspace $keyspace""".!
  if (cmdTruncateTable != 0) throw new Exception(s"$dockerScriptsDir/truncate_all_tables.sh failed with exit code $cmdTruncateTable")
}

cleanItTests := {
  val containerUuid = System.getProperty("containerUuid", "it")
  s"""$dockerScriptsDir/deploy.sh --uuid $containerUuid --kill""".!
}

(test in ITest) := {
  (test in ITest).dependsOn(startDseIt in ITest)
}.value
