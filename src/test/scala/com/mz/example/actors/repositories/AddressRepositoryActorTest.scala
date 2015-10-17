package com.mz.example.actors.repositories

import com.mz.example.actors.jdbc.JDBCConnectionActorMessages.Rollback
import com.mz.example.actors.repositories.common.AbstractRepositoryActorTest
import com.mz.example.actors.repositories.common.messages.AddressRepositoryActorMessages.{DeleteAddress, UpdateAddress, InsertAddress}
import com.mz.example.actors.repositories.common.messages.{SelectById, Inserted}
import com.mz.example.actors.repositories.common.messages.UserRepositoryActorMessages.{DeleteUser, UpdateUser, InsertUser}
import com.mz.example.domains.{Address, User}
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.pattern.ask

/**
 * Created by zemo on 17/10/15.
 */
class AddressRepositoryActorTest extends AbstractRepositoryActorTest {

  implicit val timeOut: akka.util.Timeout = 10000.millisecond

  test("CRUD operations") {
    val addressRepository = system.actorOf(AddressRepositoryActor.props(jdbcConActor))
    //Address(id: Long, street: String, zip: String, houseNumber: String, city: String)
    val result = Await.result(addressRepository ? InsertAddress(Address(0, "test", "82109", "9A", "testCity")), 9.seconds).asInstanceOf[Inserted]
    println(s"Id of inserted is ${result.id}")
    result.id should not be 0

    val resultSelect = Await.result(addressRepository ? SelectById(result.id), 9.seconds).asInstanceOf[Some[Address]]
    resultSelect.get.street shouldBe("test")

    Await.result(addressRepository ? UpdateAddress(Address(result.id, "test 2", "83109", "10A", "testCityBA")), 9.seconds).asInstanceOf[Boolean] shouldBe true
    val resultSelectUpdated = Await.result(addressRepository ? SelectById(result.id), 9.seconds).asInstanceOf[Some[Address]]
    resultSelectUpdated.get.street shouldBe("test 2")
    resultSelectUpdated.get.zip shouldBe("83109")

    Await.result(addressRepository ? DeleteAddress(result.id), 9.seconds).asInstanceOf[Boolean] shouldBe true

    val resultSelectDeleted = Await.result(addressRepository ? SelectById(result.id), 9.seconds)
    resultSelectDeleted should not be isInstanceOf[Some[Address]]
  }

  override protected def afterAll(): Unit = {
    jdbcConActor ! Rollback
    system.shutdown()
  }

}
