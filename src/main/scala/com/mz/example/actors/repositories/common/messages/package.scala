package com.mz.example.actors.repositories.common

/**
 * Created by zemo on 12/10/15.
 */
package object messages {

  case class Inserted(id: Long)

  case class SelectById(id: Long)

  case object SelectAll

}
