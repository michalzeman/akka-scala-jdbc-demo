package com.mz.example.actors.actions

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging}
import com.mz.example.actors.jdbc.JDBCConnectionActor
import com.mz.example.actors.jdbc.JDBCConnectionActorMessages.Commit
import com.mz.example.actors.repositories.{AddressRepositoryActor, UserRepositoryActor}
import com.mz.example.actors.services.UserServiceActorMessages.{UserRegistrated, RegistrateUser}
import com.mz.example.actors.services.{UserServiceActor, AddressServiceActor}

/**
 * Created by zemo on 27/10/15.
 */
class UserActionActor extends Actor with ActorLogging {

  val jdbcConActor = context.actorOf(JDBCConnectionActor.props)

  context.watch(jdbcConActor)

  val userRepositoryProps = UserRepositoryActor.props(jdbcConActor)

  val addressRepositoryProps = AddressRepositoryActor.props(jdbcConActor);

  val addressService = AddressServiceActor.props(userRepositoryProps, addressRepositoryProps)

  val userService = context.actorOf(UserServiceActor.props(userRepositoryProps, addressService))

  context.watch(userService)

  override def receive: Receive = {
    case RegistrateUser(user, address) => userService ! RegistrateUser(user, address)
    case UserRegistrated() => {
      log.info("User registered!")
      jdbcConActor ! Commit
    }
  }

}
