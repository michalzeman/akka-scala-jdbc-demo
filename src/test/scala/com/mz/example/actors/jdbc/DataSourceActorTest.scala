package com.mz.example.actors.jdbc

import akka.actor.ActorSystem
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import com.mz.example.actors.jdbc.DataSourceActorMessages.{ConnectionResult, GetConnection}
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
with MockitoSugar {

  implicit val timeOut: akka.util.Timeout = 2000.millisecond

  override protected def afterAll(): Unit = {
    system.shutdown()
  }

  test("init dataSource") {
    val dataSource = system.actorOf(DataSourceActor.props, "dataSourceTest")
    val result = Await.result((dataSource ? GetConnection), 5.seconds)
    result.isInstanceOf[ConnectionResult] should equal(true)
  }
}
