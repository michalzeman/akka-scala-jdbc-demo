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
      Some(mapResultSetDomain(resultSet))
    } else None
  }

  /**
   * Map ResultSet to list
   * @param resultSet
   * @return List[E]
   */
  def mapResultSetList(resultSet: ResultSet): Seq[E] = {
//    def mapList(restSet: ResultSet, tail:Stream[E]): Stream[E] = {
//      if (restSet.next()) {
//        mapResultSetDomain(restSet)#::mapList(restSet,tail)
//      } else tail
//    }
//    mapList(resultSet, Stream.empty)
    val list = scala.collection.mutable.MutableList[E]()
    while(resultSet.next()) {
      list += mapResultSetDomain(resultSet)
    }
    list
  }

  /**
   * Map ResultSet to Domain object
   * @param resultSet
   * @return
   */
  def mapResultSetDomain(resultSet: ResultSet): E

}
