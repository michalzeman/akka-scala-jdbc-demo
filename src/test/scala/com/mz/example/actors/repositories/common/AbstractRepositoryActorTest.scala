package com.mz.example.actors.repositories.common

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.mz.example.actors.jdbc.{JDBCConnectionActor, DataSourceActor}
import com.mz.example.actors.supervisors.{CreatedActorMsg, DataSourceSupervisorActor}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, BeforeAndAfterAll, FunSuiteLike}
import org.scalautils.ConversionCheckedTripleEquals

/**
 * Created by zemo on 12/10/15.
 */
class AbstractRepositoryActorTest extends TestKit(ActorSystem("test-jdbc-demo-AbstractRepositoryActorTest"))
with FunSuiteLike
with BeforeAndAfterAll
with Matchers
with ConversionCheckedTripleEquals
with ImplicitSender
with MockitoSugar {

  val dataSourceSupervisor = system.actorOf(DataSourceSupervisorActor.props, DataSourceSupervisorActor.actorName)

  val jdbcConActor = system.actorOf(JDBCConnectionActor.props)


  override protected def beforeAll(): Unit = {
    super.beforeAll()

  }

  protected def executeScripts: Unit = {

  }

}
