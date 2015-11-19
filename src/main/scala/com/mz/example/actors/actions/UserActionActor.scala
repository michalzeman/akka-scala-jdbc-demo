package com.mz.example.actors.actions

import com.mz.example.actors.jdbc.JDBCConnectionActor._
import akka.actor.{ActorRef, PoisonPill, Actor, ActorLogging}
import akka.util.Timeout
import com.mz.example.actors.jdbc.JDBCConnectionActor
import com.mz.example.actors.repositories.{AddressRepositoryActor, UserRepositoryActor}
import com.mz.example.actors.services.UserServiceActor.{UserRegistrated, RegistrateUser}
import com.mz.example.actors.services.{UserServiceActor, AddressServiceActor}
import scala.concurrent.duration._

/**
 * Created by zemo on 27/10/15.
 */
class UserActionActor extends Actor with ActorLogging {

  import context.dispatcher

  val jdbcConActor = context.actorOf(JDBCConnectionActor.props)
  context.watch(jdbcConActor)

  val userRepositoryProps = UserRepositoryActor.props(jdbcConActor)
  val addressRepositoryProps = AddressRepositoryActor.props(jdbcConActor);
  val addressService = AddressServiceActor.props(userRepositoryProps, addressRepositoryProps)
  val userService = context.actorOf(UserServiceActor.props(userRepositoryProps, addressService))
  context.watch(userService)

  var orgSender:ActorRef = _

  implicit val timeout: Timeout = 5 seconds

  override def receive: Receive = {
    case RegistrateUser(user, address) => {
      orgSender = sender
      userService ! RegistrateUser(user, address)
      //      val p = Promise[Boolean]
      //      (userService ? RegistrateUser(user, address))
      //        .mapTo[UserRegistrated] onComplete {
      //        case Success(s) => {
      //          log.debug("Registrate user - success!")
      //          jdbcConActor ! Commit
      //          p.success(true)
      //        }
      //        case Failure(f) => {
      //          log.error(f, f.getMessage)
      //          p.failure(f)
      //          jdbcConActor ! Rollback
      //          self ! PoisonPill
      //        }
      //      }
      //      p.future pipeTo sender
      //    }
    }
    case UserRegistrated() =>
    {
      log.debug("Registrate user - success!")
      jdbcConActor ! Commit
      orgSender ! true
    }
    case akka.actor.Status.Failure(e) =>
    {
      log.error(e, e.getMessage)
      jdbcConActor ! Rollback
      orgSender ! e
    }

  }

}
