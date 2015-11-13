package com.mz.example.actors.services

import akka.actor.{ActorLogging, Actor, Props}
import akka.util.Timeout
import akka.pattern._
import com.mz.example.actors.common.messages.Messages.UnsupportedOperation
import com.mz.example.actors.repositories.AddressRepositoryActor.InsertAddress
import com.mz.example.actors.repositories.common.messages.Inserted
import com.mz.example.actors.services.AddressServiceActor._
import com.mz.example.domains.Address
import scala.concurrent.{Promise, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by zemi on 23. 10. 2015.
 */
class AddressServiceActor(userRepProps: Props, addressRepProps: Props) extends Actor with ActorLogging {

//  import context.dispatcher

  private implicit val timeout: Timeout = 2000 milliseconds

  val userRepository = context.actorOf(userRepProps)

  val addressRepository = context.actorOf(addressRepProps)

  override def receive: Receive = {
    case CreateAddress(address) => create(address) pipeTo sender
    case UpdateAddress(address) => update(address) pipeTo sender
    case _ => sender ! UnsupportedOperation
  }

  /**
   * Update Address
   * @param address
   * @return
   */
  private def update(address: Address): Future[AddressUpdateResult] = {
    import com.mz.example.actors.repositories.AddressRepositoryActor.UpdateAddress
    log.debug("Update address")
    val p = Promise[AddressUpdateResult]
    (addressRepository ? UpdateAddress(address)).mapTo[Boolean] onComplete {
      case Success(true) => {
        log.debug("Update address success! result true")
        p.success(AddressUpdated())
      }
      case Success(false) => {
        log.debug("Update address success! result false")
        p.success(AddressNotUpdated())
      }
      case Failure(f) => {
        log.error(f, f.getMessage)
        p.failure(f)
      }
    }
    p.future
  }

  /**
   * Create address
   * @param address
   * @return
   */
  private def create(address: Address): Future[AddressCreated] = {
    log.debug("Create address")
    val p = Promise[AddressCreated]
    (addressRepository ? InsertAddress(address)).mapTo[Inserted] onComplete {
      case Success(s) => {
        log.debug("Create address - success!")
        p.success(AddressCreated(s.id))
      }
      case Failure(f) => {
        log.debug("Create address - failed!")
        p.failure(f)
      }
    }
    p.future
  }
}

object AddressServiceActor {

  case class FindAddressById(id: Long)

  case class FindAddress(address: Address)

  case class FoundAddresses(addresses:Seq[Address])

  case class FindOrCreateAddress(address: Address)

  case class CreateAddress(address: Address)

  case class AddressCreated(id: Long)

  case class UpdateAddress(address: Address)

  trait AddressUpdateResult

  case class AddressUpdated() extends AddressUpdateResult

  case class AddressNotUpdated() extends AddressUpdateResult

  case class DeleteAddress(address: Address)

  trait AddressDeleteResult

  case class AddressDeleted() extends AddressDeleteResult

  case class AddressNotDeleted() extends AddressDeleteResult

  /**
  * Create Props
  * @param userRepProps
  * @param addressRepProps
  * @return Props
  */
  def props(userRepProps: Props, addressRepProps: Props): Props = Props(classOf[AddressServiceActor], userRepProps, addressRepProps)
}
