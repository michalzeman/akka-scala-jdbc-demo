package com.mz.example.actors.services

import akka.actor.Props
import akka.pattern._
import com.mz.example.actors.services.UserServiceActor._
import com.mz.example.actors.services.common.AbstractDomainServiceActor
import com.mz.example.actors.services.messages._
import com.mz.example.domains.{Address, User}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

/**
 * Created by zemo on 18/10/15.
 */
class UserServiceActor(userRepProps: Props, addressServiceProps: Props) extends AbstractDomainServiceActor[User](userRepProps) {

  val addressService = context.actorOf(addressServiceProps)

  override def receive = userReceive orElse super.receive

  def userReceive: Receive = {
    case RegistrateUser(user, address) => registrateUser(user, address) pipeTo sender
  }

  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    log.debug("Actor stop")
    super.postStop()
  }

  private def registrateUser(user: User, address: Address): Future[UserRegistrated] = {
    log.info("registrateUser ->")
    val p = Promise[UserRegistrated]
    (addressService ? Create(address)).mapTo[Created].onComplete {
      case Success(s) =>
        (self ? Create(User(0, user.firstName, user.lastName, Some(s.id), None)))
          .mapTo[Created].onComplete  {
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

}

object UserServiceActor {

  case class RegistrateUser(user: User, address: Address)

  case class UserRegistrated()

  /**
   * Create Props
   * @param userRepProps
   * @param addressServiceProps
   * @return Props
   */
  def props(userRepProps: Props, addressServiceProps: Props): Props = Props(classOf[UserServiceActor], userRepProps, addressServiceProps)
}
