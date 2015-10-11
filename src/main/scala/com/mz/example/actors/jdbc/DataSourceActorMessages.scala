package com.mz.example.actors.jdbc

import java.sql.Connection

/**
 * Created by zemo on 05/10/15.
 */
object DataSourceActorMessages {

  /**
   * Requesting of connection
   */
  case object GetConnection

  /**
   * Message with the connection
   * @param con @java.sql.Connection
   */
  case class ConnectionResult(con: Connection)

}
