package com.mz.example.actors.actions

import akka.actor.Actor.Receive
import akka.actor.{PoisonPill, Actor, ActorLogging}
import akka.util.Timeout
import com.mz.example.actors.jdbc.JDBCConnectionActor
import com.mz.example.actors.jdbc.JDBCConnectionActorMessages.{Rollback, Commit}
import com.mz.example.actors.repositories.{AddressRepositoryActor, UserRepositoryActor}
import com.mz.example.actors.services.UserServiceActorMessages.{UserRegistrated, RegistrateUser}
import com.mz.example.actors.services.{UserServiceActor, AddressServiceActor}
import akka.pattern._
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Failure, Success}

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

  implicit val timeout: Timeout = 5 seconds

  override def receive: Receive = {
    case RegistrateUser(user, address) => {
      val p = Promise[Boolean]
      (userService ? RegistrateUser(user, address))
        .mapTo[UserRegistrated] onComplete {
        case Success(s) => {
          log.debug("Registrate user - success!")
          jdbcConActor ! Commit
          p.success(true)
        }
        case Failure(f) => {
          log.error(f, f.getMessage)
          p.failure(f)
          jdbcConActor ! Rollback
          self ! PoisonPill
        }
      }
      p.future pipeTo sender
    }

  }

}
