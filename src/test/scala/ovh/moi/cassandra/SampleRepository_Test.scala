package ovh.moi.cassandra

import com.datastax.driver.core._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar

class SampleRepository_Test extends FlatSpec with MockitoSugar {

  class TestSampleRepository(session: Session,
                             mockGetStatement: PreparedStatement = mock[PreparedStatement],
                             mockInsertStatement: PreparedStatement = mock[PreparedStatement],
                             mockInitTableStatement: PreparedStatement = mock[PreparedStatement]) extends SampleRepository(session) {
    override lazy val getStatement: PreparedStatement = mockGetStatement
    override lazy val insertStatement: PreparedStatement = mockInsertStatement
    override lazy val initTableStatement: PreparedStatement = mockInitTableStatement
  }

  "get" should "return None when key not found" in {
    //GIVEN
    val givenKey = "unknownKey"
    val expectedResult = None
    val mockedSession = mock[Session]
    val mockBoundStatement = mock[BoundStatement]
    val mockedGetStatement = mock[PreparedStatement]
    val mockedEmptyResultSet = mock[ResultSet]
    when(mockedGetStatement.bind(any)).thenReturn(mockBoundStatement)
    when(mockedSession.execute(any[BoundStatement])).thenReturn(mockedEmptyResultSet)
    when(mockedEmptyResultSet.one()).thenReturn(null)
    val sampleRepository = new TestSampleRepository(mockedSession, mockGetStatement = mockedGetStatement)

    //WHEN
    val result = sampleRepository.get(givenKey)

    //THEN
    assert(result.equals(expectedResult))
    verify(mockedGetStatement).bind(givenKey)
    verify(mockedSession).execute(mockBoundStatement)
    verify(mockedEmptyResultSet).one()
  }

  "get" should "return Value when key found" in {
    //GIVEN
    val givenKey = "knownKey"
    val expectedResult = Some("value")
    val mockedSession = mock[Session]
    val mockedBoundStatement = mock[BoundStatement]
    val mockedGetStatement = mock[PreparedStatement]
    val mockedResultSet = mock[ResultSet]
    val mockedRow = mock[Row]
    when(mockedGetStatement.bind(any)).thenReturn(mockedBoundStatement)
    when(mockedSession.execute(any[BoundStatement])).thenReturn(mockedResultSet)
    when(mockedResultSet.one()).thenReturn(mockedRow)
    when(mockedRow.getString("value")).thenReturn(expectedResult.get)
    val sampleRepository = new TestSampleRepository(mockedSession, mockGetStatement = mockedGetStatement)


    //WHEN
    val result = sampleRepository.get(givenKey)

    //THEN
    assert(result.equals(expectedResult))
    verify(mockedGetStatement).bind(givenKey)
    verify(mockedSession).execute(mockedBoundStatement)
    verify(mockedResultSet).one()
  }
}
