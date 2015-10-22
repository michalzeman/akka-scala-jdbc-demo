package com.mz.example.actors.services

import akka.actor.Actor.Receive
import akka.actor.{Props, ActorRef, ActorLogging, Actor}
import akka.util.Timeout
import com.mz.example.actors.repositories.common.messages.{SelectById, Inserted}
import com.mz.example.actors.repositories.common.messages.UserRepositoryActorMessages.InsertUser
import com.mz.example.actors.services.UserServiceActorMessages._
import com.mz.example.domains.User
import akka.pattern._
import scala.concurrent.duration._

import scala.concurrent.{Promise, Future}
import scala.util.{Failure, Success}

/**
 * Created by zemo on 18/10/15.
 */
class UserServiceActor(userRepProps: Props, addressRepProps: Props) extends Actor with ActorLogging {

  import context.dispatcher

  private implicit val timeout: Timeout = 2000 milliseconds

  val userRepository = context.actorOf(userRepProps)

  val addressRepository = context.actorOf(addressRepProps)

  override def receive: Receive = {
    case CreateUser(firstName, lastName) => createUser(firstName, lastName) pipeTo sender
    case FindUserById(id) => findUserById(id) pipeTo sender
    case DeleteUser(user) => deleteUser(user) pipeTo sender
    case UpdateUser(user) => updateUser(user) pipeTo sender
  }

  /**
   * Create user
   * @param firstName
   * @param lastName
   * @return
   */
  private def createUser(firstName: String, lastName: String): Future[UserCreated] = {
    log.info(s"createUser first name = $firstName, last name = $lastName")
    val p = Promise[UserCreated]
    (userRepository ? InsertUser(User(0, firstName, lastName, None, None))) onComplete {
      case Success(s) => {
        s match {
          case result: Inserted => {
            log.info("createUser - success!")
            p.success(UserCreated(result.id))
          }
          case _ => {
            log.warning("Unsupported message type")
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
   * Find user by id
   * @param id
   * @return
   */
  private def findUserById(id: Long): Future[FoundUsers] = {
    log.info(s"findUserById - id = $id")
    val p = Promise[FoundUsers]
    (userRepository ? SelectById(id)) onComplete {
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
    val p = Promise[UserDeleted]
    (userRepository ?
      com.mz.example.actors.repositories.common.messages.UserRepositoryActorMessages.DeleteUser(user.id)) onComplete {
      case Success(success) => success match {
        case true => {
          log.info("User delete success!")
          p.success(UserDeleted())
        }
        case _ => {
          log.warning("Unsupported message type!")
          p.failure(new RuntimeException("Unsupported message type"))
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
   * Update user
   * @param user
   * @return
   */
  private def updateUser(user: User): Future[UserUpdated] = {
    import com.mz.example.actors.repositories.common.messages.UserRepositoryActorMessages.UpdateUser
    val p = Promise[UserUpdated]
    (userRepository ? UpdateUser(user)) onComplete {
      case Success(s) => s match {
        case true => {
          p.success(UserUpdated())
        }
        case _ => {
          log.warning("Unsupported message type!")
          p.failure(new RuntimeException("Unsupported message type"))
        }
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
   * @param addressRepProps
   * @return Props
   */
  def props(userRepProps: Props, addressRepProps: Props): Props = Props(classOf[UserServiceActor], userRepProps, addressRepProps)
}
