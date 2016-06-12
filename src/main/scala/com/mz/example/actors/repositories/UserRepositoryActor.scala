package com.mz.example.actors.repositories

import akka.actor.{ActorRef, Props}
import com.mz.example.actors.repositories.common.AbstractRepositoryActor
import com.mz.example.domains.User
import com.mz.example.domains.sql.mappers.UserMapper

/**
  * Created by zemi on 8. 10. 2015.
  */
class UserRepositoryActor(jdbcActor: ActorRef)
  extends AbstractRepositoryActor[User](jdbcActor) with UserMapper {

}


object UserRepositoryActor {

  /**
    * Create Props for an actor of this type
    *
    * @return a Props
    */
  def props(jdbcConRef: ActorRef): Props = Props(classOf[UserRepositoryActor], jdbcConRef)
}