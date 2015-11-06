package com.mz.example.domains.sql.mappers

import java.sql.ResultSet

import com.mz.example.domains.User

/**
 * Created by zemo on 11/10/15.
 */
trait UserMapper extends SqlDomainMapper[User] {

  /*
  id  bigserial NOT NULL,
  last_name     VARCHAR(255),
  first_name    VARCHAR(255),
  address_id    bigint,
  */

  //case class User(id: Long, firstName: String, lastName: String, addressId: Option[Long], address: Option[Address])

  val TABLE_NAME = "users"

  val ID_COL = "id"

  val FIRST_NAME_COL = "first_name"

  val LAST_NAME_COL = "last_name"

  val ADDRESS_ID_COL = "address_id"

  /**
   * Map ResultSet to User
   * @param resultSet
   * @return User
   */
  def mapResultSetDomain(resultSet: ResultSet): User = {
    try {
      User(resultSet.getLong(ID_COL), resultSet.getString(FIRST_NAME_COL), resultSet.getString(LAST_NAME_COL),
        Option(resultSet.getLong(ADDRESS_ID_COL)), None)
    } catch {
      case e:Exception => {
        e.getMessage
        throw e
      }
    }
  }

}
