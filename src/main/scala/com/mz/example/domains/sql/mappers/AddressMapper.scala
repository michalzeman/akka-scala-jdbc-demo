package com.mz.example.domains.sql.mappers

import java.sql.ResultSet

import com.mz.example.domains.{User, Address}

/**
 * Created by zemo on 17/10/15.
 */
trait AddressMapper extends SqlDomainMapper[Address] {

  /*
   CREATE TABLE addresses (
  id  bigserial NOT NULL,
  street        VARCHAR(255),
  house_number  INTEGER,
  zip           VARCHAR(5),
  city          VARCHAR(255),
   */

  val TABLE_NAME = "addresses"

  val ID_COL = "id"

  val STREET_COL = "street"

  val HOUSE_NUMBER_COL = "house_number"

  val ZIP_COL = "zip"

  val CITY_COL = "city"

  /**
   * Map ResultSet to User
   * @param resultSet
   * @return Address(id: Long, street: String, zip: String, houseNumber: String, city: String)
   */
  def mapResultSetDomain(resultSet: ResultSet): Address = {
    Address(resultSet.getLong(ID_COL), resultSet.getString(STREET_COL), resultSet.getString(ZIP_COL),
      resultSet.getString(HOUSE_NUMBER_COL), resultSet.getString(CITY_COL))
  }
}
