package com.mz.example.actors.factories.jdbc

import akka.actor._
import com.mz.example.actors.factories.supervisors.DataSourceSupervisorActorFactory
import com.mz.example.actors.jdbc.DataSourceActor

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Created by zemo on 05/10/15.
 */
trait DataSourceActorFactory extends DataSourceSupervisorActorFactory {

  val actorPath = supervisorActorPath+"/"+ DataSourceActor.actorName

  def selectDataSourceActor(implicit context: ActorRefFactory): ActorSelection = {
    context.actorSelection(actorPath)
  }
}
