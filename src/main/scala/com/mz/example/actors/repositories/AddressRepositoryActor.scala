package com.mz.example.actors.repositories

import akka.actor.{ActorLogging, Actor, Props, ActorRef}
import akka.util.Timeout
import akka.pattern.{ask, pipe}
import com.mz.example.actors.common.messages.Messages.UnsupportedOperation
import com.mz.example.actors.jdbc.JDBCConnectionActor._
import com.mz.example.actors.repositories.AddressRepositoryActor.{DeleteAddress, InsertAddress, UpdateAddress}
import com.mz.example.actors.repositories.common.messages.{SelectById, Inserted}
import com.mz.example.domains.sql.mappers.AddressMapper
import com.mz.example.domains.{Address}
import scala.concurrent.{Promise, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by zemo on 17/10/15.
 */
class AddressRepositoryActor(jdbcActor: ActorRef) extends Actor with ActorLogging with AddressMapper {

//  import context.dispatcher

  context.watch(jdbcActor)

  private implicit val timeout: Timeout = 2000.milliseconds


  override def receive: Receive = {
    case SelectById(id) => selectById(id) pipeTo sender
    case UpdateAddress(address) => update(address) pipeTo sender
    case DeleteAddress(id) => delete(id) pipeTo sender
    case InsertAddress(address) => insert(address) pipeTo sender
    case UnsupportedOperation => log.debug(s"sender sent UnsupportedOperation $sender")
    case _ => sender ! UnsupportedOperation
  }

  /**
   * Select Address by id
   * @param id
   * @return
   *         Address(id: Long, street: String, zip: String, houseNumber: String, city: String)
   */
  private def selectById(id: Long): Future[Option[Address]] = {
    log.debug("SelectById")
    val p = Promise[Option[Address]]
    (jdbcActor ? Select(s"select $ID_COL, $STREET_COL, $ZIP_COL, $HOUSE_NUMBER_COL, $CITY_COL " +
      s"from $TABLE_NAME where $ID_COL = $id", mapResultSet)).mapTo[SelectResult[Option[Address]]] onComplete {
      case Success(result) => p.success(result.result)
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
   *
   */
  private def update(address: Address): Future[Boolean] = {
    log.debug("update")
    val p = Promise[Boolean]
    (jdbcActor ? Update(
      s"""UPDATE $TABLE_NAME SET $STREET_COL = '${address.street}', $ZIP_COL = '${address.zip}',
          $HOUSE_NUMBER_COL = '${address.houseNumber}', $CITY_COL = '${address.city}'
         WHERE $ID_COL = ${address.id}""")).mapTo[Boolean] onComplete {
      case Success(s) => p.success(s)
      case Failure(f) => {
        log.error(f, f.getMessage)
        p.failure(f)
      }
    }
    p.future
  }

  /**
   * Delete Address by id
   * @param id - id of Address
   * @return Future of boolean
   */
  private def delete(id: Long): Future[Boolean] = {
    log.debug("delete")
    val p = Promise[Boolean]
    (jdbcActor ? Delete(s"DELETE FROM $TABLE_NAME WHERE $ID_COL = $id")).mapTo[Boolean] onComplete {
      case Success(s) => p.success(s)
      case Failure(f) => {
        log.error(f, f.getMessage)
        p.failure(f)
      }
    }
    p.future
  }

  /**
   * Insert Address
   * @param address
   * @return Address(id: Long, street: String, zip: String, houseNumber: String, city: String)
   */
  private def insert(address: Address): Future[Inserted] = {
    log.debug("insert")
    val p = Promise[Inserted]
    (jdbcActor ? Insert(
      s"""INSERT INTO $TABLE_NAME ($STREET_COL, $ZIP_COL, $HOUSE_NUMBER_COL, $CITY_COL)
         VALUES ('${address.street}', '${address.zip}', '${address.houseNumber}', '${address.city}')"""))
      .mapTo[GeneratedKeyRes] onComplete {
      case Success(result) => p.success(Inserted(result.id))
      case Failure(f) => {
        log.error(f, f.getMessage)
        p.failure(f)
      }
    }
    p.future
  }


}

object AddressRepositoryActor {

  case class UpdateAddress(address: Address)

  case class DeleteAddress(id: Long)

  case class InsertAddress(address: Address)

  /**
   * Create Props for an actor of this type
   * @return a Props
   */
  def props(jdbcConRef: ActorRef): Props = Props(classOf[AddressRepositoryActor], jdbcConRef)
}
