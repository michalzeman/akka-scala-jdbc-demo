package com.mz.example.actors.jdbc

import java.sql.{ResultSet, PreparedStatement, Statement, Connection}

import akka.testkit.TestKit
import akka.actor.ActorSystem
import com.mz.example.actors.jdbc.DataSourceActorMessages.{ConnectionResult, GetConnection}
import com.mz.example.actors.jdbc.JDBCConnectionActorMessages._
import com.mz.example.domains.User
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
class JDBCConnectionActorTest extends TestKit(ActorSystem("test-jdbc-demo-JDBCConnectionActorTest")) with FunSuiteLike
with BeforeAndAfterAll
with Matchers
with ConversionCheckedTripleEquals
with ImplicitSender
with MockitoSugar {

  implicit val timeOut: akka.util.Timeout = 2000.millisecond

  override def afterAll(): Unit = {
    system.shutdown()
  }
//
//  test("init") {
//    val jdbcActor = system.actorOf(JDBCConnectionActor.props)
//  }
//
//  test("Insert operation") {
//    val jdbcActor = system.actorOf(JDBCConnectionActor.props)
//
//    val con = mock[Connection]
//    val prdStatement = mock[PreparedStatement]
//    val resultSet = mock[ResultSet]
//    val query = "insert mock"
//
//    Await.result((jdbcActor ? Insert(query)), 5.seconds)
//  }
//
//  test("Update operation") {
//    val jdbcActor = system.actorOf(JDBCConnectionActor.props)
//
//    val query = "update mock"
//
//    Await.result((jdbcActor ? Update(query)), 5.seconds)
//  }
//
//  test("Delete operation") {
//    val jdbcActor = system.actorOf(JDBCConnectionActor.props)
//    val query = "delete mock"
//    Await.result((jdbcActor ? Delete(query)), 5.seconds)
//  }
//
//  test("Select operation") {
//    val jdbcActor = system.actorOf(JDBCConnectionActor.props)
//    val query = "Select mock"
//    def mapper (resultSet: ResultSet): Option[User] = {None}
//    Await.result((jdbcActor ? Select(query, mapper)), 5.seconds).isInstanceOf[SelectResult[Option[User]]] should equal(true)
//  }

  test("GetConnection timeout") {
    val jdbcActor = system.actorOf(JDBCConnectionActor.props)
    val query = "Select from users where id = 0"
    def mapper (resultSet: ResultSet): Option[User] = {None}
    jdbcActor ! Select(query, mapper)
    expectNoMsg(2 seconds)
  }

  test("select operation") {
    system.actorOf(DataSourceActor.props, DataSourceActor.actorName)
    val jdbcActor = system.actorOf(JDBCConnectionActor.props)
    val query = "Select from users where id = 0"
    def mapper (resultSet: ResultSet): Option[User] = {None}
    jdbcActor ! Select(query, mapper)
    expectMsgAnyOf(SelectResult(None))
  }
}
