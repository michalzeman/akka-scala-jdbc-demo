package com.mz.example.actors.supervisors

import akka.actor.Actor.Receive
import akka.actor.SupervisorStrategy._
import akka.actor.{Props, OneForOneStrategy, Actor, ActorLogging}
import scala.concurrent.duration._

/**
 * Created by zemi on 5. 11. 2015.
 */
class DataSourceSupervisorActor extends Actor with ActorLogging {

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: ArithmeticException      => Resume
      case _: NullPointerException     => Restart
      case _: IllegalArgumentException => Stop
      case _: Exception                => Escalate
    }

  override def receive: Receive = ???
}

object DataSourceSupervisorActor {
  def props: Props = Props[DataSourceSupervisorActor]
}
