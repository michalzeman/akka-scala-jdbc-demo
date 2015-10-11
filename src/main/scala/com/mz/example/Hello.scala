package com.example

import akka.actor._
import com.mz.example.actors.HelloWorldActor
import com.mz.example.actors.jdbc.JDBCConnectionActor
import com.mz.example.actors.jdbc.JDBCConnectionActorMessages.Init

object HelloSimpleMain {

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("Main")
      val hello = system.actorOf(Props[HelloWorldActor])
//    val jdbc = system.actorOf(JDBCConnectionActor.props)
//    jdbc ! Init
  }

}
