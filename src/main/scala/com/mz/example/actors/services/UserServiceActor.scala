package com.mz.example.actors.services

import akka.actor.Actor.Receive
import akka.actor.{Props, ActorRef, ActorLogging, Actor}
import akka.util.Timeout
import com.mz.example.actors.common.messages.Messages.UnsupportedOperation
import com.mz.example.actors.repositories.common.messages.{SelectById, Inserted}
import com.mz.example.actors.repositories.common.messages.UserRepositoryActorMessages.InsertUser
import com.mz.example.actors.services.AddressServiceActorMessages.{AddressCreated, CreateAddress}
import com.mz.example.actors.services.UserServiceActorMessages._
import com.mz.example.domains.{Address, User}
import akka.pattern._
import scala.concurrent.duration._

import scala.concurrent.{Promise, Future}
import scala.util.{Failure, Success}

/**
 * Created by zemo on 18/10/15.
 */
class UserServiceActor(userRepProps: Props, addressServiceProps: Props) extends Actor with ActorLogging {

  import context.dispatcher

  private implicit val timeout: Timeout = 2000 milliseconds

  val userRepository = context.actorOf(userRepProps)

  val addressService = context.actorOf(addressServiceProps)

  override def receive: Receive = {
    case CreateUser(user) => createUser(user) pipeTo sender
    case FindUserById(id) => findUserById(id) pipeTo sender
    case DeleteUser(user) => deleteUser(user) pipeTo sender
    case UpdateUser(user) => updateUser(user) pipeTo sender
    case RegistrateUser(user, address) => registrateUser(user, address) pipeTo sender
    case _ => sender ! UnsupportedOperation
  }

  private def registrateUser(user: User, address: Address): Future[UserRegistrated] = {
    log.info("registrateUser ->")
    val p = Promise[UserRegistrated]
    (addressService ? CreateAddress(address)).mapTo[AddressCreated].onComplete {
      case Success(s) =>
        (self ? CreateUser(User(0, user.firstName, user.lastName, Some(s.id), None)))
          .mapTo[UserCreated].onComplete  {
          case Success(s) => {
            log.debug("registrateUser - success!")
            p.success(UserRegistrated())
          }
          case Failure(f) => {
            log.error(f, f.getMessage)
            p.failure(f)
          }
        }
      case Failure(f) => {
        log.error(f, f.getMessage)
        p.failure(f)
      }
    }
    p.future
  }

  /**
   * Create user
   * @param user
   * @return
   */
  private def createUser(user: User): Future[UserCreated] = {
    log.info(s"createUser first name = ${user.firstName}, last name = ${user.lastName}")
    val p = Promise[UserCreated]
    (userRepository ? InsertUser(user)).mapTo[Inserted] onComplete {
      case Success(s) => {
        log.info("createUser - success!")
        p.success(UserCreated(s.id))
      }
      case Failure(f) => {
        log.error(f, f.getMessage)
        p.failure(f)
      }
    }
    p.future
  }

  /**
   * Find user by id
   * @param id
   * @return
   */
  private def findUserById(id: Long): Future[FoundUsers] = {
    log.info(s"findUserById - id = $id")
    val p = Promise[FoundUsers]
    (userRepository ? SelectById(id)).mapTo[Option[User]] onComplete {
      case Success(s) => {
        log.info("findUserById - success!")
        s match {
          case s:Some[User] => p.success(FoundUsers(List(s.get)))
          case None => p.success(FoundUsers(Nil))
          case _ => {
            log.warning("Unsupported message type!")
            p.failure(new RuntimeException("Unsupported message type"))
          }
        }
      }
      case Failure(f) => {
        log.error(f, f.getMessage)
        p.failure(f)
      }
    }
    p.future
  }

  /**
   * Delete user
   * @param user - User to delete
   * @return Future[UserDeleted]
   */
  private def deleteUser(user: User): Future[UserDeleted] = {
    import com.mz.example.actors.repositories.common.messages.UserRepositoryActorMessages.DeleteUser
    val p = Promise[UserDeleted]
    (userRepository ? DeleteUser(user.id)).mapTo[Boolean] onComplete {
      case Success(success) => {
        log.info("User delete success!")
        p.success(UserDeleted())
      }
      case Failure(f) => {
        log.error(f, f.getMessage)
        p.failure(f)
      }
    }
    p.future
  }

  /**
   * Update user
   * @param user
   * @return
   */
  private def updateUser(user: User): Future[UserUpdateResult] = {
    import com.mz.example.actors.repositories.common.messages.UserRepositoryActorMessages.UpdateUser
    val p = Promise[UserUpdateResult]
    (userRepository ? UpdateUser(user)).mapTo[Boolean] onComplete {
      case Success(true) => {
        p.success(UserUpdated())
      }
      case Success(false) => {
        p.success(UserNotUpdated())
      }
      case Failure(f) => {
        log.error(f, f.getMessage)
        p.failure(f)
      }
    }
    p.future
  }

}

object UserServiceActor {

  /**
   * Create Props
   * @param userRepProps
   * @param addressServiceProps
   * @return Props
   */
  def props(userRepProps: Props, addressServiceProps: Props): Props = Props(classOf[UserServiceActor], userRepProps, addressServiceProps)
}
