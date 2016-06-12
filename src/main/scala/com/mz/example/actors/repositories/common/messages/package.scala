package com.mz.example.actors.repositories.common

import com.mz.example.domains.EntityId


/**
 * Created by zemo on 12/10/15.
 */
package object messages {

  case class Inserted(id: Long)

  case class SelectById(id: Long)

  case object SelectAll

  case class Update[E <: EntityId](entity: E)

  case class Delete(id: Long)

  case class Insert[E <: EntityId](entity: E)

}
