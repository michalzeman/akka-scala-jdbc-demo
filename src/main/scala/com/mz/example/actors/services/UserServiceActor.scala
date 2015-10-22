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
    Future {
      (userRepository ? InsertUser(User(0, firstName, lastName, None, None))).mapTo[Inserted] onComplete {
        case Success(s) => {
          log.info("createUser - success!")
          p.success(UserCreated(s.id))
        }
        case Failure(f) => {
          log.error(f, f.getMessage)
          p.failure(f)
        }
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
    Future {
      (userRepository ? SelectById(id)).mapTo[Option[User]] onComplete {
        case Success(s) => {
          log.info("findUserById - success!")
          s match {
            case Some(user) => p.success(FoundUsers(List(user)))
            case None => p.success(FoundUsers(Nil))
          }
        }
        case Failure(f) => {
          log.error(f, f.getMessage)
          p.failure(f)
        }
      }
    }
    p.future
  }

}

object UserServiceActor {

  /**
   * Create Props
   * @param jdbcActor
   * @return Props
   */
  def props(jdbcActor: ActorRef): Props = Props(classOf[UserServiceActor], jdbcActor)
}
