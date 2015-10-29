package com.example

import akka.actor._
import com.mz.example.actors.HelloWorldActor
import com.mz.example.actors.actions.UserActionActor
import com.mz.example.actors.jdbc.{DataSourceActor, JDBCConnectionActor}
import com.mz.example.actors.jdbc.JDBCConnectionActorMessages.Init
import com.mz.example.actors.services.UserServiceActorMessages.RegistrateUser
import com.mz.example.domains.{Address, User}
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.pattern._

object HelloSimpleMain {

  implicit val timeOut: akka.util.Timeout = 10000.millisecond

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("Main")
    val dataSourceActor = system.actorOf(DataSourceActor.props, DataSourceActor.actorName)
    val futures =
      for (i <- 1 to 10000) yield {
        Thread sleep 5
        val userAction = system.actorOf(Props[UserActionActor])
        (userAction ? RegistrateUser(User(0, "FirstNameTest", "LastNameTest", None, None),
          Address(0, "test", "82109", "9A", "testCity")))
      }

    for {future <- futures} yield Await.result(future, 1 minutes)
    system.shutdown()
  }

}
