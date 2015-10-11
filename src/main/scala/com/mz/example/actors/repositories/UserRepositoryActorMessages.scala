package com.mz.example.actors.repositories

import com.mz.example.domains.User

/**
 * Created by zemi on 8. 10. 2015.
 */
object UserRepositoryActorMessages {

  case class SelectById(id: Long)

  case class UpdateUser(user: User)

}
