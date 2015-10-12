package com.mz.example.actors.repositories.common.messages

import com.mz.example.domains.User

/**
 * Created by zemi on 8. 10. 2015.
 */
object UserRepositoryActorMessages {

  case class UpdateUser(user: User)

  case class DeleteUser(id: Long)

  case class InsertUser(user: User)

}
