package com.mz.example.actors.supervisors

import java.net.ConnectException
import java.sql.SQLException
import akka.actor.SupervisorStrategy._
import akka.actor.{Props, OneForOneStrategy, Actor, ActorLogging}
import com.mz.example.actors.jdbc.DataSourceActor
import org.postgresql.util.PSQLException
import scala.concurrent.duration._

case class CreatedActorMsg(props: Props, name: String)

/**
 * Created by zemi on 5. 11. 2015.
 */
class DataSourceSupervisorActor extends Actor with ActorLogging {

  val dataSourceActor = context.actorOf(DataSourceActor.props, DataSourceActor.actorName)

  context.watch(dataSourceActor)

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: SQLException              => Restart
      case _: PSQLException             => Restart
      case _: ConnectException          => Restart
      case _: NullPointerException      => Restart
//      case _: IllegalArgumentException => Stop
      case _: IllegalArgumentException  => Escalate
      case _: Exception                 => Escalate
    }

  override def receive: Receive = {
    case CreatedActorMsg(props, name) => sender ! context.actorOf(props, name)
    case props:Props => sender ! context.actorOf(props)
  }
}

object DataSourceSupervisorActor {

  val actorName = "dataSourceSupervisor"

  def props: Props = Props[DataSourceSupervisorActor]
}
