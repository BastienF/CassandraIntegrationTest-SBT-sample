package ovh.moi.cassandra

import com.datastax.driver.core.{Cluster, PlainTextAuthProvider, Session}
import com.typesafe.config.Config

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

class CassandraDatabase(config: Config) {
  private val hosts = config.getStringList("cassandra.hostnames").asScala.toSeq
  private val port = config.getInt("cassandra.port")
  private val username = config.getString("cassandra.username")
  private val password = config.getString("cassandra.password")
  private val keyspace = config.getString("cassandra.keyspace")

  private val cluster: Cluster = Cluster.builder()
    .withAuthProvider(new PlainTextAuthProvider(username, password))
    .addContactPoints(hosts: _*)
    .withPort(port)
    .build()
  private val session: Session = cluster.connect(keyspace)
  val sampleRepository: SampleRepository = new SampleRepository(session)

  def stop(): Unit = {
    session.close()
    cluster.close()
  }
}
