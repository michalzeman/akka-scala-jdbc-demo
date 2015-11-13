package com.mz.example.actors.services

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestActorRef, TestProbe, ImplicitSender, TestKit}
import com.mz.example.actors.jdbc.JDBCConnectionActor._
import com.mz.example.actors.repositories.{AddressRepositoryActor, UserRepositoryActor}
import com.mz.example.actors.services.AddressServiceActor._
import com.mz.example.domains.Address
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, BeforeAndAfterAll, FunSuiteLike}
import org.scalautils.ConversionCheckedTripleEquals

import scala.collection.mutable

/**
 * Created by zemi on 13. 11. 2015.
 */
class AddressServiceActorTest extends TestKit(ActorSystem("test-jdbc-demo-AddressServiceActorTest"))
with FunSuiteLike
with BeforeAndAfterAll
with Matchers
with ConversionCheckedTripleEquals
with ImplicitSender
with MockitoSugar {

  test("1. Create address") {
    val jdbcConA = TestProbe()
    val userRepository = Props(new UserRepositoryActor(jdbcConA.ref))
    val addressRepository = Props(new AddressRepositoryActor(jdbcConA.ref))
    val addressService = system.actorOf(AddressServiceActor.props(userRepository, addressRepository))
    //street: String, zip: String, houseNumber: String, city: String
    addressService ! CreateAddress(Address(0, "StreetCreate", "zipCreate", "houseNumCreate", "CityCreate"))
    jdbcConA.expectMsgType[Insert]
    jdbcConA.reply(GeneratedKeyRes(999))
    expectMsgAnyOf(AddressCreated(999))
  }

  test("2. delete address") {
    val jdbcConA = TestProbe()
    val userRepository = Props(new UserRepositoryActor(jdbcConA.ref))
    val addressRepository = Props(new AddressRepositoryActor(jdbcConA.ref))
    val addressService = system.actorOf(AddressServiceActor.props(userRepository, addressRepository))
    addressService ! DeleteAddress(Address(12, "Street_Find", "zip_Find", "houseNum_Find", "City_Find"))
    jdbcConA.expectMsgType[Delete]
    jdbcConA.reply(true)
    expectMsgType[AddressDeleted]
  }

  test("3. Find address by all attributes") {
    val jdbcConA = TestProbe()
    val userRepository = Props(new UserRepositoryActor(jdbcConA.ref))
    val addressRepository = Props(new AddressRepositoryActor(jdbcConA.ref))
    val addressService = system.actorOf(AddressServiceActor.props(userRepository, addressRepository))
    addressService ! FindAddress(Address(0, "StreetFind", "zipFind", "houseNumFind", "CityFind"))
    jdbcConA.expectMsgType[Select[Address]]
    jdbcConA.reply(List[Address](Address(3, "StreetFind", "zipFind", "houseNumFind", "CityFind")))
    expectMsgType[FoundAddresses]
  }

  test("4. Find or create address - create") {
    val jdbcConA = TestProbe()
    val userRepository = Props(new UserRepositoryActor(jdbcConA.ref))
    val addressRepository = Props(new AddressRepositoryActor(jdbcConA.ref))
    val addressService = system.actorOf(AddressServiceActor.props(userRepository, addressRepository))
    addressService ! FindOrCreateAddress(Address(0, "Street_Find", "zip_Find", "houseNum_Find", "City_Find"))
    jdbcConA.expectMsgType[Select[Address]]
    val addressResList:Seq[Address] = mutable.MutableList.empty
    jdbcConA.reply(SelectResult(addressResList))
    jdbcConA.expectMsg(Insert)
    jdbcConA.reply(GeneratedKeyRes(12))
    val addresses = mutable.MutableList(Address(12, "Street_Find", "zip_Find", "houseNum_Find", "City_Find"))
    expectMsgAllOf(FoundAddresses(addresses))
  }

  test("5. Find or create address - find") {
    val jdbcConA = TestProbe()
    val userRepository = Props(new UserRepositoryActor(jdbcConA.ref))
    val addressRepository = Props(new AddressRepositoryActor(jdbcConA.ref))
    val addressService = system.actorOf(AddressServiceActor.props(userRepository, addressRepository))
    addressService ! FindOrCreateAddress(Address(0, "Street_Find", "zip_Find", "houseNum_Find", "City_Find"))
    jdbcConA.expectMsgType[Select[Address]]
    val addressResList:Seq[Address] = mutable.MutableList(Address(12, "Street_Find", "zip_Find", "houseNum_Find", "City_Find"))
    jdbcConA.reply(SelectResult(addressResList))
    val addresses = mutable.MutableList(Address(12, "Street_Find", "zip_Find", "houseNum_Find", "City_Find"))
    expectMsgAllOf(FoundAddresses(addresses))
  }

}
