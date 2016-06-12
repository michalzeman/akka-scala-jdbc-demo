package com.mz.example.actors.repositories

import akka.actor.{ActorRef, Props}
import com.mz.example.actors.repositories.common.AbstractRepositoryActor
import com.mz.example.domains.Address
import com.mz.example.domains.sql.mappers.AddressMapper


/**
 * Created by zemo on 17/10/15.
 */
class AddressRepositoryActor(jdbcActor: ActorRef)
  extends AbstractRepositoryActor[Address](jdbcActor) with AddressMapper {

}

object AddressRepositoryActor {

  /**
   * Create Props for an actor of this type
   * @return a Props
   */
  def props(jdbcConRef: ActorRef): Props = Props(classOf[AddressRepositoryActor], jdbcConRef)
}
