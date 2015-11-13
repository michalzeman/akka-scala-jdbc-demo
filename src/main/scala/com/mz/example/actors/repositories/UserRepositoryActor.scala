package com.mz.example.actors.repositories

import akka.actor.{Props, ActorLogging, Actor, ActorRef}
import akka.pattern._
import akka.util.Timeout
import com.mz.example.actors.common.messages.Messages.UnsupportedOperation
import com.mz.example.actors.repositories.common.messages.{SelectAll, Inserted, SelectById}
import UserRepositoryActor._
import com.mz.example.domains.User
import com.mz.example.domains.sql.mappers.UserMapper
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import com.mz.example.actors.jdbc.JDBCConnectionActor._

/**
 * Created by zemi on 8. 10. 2015.
 */
class UserRepositoryActor(jdbcActor: ActorRef) extends Actor with ActorLogging with UserMapper {

  //import context.dispatcher

  context.watch(jdbcActor)

  private implicit val timeout: Timeout = 2000.milliseconds

  override def receive: Receive = {
    case SelectById(id) => selectById(id) pipeTo sender
    case UpdateUser(user) => update(user) pipeTo sender
    case DeleteUser(id) => delete(id) pipeTo sender
    case InsertUser(user) => insert(user) pipeTo sender
    case SelectAll => selectAll pipeTo sender
    case UnsupportedOperation => log.debug(s"sender sent UnsupportedOperation $sender")
    case _ => sender ! UnsupportedOperation
  }

  /**
   * Select all users
   * @return
   */
  private def selectAll: Future[List[User]] = {
    log.debug("selectAll")
    val p = Promise[List[User]]
    (jdbcActor ? Select(s"select $ID_COL, $LAST_NAME_COL, $FIRST_NAME_COL, $ADDRESS_ID_COL " +
      s"from $TABLE_NAME ", mapResultSetList)).mapTo[SelectResult[List[User]]] onComplete {
      case Success(result) => {
        log.debug("selectAll - success!")
        p.success(result.result)
      }
      case Failure(f) => {
        log.error(f, f.getMessage)
        p.failure(f)
      }
    }
    p.future
  }

  /**
   * Select user by id
   * @param id
   * @return
   */
  private def selectById(id: Long): Future[Option[User]] = {
    log.debug("SelectById")
    val p = Promise[Option[User]]
    log.debug("UserRepositoryActor going to selectId id = "+Thread.currentThread().getId())
    (jdbcActor ? Select(s"select $ID_COL, $LAST_NAME_COL, $FIRST_NAME_COL, $ADDRESS_ID_COL " +
      s"from $TABLE_NAME where $ID_COL = $id", mapResultSet)).mapTo[SelectResult[Option[User]]] onComplete {
      case Success(result) => {
        log.debug("UserRepositoryActor future execution of selectById id = "+Thread.currentThread().getId())
        p.success(result.result)
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
   */
  private def update(user: User): Future[Boolean] = {
    log.debug("update")
    log.debug("UserRepositoryActor going to update id = "+Thread.currentThread().getId())
    val p = Promise[Boolean]
    Future {
      val addressIdColm = user.addressId match {
        case Some(addressId) => s""", $ADDRESS_ID_COL = ${addressId}"""
        case None => ""
      }
      val updateQuery =
        s"""UPDATE $TABLE_NAME SET $FIRST_NAME_COL = '${user.firstName}',
           $LAST_NAME_COL = '${user.lastName}'""".stripMargin.concat(addressIdColm)
      val whereClause = s"""WHERE $ID_COL = ${user.id}"""
      (jdbcActor ? Update(updateQuery.concat(whereClause))).mapTo[Boolean] onComplete {
        case Success(s) => {
          log.debug("UserRepositoryActor future execution of update id = "+Thread.currentThread().getId())
          p.success(s)
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
   * Delete user by id
   * @param id - id of user
   * @return Future of boolean
   */
  private def delete(id: Long): Future[Boolean] = {
    log.debug("delete")
    log.debug("UserRepositoryActor going to delete id = "+Thread.currentThread().getId())
    val p = Promise[Boolean]
    (jdbcActor ? Delete(s"DELETE FROM $TABLE_NAME WHERE id = $id")).mapTo[Boolean] onComplete {
      case Success(s) => {
        log.debug("UserRepositoryActor future execution of delete id = "+Thread.currentThread().getId())
        p.success(s)
      }
      case Failure(f) => {
        log.error(f, f.getMessage)
        p.failure(f)
      }
    }
    p.future
  }

  /**
   * Insert User
   * @param user
   * @return
   */
  private def insert(user: User): Future[Inserted] = {
    log.debug("insert")
    log.debug("UserRepositoryActor going to insert id = "+Thread.currentThread().getId())
    val p = Promise[Inserted]
    Future {
      log.debug("UserRepositoryActor future execution of insert id = "+Thread.currentThread().getId())
      val addressId = user.addressId match {
        case Some(addressId) => s""", $addressId"""
        case None => ""
      }
      val columns = s"""$LAST_NAME_COL, $FIRST_NAME_COL""".concat(
        if(addressId.length > 0) s", $ADDRESS_ID_COL" else "")
      val values = s"""'${user.lastName}', '${user.firstName}'""".concat(addressId)
      (jdbcActor ? Insert(
        s"""INSERT INTO $TABLE_NAME ($columns)
           VALUES ($values)""")).mapTo[GeneratedKeyRes] onComplete {
        case Success(result) => p.success(Inserted(result.id))
        case Failure(f) => {
          log.error(f, f.getMessage)
          p.failure(f)
        }
      }
    }
    p.future
  }

}


object UserRepositoryActor {

  case class UpdateUser(user: User)

  case class DeleteUser(id: Long)

  case class InsertUser(user: User)

  /**
   * Create Props for an actor of this type
   * @return a Props
   */
  def props(jdbcConRef: ActorRef): Props = Props(classOf[UserRepositoryActor], jdbcConRef)
}