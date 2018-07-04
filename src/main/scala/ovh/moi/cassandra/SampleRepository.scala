package ovh.moi.cassandra

import com.datastax.driver.core.{PreparedStatement, ResultSet, Session}

object SampleRepository {
  val TABLE_NAME = "sample"

  val tableSchema = s"""CREATE TABLE IF NOT EXISTS $TABLE_NAME (
      key text,
      value text,
      PRIMARY KEY(key));"""
}

class SampleRepository(session: Session) {
  import SampleRepository._
  protected lazy val getStatement: PreparedStatement = session.prepare(s"SELECT * FROM $TABLE_NAME where key = ?;")
  protected lazy val insertStatement: PreparedStatement = session.prepare(s"INSERT INTO $TABLE_NAME (key, value) VALUES (?, ?);")
  protected lazy val initTableStatement: PreparedStatement = session.prepare(tableSchema)


  def get(key: String): Option[String] = {
    Option(session.execute(getStatement.bind(key)).one()).map(_.getString("value"))
  }

  def insert(key: String, value: String): ResultSet = {
    session.execute(insertStatement.bind(key, value))
  }

  def initTable(): ResultSet = {
    session.execute(initTableStatement.bind())
  }
}
