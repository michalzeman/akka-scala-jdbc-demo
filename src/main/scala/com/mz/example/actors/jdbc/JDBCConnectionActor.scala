package com.mz.example.actors.jdbc

import java.sql.{Statement, SQLException, Connection}
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

  private implicit val timeout: Timeout = 5.seconds

  var connection: Option[Connection] = None

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
        throw new RuntimeException(f)
      }
    }
  }

  override def receive: Receive = {
    case opr:Insert => connectionNotReady(opr)
    case opr:Update => connectionNotReady(opr)
    case opr:Delete => connectionNotReady(opr)
    case opr:Select => connectionNotReady(opr)
    case _ => {
      log.warning("Unsupported operation")
      sender() ! UnsupportedOperation
    }
  }

  private def connectionNotReady(opr: Any): Unit = {
    log.info("Connection is not jet ready!")
    context.system.scheduler.scheduleOnce(
      150.millisecond, self, RetryOperation(opr, sender()))
  }

  private def connectionReady: Receive = {
    case RetryOperation(message, senderOrg) => message match {
      case Insert(query) => insert(query) pipeTo senderOrg
      case Update(query) => update(query) pipeTo senderOrg
      case Delete(query) => delete(query) pipeTo senderOrg
      case Select(query) => select(query) pipeTo senderOrg
    }
    case Insert(query) => insert(query) pipeTo sender
    case Update(query) => update(query) pipeTo sender
    case Delete(query) => delete(query) pipeTo sender
    case Select(query) => select(query) pipeTo sender
    case Commit => commit
    case Rollback => rollback
    case _ => {
      log.warning("Unsupported operation")
      sender() ! UnsupportedOperation
    }
  }

  /**
   * Execute select
   * @param query
   * @return
   */
  private def select(query: String): Future[SelectResult] = {
    val p = Promise[SelectResult]
    Future {
      connection.map(con => {
        log.info("Update")
        val prtStatement = con.prepareStatement(query)
        try {
          val result = SelectResult(prtStatement.executeQuery())
          p.success(result)
        } catch {
          case e:SQLException =>  {
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
        log.info("Delete")
        executeUpdate(query, p, con)
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
        log.info("Update")
        val prtStatement = con.prepareStatement(query)
        try {
          prtStatement.executeUpdate()
          p.success(true)
        } catch {
          case e:SQLException =>  {
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
        log.info("Insert")
        val prtStatement = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)
        try {
          prtStatement.executeUpdate()
          val keys = prtStatement.getGeneratedKeys
          if (keys.next) {
            p.success(GeneratedKeyRes(keys.getLong(1)))
          } else {
            p.success(GeneratedKeyRes(0))
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

  private def executeUpdate(query: String, p: Promise[Boolean], con: Connection): p.type = {
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

object JDBCConnectionActor {

  /**
   * Create Props for an actor of this type
   * @return a Props
   */
  def props(dataSourceRef: ActorRef): Props = Props(classOf[JDBCConnectionActor], dataSourceRef)
}
