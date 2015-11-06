package com.mz.example.actors.jdbc

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import com.mz.example.actors.factories.jdbc.DataSourceActorFactory
import com.mz.example.actors.jdbc.DataSourceActor
import com.mz.example.actors.jdbc.DataSourceActorMessages.{ConnectionResult, GetConnection}
import com.mz.example.actors.supervisors.{CreatedActorMsg, DataSourceSupervisorActor}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, BeforeAndAfterAll, FunSuiteLike}
import org.scalautils.ConversionCheckedTripleEquals
import akka.pattern.ask
import scala.concurrent.duration._
import scala.concurrent._

/**
 * Created by zemo on 05/10/15.
 */
class DataSourceActorTest extends TestKit(ActorSystem("test-jdbc-demo-DataSourceActorTest"))
with FunSuiteLike
with BeforeAndAfterAll
with Matchers
with ConversionCheckedTripleEquals
with ImplicitSender
with MockitoSugar
with DataSourceActorFactory {

  implicit val timeOut: akka.util.Timeout = 2000.millisecond

  override protected def afterAll(): Unit = {
    system.shutdown()
  }

  test("init dataSource") {
    val dataSourceSupervisor = system.actorOf(DataSourceSupervisorActor.props, DataSourceSupervisorActor.actorName)
    selectDataSourceActor(system) ! GetConnection
    expectMsgType[ConnectionResult]
  }
}
