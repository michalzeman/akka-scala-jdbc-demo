package com.mz.example.actors.services

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.mz.example.actors.jdbc.JDBCConnectionActorMessages.{Commit, Rollback}
import com.mz.example.actors.jdbc.{JDBCConnectionActor, DataSourceActor}
import com.mz.example.actors.repositories.{AddressRepositoryActor, UserRepositoryActor}
import com.mz.example.actors.services.UserServiceActorMessages._
import com.mz.example.domains.User
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, BeforeAndAfterAll, FunSuiteLike}
import org.scalautils.ConversionCheckedTripleEquals
import akka.pattern.ask
import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by zemi on 22. 10. 2015.
 */
class UserServiceActorIntegrationTest extends TestKit(ActorSystem("test-jdbc-demo-UserServiceActorTest"))
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

    jdbcConActor ! Rollback
  }

  test("Update user") {
    val userRepositoryProps = UserRepositoryActor.props(jdbcConActor)

    val addressRepositoryProps = AddressRepositoryActor.props(jdbcConActor);

    val userService = system.actorOf(UserServiceActor.props(userRepositoryProps, addressRepositoryProps))

    val result = Await.result(userService ? CreateUser("FirstNameTest", "LastNameTest"), 1.seconds).asInstanceOf[UserCreated]

    Await.result(userService ? UpdateUser(User(result.id, "FirstNameUpdated", "LastNameUpdated", None, None)), 1.seconds).isInstanceOf[UserUpdated] shouldBe true

    val resultAfterUpdate = Await.result((userService ? FindUserById(result.id)), 1.seconds).asInstanceOf[FoundUsers]

    resultAfterUpdate.users.size should not be 0

    resultAfterUpdate.users.head.firstName shouldBe "FirstNameUpdated"

    jdbcConActor ! Rollback
  }

  test("Delete user") {
    val userRepositoryProps = UserRepositoryActor.props(jdbcConActor)

    val addressRepositoryProps = AddressRepositoryActor.props(jdbcConActor);

    val userService = system.actorOf(UserServiceActor.props(userRepositoryProps, addressRepositoryProps))

    val result = Await.result(userService ? CreateUser("FirstNameTest", "LastNameTest"), 1.seconds).asInstanceOf[UserCreated]

    Await.result((userService ? DeleteUser(User(result.id, "FirstNameTest", "LastNameTest", None, None))), 1.seconds)

    jdbcConActor ! Commit

    val resultAfterDelete = Await.result((userService ? FindUserById(result.id)), 1.seconds).asInstanceOf[FoundUsers]

    resultAfterDelete.users.size shouldBe 0
  }

  override protected def afterAll(): Unit = {
    jdbcConActor ! Rollback
    system.shutdown()
  }
}