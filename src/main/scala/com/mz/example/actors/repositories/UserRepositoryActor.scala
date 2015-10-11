package com.mz.example.actors.repositories

import java.sql.ResultSet

import akka.actor.{ActorLogging, Actor, ActorRef}
import akka.pattern._
import akka.util.Timeout
import com.mz.example.actors.common.messages.Messages.UnsupportedOperation
import com.mz.example.actors.jdbc.JDBCConnectionActorMessages.{Update, Select, SelectResult}
import com.mz.example.actors.repositories.UserRepositoryActorMessages._
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

  private implicit val timeout: Timeout = 5.seconds

  override def receive: Receive = {
    case SelectById(id) => selectById(id) pipeTo sender
    case UpdateUser(user) => update(user) pipeTo sender
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
        s"from $TABLE_NAME where $ID_COL = $id")).mapTo[SelectResult] onComplete {
        case Success(result) => p.success(mapResultSet(result.resultSet))
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
      (jdbcActor ? Update(s"UPDATE $TABLE_NAME SET $FIRST_NAME_COL = ${user.firstName}," +
        s" $LAST_NAME_COL = ${user.lastName} WHERE $ID_COL = ${user.id}")).mapTo[Boolean] onComplete {
        case Success(s) => p.success(s)
        case Failure(f) => {
          log.error(f, f.getMessage)
          p.failure(f)
        }
      }
    }
    p.future
  }

}
