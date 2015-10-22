package com.mz.example.actors.repositories

import com.mz.example.actors.jdbc.{JDBCConnectionActor, DataSourceActor}
import com.mz.example.actors.jdbc.JDBCConnectionActorMessages.{Rollback, Commit}
import com.mz.example.actors.repositories.common.AbstractRepositoryActorTest
import akka.pattern.ask
import com.mz.example.actors.repositories.common.messages.AddressRepositoryActorMessages.InsertAddress
import com.mz.example.actors.repositories.common.messages.{SelectById, Inserted}
import com.mz.example.actors.repositories.common.messages.UserRepositoryActorMessages.{DeleteUser, UpdateUser, InsertUser}
import com.mz.example.domains.{Address, User}
import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by zemo on 12/10/15.
 */
class UserRepositoryActorTest extends AbstractRepositoryActorTest {

  implicit val timeOut: akka.util.Timeout = 10000.millisecond

  test("CRUD operations") {
    val userRepository = system.actorOf(UserRepositoryActor.props(jdbcConActor))
    val addressRepository = system.actorOf(AddressRepositoryActor.props(jdbcConActor))
    val resultAddress = Await.result(addressRepository ? InsertAddress(Address(0, "test", "82109", "9A", "testCity")), 9.seconds).asInstanceOf[Inserted]

    val result = Await.result(userRepository ? InsertUser(User(0, "test", "Test 2", Option(resultAddress.id), None)), 9.seconds).asInstanceOf[Inserted]
    println(s"Id of inserted is ${result.id}")
    result.id should not be 0

    val resultSelect = Await.result(userRepository ? SelectById(result.id), 9.seconds).asInstanceOf[Some[User]]
    resultSelect.get.firstName shouldBe("test")

    Await.result(userRepository ? UpdateUser(User(result.id, "UpdateTest", "UpdateTest 2", None, None)), 9.seconds).asInstanceOf[Boolean] shouldBe true
    val resultSelectUpdated = Await.result(userRepository ? SelectById(result.id), 9.seconds).asInstanceOf[Some[User]]
    resultSelectUpdated.get.firstName shouldBe("UpdateTest")
    resultSelectUpdated.get.lastName shouldBe("UpdateTest 2")

    Await.result(userRepository ? DeleteUser(result.id), 9.seconds).asInstanceOf[Boolean] shouldBe true

    val resultSelectDeleted = Await.result(userRepository ? SelectById(result.id), 9.seconds)
    resultSelectDeleted should not be isInstanceOf[Some[User]]
  }

  override protected def afterAll(): Unit = {
    jdbcConActor ! Rollback
    system.shutdown()
  }

}
