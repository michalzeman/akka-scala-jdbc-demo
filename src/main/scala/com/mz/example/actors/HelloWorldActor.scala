package com.mz.example.actors

import akka.actor.{ActorLogging, Actor, Props}
import com.typesafe.config.ConfigFactory

class HelloWorldActor extends Actor with ActorLogging {

  override def preStart(): Unit = {
    // create the greeter actor
    val greeter = context.actorOf(Props[GreeterActor], "greeter")
    val conf = ConfigFactory.load()
//    log.info("Application configuration = "+conf.getString("akka.test"))
    // Send it the 'Greet' message
//    context.actorSelection("").anchor
    greeter ! GreeterMessages.Greet
  }

  def receive = {
    // When we receive the 'Done' message, stop this actor
    // (which if this is still the initialActor will trigger the deathwatch and stop the entire ActorSystem)
    case GreeterMessages.Done => {
      context.stop(self)
    }
  }
}

