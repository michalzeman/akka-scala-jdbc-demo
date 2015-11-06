package com.mz.example.actors.factories.supervisors

import akka.actor.{ActorSelection, ActorRefFactory}
import com.mz.example.actors.supervisors.DataSourceSupervisorActor

/**
 * Created by zemi on 5. 11. 2015.
 */
trait DataSourceSupervisorActorFactory {

  val supervisorActorPath = "/user/"+DataSourceSupervisorActor.actorName

  def selectDataSourceSupervisorActor(implicit context: ActorRefFactory): ActorSelection = {
    context.actorSelection(supervisorActorPath)
  }

}
