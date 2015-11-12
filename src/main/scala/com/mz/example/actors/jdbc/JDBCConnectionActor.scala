package com.mz.example.actors.jdbc

import java.sql.{ResultSet, Statement, SQLException, Connection}
import akka.actor._
import akka.util.Timeout
import com.typesafe.config.Config
import scala.concurrent.{Promise, Future}
import scala.concurrent.duration._
import com.mz.example.actors.common.messages.Messages.{RetryOperation, UnsupportedOperation, OperationDone}
import com.mz.example.actors.factories.jdbc.DataSourceActorFactory
import com.mz.example.actors.jdbc.DataSourceActorMessages.{ConnectionResult, GetConnection}
import akka.pattern._

/**
 * Created by zemi on 1. 10. 2015.
 */
class JDBCConnectionActor extends Actor with ActorLogging with DataSourceActorFactory {

  import JDBCConnectionActorMessages._
  import DataSourceActor.SCHEMA
  import scala.concurrent.ExecutionContext.Implicits.global
  //import context.dispatcher

  private implicit val timeout: Timeout = 1.seconds

  private val sysConfig: Config = context.system.settings.config
  private val defaultSchema = sysConfig.getString(SCHEMA)

  var connection: Option[Connection] = None
  val conInterceptorActor = context.actorOf(ConnectionInterceptorActor.props)

  context.become(connectionClosed)

  @throws[RuntimeException](classOf[RuntimeException])
  override def preStart(): Unit = {
//    log.info("init of Actor")
//    askForConnection
  }

  override def receive: Receive = {
    case UnsupportedOperation => log.debug(s"Receive => sender sent UnsupportedOperation $sender")
    case obj: Any => {
      log.warning(s"receive => Unsupported operation object ${obj.getClass}")
      sender() ! UnsupportedOperation
    }
  }

  private def connectionNotReady: Receive = {
    case obj: Any => {
      log.info(s"Connection is not jet ready!")
      retryOperation(obj, sender)
    }
    case UnsupportedOperation => log.debug(s"Receive => sender sent UnsupportedOperation $sender")
  }

  private def connectionClosed: Receive = {
    case RetryOperation(opr, orgSender) => retryOperation(opr, orgSender)
    case obj: Any => {
      log.info(s"Connection is closed! Going to ask new connection!")
      retryOperation(obj, sender)
      askForConnection
    }
  }

  private def waitingForConnection: Receive = {
    case ConnectionResult(con) => {
      log.debug("Connection returned!")
      conInterceptorActor ! ActorStop
      con.setSchema(defaultSchema)
      connection = Some(con)
      context.become(connectionReady)
    }
    case opr: Insert => retryOperation(opr, sender)
    case opr: Update => retryOperation(opr, sender)
    case opr: Delete => retryOperation(opr, sender)
    case Select(query, mapper) => retryOperation(Select(query, mapper), sender)
    case RetryOperation(opr, orgSender) => retryOperation(opr, orgSender)
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
   * in case that connection is not ready it is scheduled sneding nex same message
   * @param obj - message
   * @param orgSender - sender of original message
   */
  def retryOperation(obj: Any, orgSender: ActorRef): Unit = {
    context.system.scheduler.scheduleOnce(
      9.millisecond, self, RetryOperation(obj, orgSender))
  }

  /**
   * Ask for new connection from DataSourceActor
   */
  private def askForConnection: Unit = {
    context.become(waitingForConnection)
    selectDataSourceActor ! GetConnection
    conInterceptorActor ! GetConnection
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
   * execute delete
   * @param query
   * @return Future
   */
  private def delete(query: String): Future[Boolean] = {
    log.info(s"Delete query = $query")
    executeUpdate(query)
  }

  /**
   * execute update
   * @param query
   * @return Future
   */
  private def update(query: String): Future[Boolean] = {
    log.info(s"Update query = $query")
    executeUpdate(query)
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
          sender ! akka.actor.Status.Failure(e)
          throw e
        }
      } finally {
        context.become(connectionClosed)
        if (!con.isClosed) {
          con.close()
        }
        connection = None
      }
    })
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
          sender ! akka.actor.Status.Failure(e)
          throw e
        }
      } finally {
        context.become(connectionClosed)
        if (!con.isClosed) {
          con.close()
        }
        connection = None
      }
    })
  }

  private def executeUpdate(query: String): Future[Boolean] = {
    val p = Promise[Boolean]
    Future {
      connection.map(con => {
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
  def props: Props = Props[JDBCConnectionActor]
}
