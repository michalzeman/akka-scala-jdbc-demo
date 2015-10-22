package com.mz.example.actors.services

import com.mz.example.domains.{Address, User}

/**
 * Created by zemo on 18/10/15.
 */
object UserServiceActorMessages {

  case class CreateUser(firstName: String, lastName: String)

  case class UserCreated(id: Long)

  case class FindUserById(id: Long)

  case class FoundUsers(users: Seq[User])

  case class DeleteUser(user: User)

  case class UserDeleted()

  case class UserNotDeleted()

  case class UpdateUser(user: User)

  case class UserUpdated()

  case class UserNotUpdated()

  case class AddAddressToUser(user: User, address: Address)

  case class AddressAddedTodUser()
}
