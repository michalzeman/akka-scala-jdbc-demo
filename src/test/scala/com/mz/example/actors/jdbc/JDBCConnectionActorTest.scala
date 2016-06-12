package com.mz.example.actors.jdbc

import java.sql.ResultSet

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.mz.example.actors.jdbc.JDBCConnectionActor._
import com.mz.example.actors.supervisors.DataSourceSupervisorActor
import com.mz.example.domains.User
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}
import org.scalatest.mock.MockitoSugar
import org.scalautils.ConversionCheckedTripleEquals

import scala.concurrent.duration._

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

  test("GetConnection timeout") {
    val jdbcActor = system.actorOf(JDBCConnectionActor.props)
    val query = "Select from users where id = 0"
    def mapper (resultSet: ResultSet): Option[User] = {None}
    jdbcActor ! JdbcSelect(query, mapper)
    expectNoMsg(2 seconds)
  }

  test("select operation") {
    val dataSourceSupervisor = system.actorOf(DataSourceSupervisorActor.props, DataSourceSupervisorActor.actorName)
    val jdbcActor = system.actorOf(JDBCConnectionActor.props)
    val query = "Select from users where id = 0"
    def mapper (resultSet: ResultSet): Option[User] = {None}
    jdbcActor ! JdbcSelect(query, mapper)
    expectMsgAnyOf(JdbcSelectResult(None))
  }
}
