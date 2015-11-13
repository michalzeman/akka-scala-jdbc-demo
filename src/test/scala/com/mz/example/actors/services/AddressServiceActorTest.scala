package com.mz.example.actors.services

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestActorRef, TestProbe, ImplicitSender, TestKit}
import com.mz.example.actors.jdbc.JDBCConnectionActor._
import com.mz.example.actors.repositories.{AddressRepositoryActor, UserRepositoryActor}
import com.mz.example.actors.services.AddressServiceActor.{FoundAddresses, FindAddress, AddressCreated, CreateAddress}
import com.mz.example.domains.Address
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, BeforeAndAfterAll, FunSuiteLike}
import org.scalautils.ConversionCheckedTripleEquals

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

  test("2. Find address by all attributes") {
    val jdbcConA = TestProbe()
    val userRepository = Props(new UserRepositoryActor(jdbcConA.ref))
    val addressRepository = Props(new AddressRepositoryActor(jdbcConA.ref))
    val addressService = system.actorOf(AddressServiceActor.props(userRepository, addressRepository))
    addressService ! FindAddress(Address(0, "StreetFind", "zipFind", "houseNumFind", "CityFind"))
    jdbcConA.expectMsgType[Select[Address]]
    jdbcConA.reply(List[Address](Address(3, "StreetFind", "zipFind", "houseNumFind", "CityFind")))
    expectMsgType[FoundAddresses]
  }

  test("3. Find or create address") {
    val jdbcConA = TestProbe()
    val userRepository = Props(new UserRepositoryActor(jdbcConA.ref))
    val addressRepository = Props(new AddressRepositoryActor(jdbcConA.ref))
    val addressService = system.actorOf(AddressServiceActor.props(userRepository, addressRepository))
    addressService ! FindAddress(Address(0, "Street_Find", "zip_Find", "houseNum_Find", "City_Find"))
    jdbcConA.expectMsgType[Select[Address]]
  }

  test("4. delete address") {

  }

}
