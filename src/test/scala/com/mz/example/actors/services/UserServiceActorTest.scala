package com.mz.example.actors.services

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.mz.example.actors.jdbc.JDBCConnectionActorMessages.Rollback
import com.mz.example.actors.jdbc.{JDBCConnectionActor, DataSourceActor}
import com.mz.example.actors.repositories.{AddressRepositoryActor, UserRepositoryActor}
import com.mz.example.actors.services.UserServiceActorMessages.{UserCreated, CreateUser}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, BeforeAndAfterAll, FunSuiteLike}
import org.scalautils.ConversionCheckedTripleEquals
import akka.pattern.ask
import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by zemi on 22. 10. 2015.
 */
class UserServiceActorTest extends TestKit(ActorSystem("test-jdbc-demo-UserServiceActorTest"))
with FunSuiteLike
with BeforeAndAfterAll
with Matchers
with ConversionCheckedTripleEquals
with ImplicitSender
with MockitoSugar {

  implicit val timeOut: akka.util.Timeout = 10000.millisecond

  val dataSourceActor = system.actorOf(DataSourceActor.props, "dataSource")

  val jdbcConActor = system.actorOf(JDBCConnectionActor.props(dataSourceActor))

  test("Create user") {

    val userRepositoryProps = UserRepositoryActor.props(jdbcConActor)

    val addressRepositoryProps = AddressRepositoryActor.props(jdbcConActor);

    val userService = system.actorOf(UserServiceActor.props(userRepositoryProps, addressRepositoryProps))

    val result = Await.result(userService ? CreateUser("FirstNameTest", "LastNameTest"), 1.seconds).asInstanceOf[UserCreated]

    result.id should not be(0)
  }

  override protected def afterAll(): Unit = {
    jdbcConActor ! Rollback
    system.shutdown()
  }
}
