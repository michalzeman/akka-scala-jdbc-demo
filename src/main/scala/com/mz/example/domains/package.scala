package com.mz.example

/**
 * Created by zemi on 8. 10. 2015.
 */
package object domains {

  case class User(id: Long, firstName: String, lastName: String, addressId: Option[Long], address: Option[Address])

  case class Address(id: Long, street: String, zip: String, houseNumber: String, city: String)

}
