package com.mz.example.actors.actions

import akka.actor.{PoisonPill, Props, ActorSystem}
import akka.testkit.{JavaTestKit, ImplicitSender, TestKit}
import com.mz.example.actors.jdbc.DataSourceActor
import com.mz.example.actors.services.UserServiceActorMessages.RegistrateUser
import com.mz.example.actors.supervisors.{CreatedActorMsg, DataSourceSupervisorActor}
import com.mz.example.domains.{Address, User}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, BeforeAndAfterAll, FunSuiteLike}
import org.scalautils.ConversionCheckedTripleEquals
import akka.pattern.ask
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

/**
 * Created by zemi on 29. 10. 2015.
 */
class UserActionActorIntegrationTest extends TestKit(ActorSystem("test-jdbc-demo-UserActionActorTest"))
with FunSuiteLike
with BeforeAndAfterAll
with Matchers
with ConversionCheckedTripleEquals
with ImplicitSender
with MockitoSugar {

  implicit val timeOut: akka.util.Timeout = 10000.millisecond

  import system.dispatcher

  val dataSourceSupervisor = system.actorOf(DataSourceSupervisorActor.props, DataSourceSupervisorActor.actorName)

  test("Registrate user") {
    val futures =
      for (i <- 1 to 10000) yield {
        Thread sleep 2
        val userAction = system.actorOf(Props[UserActionActor])
        (userAction ? RegistrateUser(User(0, "FirstNameTest", "LastNameTest", None, None),
          Address(0, "test", "82109", "9A", "testCity")))
      }

    for {future <- futures} yield Await.result(future, 1 minutes)

  }

  test("Parent acotr stop") {
    val userAction = system.actorOf(Props[UserActionActor])
    userAction ! PoisonPill
    expectNoMsg(200 microseconds)
    userAction ! RegistrateUser(User(0, "FirstNameTest", "LastNameTest", None, None)
      ,Address(0, "test", "82109", "9A", "testCity"))
    expectNoMsg(5 seconds)
  }

//  test("test one") {
//    val userAction = system.actorOf(Props[UserActionActor])
//
//    (userAction ? RegistrateUser(User(0, "FirstNameTest", "LastNameTest", None, None),
//      Address(0, "test", "82109", "9A", "testCity")))
//  }

  override protected def afterAll(): Unit = {
    system.shutdown()
  }
}
