package com.mz.example.actors.services

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.mz.example.actors.jdbc.{JDBCConnectionActor, DataSourceActor}
import com.mz.example.actors.repositories.{AddressRepositoryActor, UserRepositoryActor}
import com.mz.example.actors.services.UserServiceActor._
import com.mz.example.actors.supervisors.{CreateActorMsg, DataSourceSupervisorActor}
import com.mz.example.domains.{Address, User}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, BeforeAndAfterAll, FunSuiteLike}
import org.scalautils.ConversionCheckedTripleEquals
import com.mz.example.actors.jdbc.JDBCConnectionActor._
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

  val dataSourceSupervisor = system.actorOf(DataSourceSupervisorActor.props, DataSourceSupervisorActor.actorName)

  val jdbcConActor = system.actorOf(JDBCConnectionActor.props)

  test("Create user") {

    val userRepositoryProps = UserRepositoryActor.props(jdbcConActor)

    val addressRepositoryProps = AddressRepositoryActor.props(jdbcConActor);

    val addressService = AddressServiceActor.props(userRepositoryProps, addressRepositoryProps)

    val userService = system.actorOf(UserServiceActor.props(userRepositoryProps, addressService))

    userService ! CreateUser(User(0,"FirstNameTest", "LastNameTest", None, None))

    val result = expectMsgType[UserCreated]

    result.id should not be(0)

    jdbcConActor ! Rollback
  }

  test("Update user") {
    val userRepositoryProps = UserRepositoryActor.props(jdbcConActor)

    val addressRepositoryProps = AddressRepositoryActor.props(jdbcConActor);

    val addressService = AddressServiceActor.props(userRepositoryProps, addressRepositoryProps)

    val userService = system.actorOf(UserServiceActor.props(userRepositoryProps, addressService))

    userService ! CreateUser(User(0,"FirstNameTest", "LastNameTest", None, None))

    val result = expectMsgType[UserCreated]

    userService ! UpdateUser(User(result.id, "FirstNameUpdated", "LastNameUpdated", None, None))
    expectMsgType[UserUpdated]

    userService ! FindUserById(result.id)
    val resultAfterUpdate = expectMsgType[FoundUsers]

    resultAfterUpdate.users.size should not be 0

    resultAfterUpdate.users.head.firstName shouldBe "FirstNameUpdated"

    jdbcConActor ! Rollback
  }

  test("Delete user") {
    val userRepositoryProps = UserRepositoryActor.props(jdbcConActor)

    val addressRepositoryProps = AddressRepositoryActor.props(jdbcConActor);

    val addressService = AddressServiceActor.props(userRepositoryProps, addressRepositoryProps)

    val userService = system.actorOf(UserServiceActor.props(userRepositoryProps, addressService))

    userService ! CreateUser(User(0,"FirstNameTest", "LastNameTest", None, None))
    val result = expectMsgType[UserCreated]

    userService ! DeleteUser(User(result.id, "FirstNameTest", "LastNameTest", None, None))
    expectMsgType[UserDeleted]

    jdbcConActor ! Commit
    expectMsg(Committed)

    userService ! FindUserById(result.id)
    val resultAfterDelete = expectMsgType[FoundUsers]

    resultAfterDelete.users.size shouldBe 0
  }

  test("Register user") {
    val userRepositoryProps = UserRepositoryActor.props(jdbcConActor)

    val addressRepositoryProps = AddressRepositoryActor.props(jdbcConActor);

    val addressService = AddressServiceActor.props(userRepositoryProps, addressRepositoryProps)

    val userService = system.actorOf(UserServiceActor.props(userRepositoryProps, addressService))

    userService ! RegistrateUser(User(0,"FirstNameTest", "LastNameTest", None, None),
      Address(0, "test", "82109", "9A", "testCity"))
    expectMsgType[UserRegistrated]

    jdbcConActor ! Rollback
  }

  override protected def afterAll(): Unit = {
    jdbcConActor ! Rollback
    system.shutdown()
  }
}
