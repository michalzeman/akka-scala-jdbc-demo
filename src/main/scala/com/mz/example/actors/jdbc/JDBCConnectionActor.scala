package com.mz.example.actors.jdbc

import java.sql.{ResultSet, Statement, SQLException, Connection}
import akka.actor._
import akka.util.Timeout
import scala.concurrent.{Promise, Future}
import scala.concurrent.duration._
import com.mz.example.actors.common.messages.Messages.{RetryOperation, UnsupportedOperation, OperationDone}
import com.mz.example.actors.factories.jdbc.DataSourceActorFactory
import com.mz.example.actors.jdbc.DataSourceActorMessages.{ConnectionResult, GetConnection}
import akka.pattern._

import scala.util.{Failure, Success}

/**
 * Created by zemi on 1. 10. 2015.
 */
class JDBCConnectionActor(dataSourceRef: ActorRef) extends Actor with ActorLogging with DataSourceActorFactory {

  import JDBCConnectionActorMessages._
  import context.dispatcher

  context.watch(dataSourceRef)

  private implicit val timeout: Timeout = 1.seconds

  var connection: Option[Connection] = None

  var retryConnectionAttempt = 7

  @throws[RuntimeException](classOf[RuntimeException])
  override def preStart(): Unit = {
    log.info("init of Actor")
    (dataSourceRef ? GetConnection).mapTo[ConnectionResult] onComplete {
      case Success(con) => {
        log.debug("Connection returned!")
        connection = Some(con.con)
        context.become(connectionReady)
      }
      case Failure(f) => {
        log.error(f, f.getMessage)
        throw f
      }
    }
  }

  override def receive: Receive = {
    case opr: Insert => connectionNotReady(opr, sender)
    case opr: Update => connectionNotReady(opr, sender)
    case opr: Delete => connectionNotReady(opr, sender)
    case Select(query, mapper) => connectionNotReady(Select(query, mapper), sender)
    case RetryOperation(opr, orgSender) => connectionNotReady(opr, orgSender)
    case UnsupportedOperation => log.debug(s"Receive => sender sent UnsupportedOperation $sender")
    case obj: Any => {
      log.warning(s"receive => Unsupported operation object ${obj.getClass}")
      sender() ! UnsupportedOperation
    }
  }

  private def connectionNotReady(opr: Any, orgSender: ActorRef): Unit = {
    log.info(s"Connection is not jet ready! Attempt for getting of connection $retryConnectionAttempt")
    retryConnectionAttempt -= 1
    context.system.scheduler.scheduleOnce(
      150.millisecond, self, RetryOperation(opr, orgSender))
  }

  private def connectionReady: Receive = {
    case RetryOperation(message, senderOrg) => message match {
      case Insert(query) => senderOrg ! insert(query)
      case Update(query) => senderOrg ! update(query)
      case Delete(query) => senderOrg ! delete(query)
      case Select(query, mapper) => senderOrg ! select(query, mapper)
    }
    case Insert(query) => sender ! insert(query)
    case Update(query) => sender ! update(query)
    case Delete(query) => sender ! delete(query)
    case Select(query, mapper) => sender ! select(query, mapper)
    case Commit => commit
    case Rollback => rollback
    case UnsupportedOperation => log.debug(s"sender sent UnsupportedOperation $sender")
    case obj: Any => {
      log.warning(s"connectionReady => Unsupported operation object ${obj.getClass}")
      sender() ! UnsupportedOperation
    }
  }

  /**
   * Execute select
   * @param query
   * @return
   */
  private def select[E](query: String, mapper: ResultSet => E): SelectResult[E] = {
    connection.map(con => {
      log.info(s"Select query = $query")
      val prtStatement = con.prepareStatement(query)
      try {
        val result = SelectResult(mapper(prtStatement.executeQuery()))
        result
      } catch {
        case e: SQLException => {
          log.error(e.getMessage, e)
          throw e
        }
      } finally {
        prtStatement.close
      }
    }).get
  }

  /**
   * execute update
   * @param query
   * @return Future
   */
  private def delete(query: String): Boolean = {
    connection.map(con => {
      log.info(s"Delete query = $query")
      executeUpdate(query, con)
    }).get
  }

  /**
   * execute update
   * @param query
   * @return Future
   */
  private def update(query: String): Boolean = {
    connection.map(con => {
      log.info(s"Update query = $query")
      val prtStatement = con.prepareStatement(query)
      try {
        prtStatement.executeUpdate()
        true
      } catch {
        case e: SQLException => {
          log.error(e.getMessage, e)
          throw e
        }
      } finally {
        prtStatement.close
      }
    }).get
  }

  /**
   * Insert
   * @param query
   * @return
   */
  private def insert(query: String): GeneratedKeyRes = {
    connection.map(con => {
      log.info(s"Insert query = $query")
      val prtStatement = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)
      try {
        prtStatement.executeUpdate()
        val keys = prtStatement.getGeneratedKeys
        if (keys.next) {
          log.debug("inserted successful!")
          GeneratedKeyRes(keys.getLong(1))
        } else {
          GeneratedKeyRes(0)
        }
      } catch {
        case e: SQLException => {
          log.error(e, e.getMessage)
          throw e
        }
      } finally {
        prtStatement.close
      }
    }).get
  }

  /**
   * Execute Commit
   */
  private def commit: Unit = {
    log.debug("JDBCConnectionActor.Commit")
    try {
      connection.map(con => {
        con.commit()
        sender() ! Committed
      })
    } catch {
      case e: SQLException => {
        log.error(e, "JDBCConnectionActor.Commit")
        throw e
      }
    }
  }

  /**
   * execute rollback
   */
  private def rollback: Unit = {
    log.debug("JDBCConnectionActor.Rollback")
    try {
      connection.map(con => {
        con.rollback()
      })
    } catch {
      case e: SQLException => {
        log.error(e, "JDBCConnectionActor.Rollback")
        throw e
      }
    }
  }

  private def executeUpdate(query: String, con: Connection): Boolean = {
    val prtStatement = con.prepareStatement(query)
    try {
      prtStatement.executeUpdate()
      true
    } catch {
      case e: SQLException => {
        log.error(e.getMessage, e)
        throw e
      }
    } finally {
      prtStatement.close
    }
  }

  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    connection.map(con => {
      try {
        con.rollback()
      } catch {
        case e: SQLException => {
          log.error(e, e.getMessage)
        }
      } finally {
        if (!con.isClosed) {
          log.debug("JDBCConnectionActor.connection.close()")
          con.close
        }
      }
    })
  }
}

object JDBCConnectionActor {

  /**
   * Create Props for an actor of this type
   * @return a Props
   */
  def props(dataSourceRef: ActorRef): Props = Props(classOf[JDBCConnectionActor], dataSourceRef)
}
