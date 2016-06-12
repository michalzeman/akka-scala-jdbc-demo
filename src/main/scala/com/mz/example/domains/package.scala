package com.mz.example

/**
 * Created by zemi on 8. 10. 2015.
 */
package object domains {

  trait EntityId {
    val id: Long
  }

  case class User(id: Long, firstName: String, lastName: String, addressId: Option[Long], address: Option[Address]) extends EntityId

  case class Address(id: Long, street: String, zip: String, houseNumber: String, city: String) extends EntityId

}
