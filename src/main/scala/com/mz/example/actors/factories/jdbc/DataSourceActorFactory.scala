package com.mz.example.actors.factories.jdbc

import akka.actor.{ActorContext, ActorSelection, ActorRef, ActorSystem}

/**
 * Created by zemo on 05/10/15.
 */
trait DataSourceActorFactory {

  val actorPath = "/user/dataSource"

  def selectActor(implicit context: ActorContext): ActorSelection = {
    context.actorSelection(actorPath)
  }
}
