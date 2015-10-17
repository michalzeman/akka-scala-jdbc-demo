package com.mz.example.actors.factories.jdbc

import akka.actor.{ActorContext, ActorSelection, ActorRef, ActorSystem}

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Created by zemo on 05/10/15.
 */
trait DataSourceActorFactory {

  val actorPath = "/user/dataSource"

  def selectActor(implicit context: ActorContext): Future[ActorRef] = {
    implicit val dispatcher = context.dispatcher
    context.actorSelection(actorPath).resolveOne(100 milliseconds)
  }
}
