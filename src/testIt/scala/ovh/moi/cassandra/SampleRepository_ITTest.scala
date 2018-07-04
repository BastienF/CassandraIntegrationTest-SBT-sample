package ovh.moi.cassandra

import com.datastax.driver.core._
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import scala.io.Source
import com.datastax.driver.core._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar


class SampleRepository_ITTest extends FlatSpec with MockitoSugar with BeforeAndAfterAll with BeforeAndAfterEach {

  class TestSampleRepository(session: Session,
                             mockGetStatement: PreparedStatement = mock[PreparedStatement],
                             mockInsertStatement: PreparedStatement = mock[PreparedStatement],
                             mockInitTableStatement: PreparedStatement = mock[PreparedStatement]) extends SampleRepository(session) {
    override lazy val getStatement: PreparedStatement = mockGetStatement
    override lazy val insertStatement: PreparedStatement = mockInsertStatement
    override lazy val initTableStatement: PreparedStatement = mockInitTableStatement
  }

  private val cassandraConfig = ConfigFactory.parseString(Source.fromResource("cassandra.conf").getLines().mkString("\n"))
  private val hosts = cassandraConfig.getStringList("cassandra.hostnames").asScala.toSeq
  private val port = cassandraConfig.getInt("cassandra.port")
  private val username = cassandraConfig.getString("cassandra.username")
  private val password = cassandraConfig.getString("cassandra.password")
  private val keyspace = cassandraConfig.getString("cassandra.keyspace")
  private val testCluster: Cluster = Cluster.builder()
    .withAuthProvider(new PlainTextAuthProvider(username, password))
    .addContactPoints(hosts: _*)
    .withPort(port)
    .build()
  private val testSession = testCluster.connect(keyspace)

  override def beforeAll(): Unit = testSession.execute(SampleRepository.tableSchema)

  override def afterAll(): Unit = {
    testSession.close()
    testCluster.close()
  }

  override def beforeEach(): Unit = testSession.execute(s"TRUNCATE ${SampleRepository.TABLE_NAME};")

  "get" should "return None when key not found" in {
    //GIVEN

    val givenKey = "unknownKey"
    val expectedResult = None
    val cassandraDatabase: CassandraDatabase = new CassandraDatabase(cassandraConfig)
    val sampleRepository = cassandraDatabase.sampleRepository

    //WHEN
    val result = sampleRepository.get(givenKey)
    cassandraDatabase.stop()

    //THEN
    assert(result.equals(expectedResult))
  }

  "get" should "return Value when key found" in {
    //GIVEN
    val givenKey = "knownKey"
    val expectedResult = Some("value")
    testSession.execute(s"INSERT INTO ${SampleRepository.TABLE_NAME} (key, value) VALUES ('$givenKey', '${expectedResult.get}');")

    val cassandraDatabase: CassandraDatabase = new CassandraDatabase(cassandraConfig)
    val sampleRepository = cassandraDatabase.sampleRepository

    //WHEN
    val result = sampleRepository.get(givenKey)
    cassandraDatabase.stop()

    //THEN
    assert(result.equals(expectedResult))
  }
}
