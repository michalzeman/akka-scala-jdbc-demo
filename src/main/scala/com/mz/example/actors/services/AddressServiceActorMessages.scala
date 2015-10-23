package com.mz.example.actors.services

import com.mz.example.domains.Address

/**
 * Created by zemi on 23. 10. 2015.
 */
object AddressServiceActorMessages {

  case class FindAddressById(id: Long)

  case class CreateAddress(address: Address)

  case class AddressCreated(id: Long)

  case class UpdateAddress(address: Address)

  trait AddressUpdateResult

  case class AddressUpdated() extends AddressUpdateResult

  case class AddressNotUpdated() extends AddressUpdateResult

  case class DeleteAddress(address: Address)

  trait AddressDeleteResult

  case class AddressDeleted() extends AddressDeleteResult

  case class AddressNotDeleted() extends AddressDeleteResult

}
