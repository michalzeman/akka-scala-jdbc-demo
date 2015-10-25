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

  @throws[RuntimeException](classOf[RuntimeException])
  override def preStart(): Unit = {
    log.info("init of Actor")
    askForConnection
  }

  override def receive: Receive = {
    case opr: Insert => {
      connectionNotReady(opr, sender)
      askForConnection
    }
    case opr: Update => {
      connectionNotReady(opr, sender)
      askForConnection
    }
    case opr: Delete => {
      connectionNotReady(opr, sender)
      askForConnection
    }
    case Select(query, mapper) => {
      connectionNotReady(Select(query, mapper), sender)
      askForConnection
    }
    case RetryOperation(opr, orgSender) => {
      log.warning("JDBCConnectionActor is in the wrong state!")
      throw new RuntimeException("JDBCConnectionActor is in the wrong state!")
    }
    case UnsupportedOperation => log.debug(s"Receive => sender sent UnsupportedOperation $sender")
    case obj: Any => {
      log.warning(s"receive => Unsupported operation object ${obj.getClass}")
      sender() ! UnsupportedOperation
    }
  }

  private def connectionNotReady(opr: Any, orgSender: ActorRef): Unit = {
    log.info(s"Connection is not jet ready!")
    context.system.scheduler.scheduleOnce(
      150.millisecond, self, RetryOperation(opr, orgSender))
  }

  private def waitingForConnection: Receive = {
    case ConnectionResult(con) => {
      log.debug("Connection returned!")
      connection = Some(con)
      context.become(connectionReady)
    }
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

  private def connectionReady: Receive = {
    case RetryOperation(message, senderOrg) => message match {
      case Insert(query) => insert(query) pipeTo senderOrg
      case Update(query) => update(query) pipeTo senderOrg
      case Delete(query) => delete(query) pipeTo senderOrg
      case Select(query, mapper) => select(query, mapper) pipeTo senderOrg
    }
    case Insert(query) => insert(query) pipeTo sender
    case Update(query) => update(query) pipeTo sender
    case Delete(query) => delete(query) pipeTo sender
    case Select(query, mapper) =>  select(query, mapper) pipeTo sender
    case Commit => commit
    case Rollback => rollback
    case UnsupportedOperation => log.debug(s"sender sent UnsupportedOperation $sender")
    case obj: Any => {
      log.warning(s"connectionReady => Unsupported operation object ${obj.getClass}")
      sender() ! UnsupportedOperation
    }
  }

  /**
   * Ask for new connection from DataSourceActor
   */
  private def askForConnection: Unit = {
    context.become(waitingForConnection)
    dataSourceRef ! GetConnection
  }

  /**
   * Execute select
   * @param query
   * @return
   */
  private def select[E](query: String, mapper: ResultSet => E): Future[SelectResult[E]] = {
    val p = Promise[SelectResult[E]]
    Future {
      connection.map(con => {
        log.info(s"Select query = $query")
        val prtStatement = con.prepareStatement(query)
        try {
          val result = SelectResult(mapper(prtStatement.executeQuery()))
          p.success(result)
        } catch {
          case e: SQLException => {
            log.error(e.getMessage, e)
            p.failure(e)
          }
        } finally {
          prtStatement.close
        }
      })
    }
    p.future
  }

  /**
   * execute update
   * @param query
   * @return Future
   */
  private def delete(query: String): Future[Boolean] = {
    val p = Promise[Boolean]
    Future {
      connection.map(con => {
        log.info(s"Delete query = $query")
        executeUpdate(query, con, p)
      })
    }
    p.future
  }

  /**
   * execute update
   * @param query
   * @return Future
   */
  private def update(query: String): Future[Boolean] = {
    val p = Promise[Boolean]
    Future {
      connection.map(con => {
        log.info(s"Update query = $query")
        val prtStatement = con.prepareStatement(query)
        try {
          prtStatement.executeUpdate()
          p.success(true)
        } catch {
          case e: SQLException => {
            log.error(e.getMessage, e)
            p.failure(e)
          }
        } finally {
          prtStatement.close
        }
      })
    }
    p.future
  }

  /**
   * Insert
   * @param query
   * @return
   */
  private def insert(query: String): Future[GeneratedKeyRes] = {
    val p = Promise[GeneratedKeyRes]
    Future {
      connection.map(con => {
        log.info(s"Insert query = $query")
        val prtStatement = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)
        try {
          prtStatement.executeUpdate()
          val keys = prtStatement.getGeneratedKeys
          if (keys.next) {
            log.debug("inserted successful!")
            p.success(GeneratedKeyRes(keys.getLong(1)))
          } else {
            p.success(GeneratedKeyRes(0))
          }
        } catch {
          case e: SQLException => {
            log.error(e, e.getMessage)
            p.failure(e)
          }
        } finally {
          prtStatement.close
        }
      })
    }
    p.future
  }

  /**
   * Execute Commit
   */
  private def commit: Unit = {
    log.debug("JDBCConnectionActor.Commit")
    connection.map(con => {
      try {
        con.commit()
        sender() ! Committed
      } catch {
        case e: SQLException => {
          log.error(e, "JDBCConnectionActor.Commit")
          throw e
        }
      } finally {
        context.unbecome()
        if (!con.isClosed) {
          con.close()
        }
      }
    })
    connection = None
  }

  /**
   * execute rollback
   */
  private def rollback: Unit = {
    log.debug("JDBCConnectionActor.Rollback")
    connection.map(con => {
      try {
        con.rollback()
      } catch {
        case e: SQLException => {
          log.error(e, "JDBCConnectionActor.Rollback")
          throw e
        }
      } finally {
        context.unbecome()
        if (!con.isClosed) {
          con.close()
        }
      }
    })
    connection = None
  }

  private def executeUpdate(query: String, con: Connection, p: Promise[Boolean]): Unit = {
    val prtStatement = con.prepareStatement(query)
    try {
      prtStatement.executeUpdate()
      p.success(true)
    } catch {
      case e: SQLException => {
        log.error(e.getMessage, e)
        p.failure(e)
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

/**
 * Companion object
 */
object JDBCConnectionActor {

  /**
   * Create Props for an actor of this type
   * @return a Props
   */
  def props(dataSourceRef: ActorRef): Props = Props(classOf[JDBCConnectionActor], dataSourceRef)
}
