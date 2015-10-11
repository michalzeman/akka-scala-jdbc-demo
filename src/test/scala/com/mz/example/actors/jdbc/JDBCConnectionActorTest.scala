package com.mz.example.actors.jdbc

import java.sql.{ResultSet, PreparedStatement, Statement, Connection}

import akka.testkit.TestKit
import akka.actor.ActorSystem
import com.mz.example.actors.jdbc.DataSourceActorMessages.{ConnectionResult, GetConnection}
import com.mz.example.actors.jdbc.JDBCConnectionActorMessages._
import org.scalatest.FunSuiteLike
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import akka.testkit.ImplicitSender
import akka.pattern.ask
import akka.testkit.TestProbe
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._;
import scala.concurrent.duration._
import scala.concurrent._
import org.scalautils.ConversionCheckedTripleEquals

import scala.util.{Failure, Success}

/**
 * Created by zemo on 04/10/15.
 */
class JDBCConnectionActorTest extends TestKit(ActorSystem("test-jdbc-demo")) with FunSuiteLike
with BeforeAndAfterAll
with Matchers
with ConversionCheckedTripleEquals
with ImplicitSender
with MockitoSugar {

  implicit val timeOut: akka.util.Timeout = 200.millisecond

  override def afterAll(): Unit = {
    system.shutdown()
  }

  test("init") {
    val dataSource = TestProbe()
    val jdbcActor = system.actorOf(JDBCConnectionActor.props(dataSource.ref))
    dataSource.expectMsg(GetConnection)
  }

  test("Insert operation") {
    val dataSource = TestProbe()
    val jdbcActor = system.actorOf(JDBCConnectionActor.props(dataSource.ref))
    dataSource.expectMsg(GetConnection)

    val con = mock[Connection]
    val prdStatement = mock[PreparedStatement]
    val resultSet = mock[ResultSet]
    val query = "insert mock"
    when(con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)).thenReturn(prdStatement)
    when(prdStatement.getGeneratedKeys).thenReturn(resultSet)
    when(resultSet.next).thenReturn(true)
    when(resultSet.getLong(1)).thenReturn(1024)
    dataSource.reply(ConnectionResult(con))

    Await.result((jdbcActor ? Insert(query)), 5.seconds)
  }

  test("Update operation") {
    val dataSource = TestProbe()
    val jdbcActor = system.actorOf(JDBCConnectionActor.props(dataSource.ref))
    dataSource.expectMsg(GetConnection)

    val con = mock[Connection]
    val prdStatement = mock[PreparedStatement]
    val query = "update mock"
    when(con.prepareStatement(query)).thenReturn(prdStatement)
    dataSource.reply(ConnectionResult(con))

    Await.result((jdbcActor ? Update(query)), 5.seconds)
  }

  test("Delete operation") {
    val dataSource = TestProbe()
    val jdbcActor = system.actorOf(JDBCConnectionActor.props(dataSource.ref))
    dataSource.expectMsg(GetConnection)

    val con = mock[Connection]
    val prdStatement = mock[PreparedStatement]
    val query = "delete mock"
    when(con.prepareStatement(query)).thenReturn(prdStatement)
    dataSource.reply(ConnectionResult(con))


    Await.result((jdbcActor ? Delete(query)), 5.seconds)
  }

  test("Select operation") {
    val dataSource = TestProbe()
    val jdbcActor = system.actorOf(JDBCConnectionActor.props(dataSource.ref))
    dataSource.expectMsg(GetConnection)

    val con = mock[Connection]
    val prdStatement = mock[PreparedStatement]
    val query = "Select mock"
    when(con.prepareStatement(query)).thenReturn(prdStatement)
    dataSource.reply(ConnectionResult(con))


    Await.result((jdbcActor ? Select(query)), 5.seconds).isInstanceOf[SelectResult] should equal(true)
  }
}
