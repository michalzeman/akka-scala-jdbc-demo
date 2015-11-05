package com.mz.example.actors.repositories

import com.mz.example.actors.jdbc.JDBCConnectionActorMessages.{SelectResult, Rollback, Commit}
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
    var addrIdRes:Inserted = null
    addressRepository ! InsertAddress(Address(0, "test", "82109", "9A", "testCity"))
    receiveWhile(2000 millis) {
      case addrId:Inserted => addrIdRes = addrId
    }

    val user = User(0, "test", "Test 2", Option(addrIdRes.id), None)
    userRepository ! InsertUser(user)
    var result: Inserted = null
    receiveWhile(500 millis) {
      case inserted: Inserted => result = inserted
    }

    val userSel = User(result.id, "test", "Test 2", Option(addrIdRes.id), None)
    userRepository ! SelectById(result.id)
    expectMsg(Some(userSel))

    val user2 = User(result.id, "UpdateTest", "UpdateTest 2", Option(addrIdRes.id), None)
    userRepository ! UpdateUser(user2)
    expectMsg(true)
    userRepository ! SelectById(result.id)
    expectMsg(Some(user2))

    userRepository ! DeleteUser(result.id)
    expectMsg(true)
    userRepository ! SelectById(result.id)
    expectMsgAnyOf(None)
  }

  override protected def afterAll(): Unit = {
    jdbcConActor ! Rollback
    system.shutdown()
  }

}
