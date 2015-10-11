package com.mz.example.domains.sql.mappers

import java.sql.ResultSet

import com.mz.example.domains.User

/**
 * Created by zemo on 11/10/15.
 */
trait SqlDomainMapper[+E] {

  /**
   * Map ResultSet to Domain object
   * @param resultSet
   * @return Some[E] or None
   */
  def mapResultSet(resultSet: ResultSet): Option[E] = {
    if (resultSet.next()) {
      Some(mapResultSetUser(resultSet))
    } else None
  }

  /**
   * Map ResultSet to Domain object
   * @param resultSet
   * @return
   */
  def mapResultSetUser(resultSet: ResultSet): E

}
