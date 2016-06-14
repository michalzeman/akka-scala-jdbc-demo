package com.mz.example.actors.services

import akka.actor.Props
import akka.pattern._
import com.mz.example.actors.services.AddressServiceActor._
import com.mz.example.actors.services.common.AbstractDomainServiceActor
import com.mz.example.actors.services.messages._
import com.mz.example.domains.Address

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

/**
 * Created by zemi on 23. 10. 2015.
 */
class AddressServiceActor(userRepProps: Props, addressRepProps: Props) extends AbstractDomainServiceActor[Address](addressRepProps)  {


  val userRepository = context.actorOf(userRepProps)

  override def receive: Receive = addressServiceReceive orElse super.receive

  def addressServiceReceive: Receive = {
    case FindOrCreateAddress(address) => findOrCreate(address) pipeTo sender
  }

  private def findOrCreate(address: Address): Future[Found[Address]] = {
    log.debug("findOrCreate")
    val p = Promise[Found[Address]]
    (self ? FindById(address.id)).mapTo[Found[Address]] onComplete {
      case Success(s) => {
        log.debug("findOrCreate - success!")
        if (!s.results.isEmpty) p.success(s)
        else (self ? Create(address)).mapTo[Created] onComplete {
          case Success(s) => {
            //street: String, zip: String, houseNumber: String, city: String
//            p.success(Found(List(Address(s.id, address.street, address.zip, address.houseNumber, address.city))))
            (self ? FindById(s.id)).mapTo[Found[Address]] onComplete {
              case Success(result) => {
                p.success(result)
              }
              case Failure(f) => {
                log.error(f, f.getMessage)
                p.failure(f)
              }
            }
          }
          case Failure(f) => {
            log.error(f, f.getMessage)
            p.failure(f)
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
}

object AddressServiceActor {

  case class FindOrCreateAddress(address: Address)

  /**
  * Create Props
  * @param userRepProps
  * @param addressRepProps
  * @return Props
  */
  def props(userRepProps: Props, addressRepProps: Props): Props = Props(classOf[AddressServiceActor], userRepProps, addressRepProps)
}
