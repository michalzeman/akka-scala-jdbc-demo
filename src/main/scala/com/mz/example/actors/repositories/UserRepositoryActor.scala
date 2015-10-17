package com.mz.example.actors.repositories

import akka.actor.{Props, ActorLogging, Actor, ActorRef}
import akka.pattern._
import akka.util.Timeout
import com.mz.example.actors.common.messages.Messages.UnsupportedOperation
import com.mz.example.actors.jdbc.JDBCConnectionActorMessages._
import com.mz.example.actors.repositories.common.messages.{Inserted, SelectById, UserRepositoryActorMessages}
import UserRepositoryActorMessages._
import com.mz.example.domains.User
import com.mz.example.domains.sql.mappers.UserMapper
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

/**
 * Created by zemi on 8. 10. 2015.
 */
class UserRepositoryActor(jdbcActor: ActorRef) extends Actor with ActorLogging with UserMapper {

  import context.dispatcher

  context.watch(jdbcActor)

  private implicit val timeout: Timeout = 2000.milliseconds

  override def receive: Receive = {
    case SelectById(id) => selectById(id) pipeTo sender
    case UpdateUser(user) => update(user) pipeTo sender
    case DeleteUser(id) => delete(id) pipeTo sender
    case InsertUser(user) => insert(user) pipeTo sender
    case UnsupportedOperation => log.debug(s"sender sent UnsupportedOperation $sender")
    case _ => sender ! UnsupportedOperation
  }

  /**
   * Select user by id
   * @param id
   * @return
   */
  private def selectById(id: Long): Future[Option[User]] = {
    log.debug("SelectById")
    val p = Promise[Option[User]]
    Future {
      (jdbcActor ? Select(s"select $ID_COL, $LAST_NAME_COL, $FIRST_NAME_COL, $ADDRESS_ID_COL " +
        s"from $TABLE_NAME where $ID_COL = $id", mapResultSet)).mapTo[SelectResult[Option[User]]] onComplete {
        case Success(result) => p.success(result.result)
        case Failure(f) => {
          log.error(f, f.getMessage)
          p.failure(f)
        }
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
    val p = Promise[Boolean]
    Future {
      (jdbcActor ? Update(
        s"""UPDATE $TABLE_NAME SET $FIRST_NAME_COL = '${user.firstName}', $LAST_NAME_COL = '${user.lastName}'
           WHERE $ID_COL = ${user.id}""")).mapTo[Boolean] onComplete {
        case Success(s) => p.success(s)
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
    val p = Promise[Boolean]
    Future {
      (jdbcActor ? Delete(s"DELETE FROM $TABLE_NAME WHERE id = $id")).mapTo[Boolean] onComplete {
        case Success(s) => p.success(s)
        case Failure(f) => {
          log.error(f, f.getMessage)
          p.failure(f)
        }
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
    val p = Promise[Inserted]
    Future {
      (jdbcActor ? Insert(
        s"""INSERT INTO $TABLE_NAME ($LAST_NAME_COL, $FIRST_NAME_COL)
           VALUES ('${user.lastName}', '${user.firstName}')""")).mapTo[GeneratedKeyRes] onComplete {
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
  /**
   * Create Props for an actor of this type
   * @return a Props
   */
  def props(jdbcConRef: ActorRef): Props = Props(classOf[UserRepositoryActor], jdbcConRef)
}