package com.mz.example.actors.repositories.common.messages

import com.mz.example.domains.Address

/**
 * Created by zemo on 17/10/15.
 */
object AddressRepositoryActorMessages {

  case class UpdateAddress(address: Address)

  case class DeleteAddress(id: Long)

  case class InsertAddress(address: Address)

}
