package com.mz.example.actors.jdbc

import java.sql.ResultSet

import akka.actor.ActorRef

/**
 * Created by zemo on 04/10/15.
 */
object JDBCConnectionActorMessages {

  case object Init

  /**
   *
   */
  case class InitConnection(actor: ActorRef)

  /**
   * message for commit transaction
   */
  case object Commit

  /**
   * confirmation message for successful commit
   */
  case object Committed

  /**
   * message for rollback transaction
   */
  case object Rollback

  /**
   * confirmation message for successful rollback
   */
  case object RollbackSuccess

  /**
   * Insert statement
   * @param query
   */
  case class Insert(query: String)

  /**
   * Update statement
   * @param query
   */
  case class Update(query: String)

  /**
   * Delete statement
   * @param query
   */
  case class Delete(query: String)

  /**
   * Select statement
   * @param query
   */
  case class Select[+E](query: String, mapper: ResultSet => E)

  /**
   * Result of select
   * @param result - E
   */
  case class SelectResult[+E](result: E)

  /**
   * Generated key as a result after Insert
   * @param id
   */
  case class GeneratedKeyRes(id: Long)

}
