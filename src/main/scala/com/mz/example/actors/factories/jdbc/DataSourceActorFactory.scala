package com.mz.example.actors.factories.jdbc

import akka.actor.{ActorContext, ActorSelection, ActorRef, ActorSystem}
import com.mz.example.actors.factories.supervisors.DataSourceSupervisorActorFactory

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Created by zemo on 05/10/15.
 */
trait DataSourceActorFactory extends DataSourceSupervisorActorFactory {

  val actorPath = "/user/dataSource"

  def selectActor(implicit context: ActorContext): Future[ActorRef] = {
    implicit val dispatcher = context.dispatcher
    context.actorSelection(actorPath).resolveOne(100 milliseconds)
  }
}
