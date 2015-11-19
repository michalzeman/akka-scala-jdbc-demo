package com.mz.example.actors.services

import akka.actor.{ActorLogging, Actor, Props}
import akka.util.Timeout
import akka.pattern._
import com.mz.example.actors.common.messages.Messages.UnsupportedOperation
import com.mz.example.actors.repositories.AddressRepositoryActor.SelectAddress
import com.mz.example.actors.repositories.common.messages.Inserted
import com.mz.example.actors.services.AddressServiceActor
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
    case DeleteAddress(id) => delete(id) pipeTo sender
    case FindAddress(address) => findByAllAttributes(address) pipeTo sender
    case FindOrCreateAddress(address) => findOrCreate(address) pipeTo sender
    case _ => sender ! UnsupportedOperation
  }

  private def findByAllAttributes(address: Address): Future[FoundAddresses] = {
    log.debug("findByAllAttributes")
    val p = Promise[FoundAddresses]
    (addressRepository ? SelectAddress(address)).mapTo[Seq[Address]] onComplete {
      case Success(s) => {
        log.debug("findByAllAttributes - success!")
        p.success(FoundAddresses(s))
      }
      case Failure(f) => {
        log.error(f, f.getMessage)
        p.failure(f)
      }
    }
    p.future
  }

  private def findOrCreate(address: Address): Future[FoundAddresses] = {
    log.debug("findOrCreate")
    val p = Promise[FoundAddresses]
    (self ? FindAddress(address)).mapTo[FoundAddresses] onComplete {
      case Success(s) => {
        log.debug("findOrCreate - success!")
        if (s.addresses.size > 0) p.success(s)
        else (self ? CreateAddress(address)).mapTo[AddressCreated] onComplete {
          case Success(s) => {
            //street: String, zip: String, houseNumber: String, city: String
            p.success(FoundAddresses(List(Address(s.id, address.street, address.zip, address.houseNumber, address.city))))
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

  /**
   * Delete address
   * @param address
   * @return
   */
  private def delete(address: Address): Future[AddressDeleteResult] = {
    import com.mz.example.actors.repositories.AddressRepositoryActor.DeleteAddress
    val p = Promise[AddressDeleteResult]
    log.debug(s"delete address id = ${address.id}")
    (addressRepository ? DeleteAddress(address.id)).mapTo[Boolean] onComplete{
      case Success(true) => {
        log.debug(s"delete success with result = true")
        p.success(AddressDeleted())
      }
      case Success(false) => {
        log.debug(s"delete success with result = false")
        p.success(AddressNotDeleted())
      }
      case Failure(f) => {
        log.error(f, f.getMessage)
        p.failure(f)
      }
    }
    p.future
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
    import com.mz.example.actors.repositories.AddressRepositoryActor.InsertAddress
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
