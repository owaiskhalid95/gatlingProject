package simulations

import io.gatling.core.scenario.Simulation
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import javafx.util.Duration.minutes

import java.time.LocalDate
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Random



class TestFlow extends Simulation {


  val httpConfg= http.baseUrl("http://127.0.0.1:8900")
    .header("Accept",value = "application/json")
    .header("content-type",value = "application/json")

  val rnd= new Random()
  val now=LocalDate.now()
  val previousnow=LocalDate.now().minusDays(1)
  val alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
  def randStr(n:Int) = (1 to n).map(_ => alpha(Random.nextInt(alpha.length))).mkString


  val customFeeder=Iterator.continually(Map(
    "email" -> (randStr(5)+"@gmail.com"),
    "name"-> randStr(5),
    "surname"->randStr(5),
    "date" -> now,
    "date1" -> previousnow,
    "destination"->randStr(3),
    "origin"->randStr(3),
  ))


  def createUser={
    repeat(1)(
      feed(customFeeder)
        .exec(http("create user")
          .post("/user")
          .check(jsonPath("$.id").saveAs("userid"))
          .body(StringBody(
            "{"+
              "\n\t\"email\":\"${email}\"," +
              "\n\t\"name\":\"${name}\"," +
              "\n\t\"surname\":\"${surname}\"\n}")).asJson
          .check(status.is(201))
        ).pause(1))

  }

  def createBookingCurrent={
    repeat(1){
      feed(customFeeder)
        .exec(http("create Booking current date")
          .post("/booking")

          .body(StringBody(
            "{"+
              "\n\t\"date\":\"${date}\"," +
              "\n\t\"destination\":\"${destination}\"," +
              "\n\t\"origin\":\"${origin}\"," +
              "\n\t\"userId\":${userid}}")).asJson
          .check(status.is(201))
        ).pause(1)

    }
  }

  def createBookingPast={
    repeat(1){
      feed(customFeeder)
        .exec(http("create Booking past date")
          .post("/booking")
          .body(StringBody(
            "{"+
              "\n\t\"date\":\"${date1}\"," +
              "\n\t\"destination\":\"${destination}\"," +
              "\n\t\"origin\":\"${origin}\"," +
              "\n\t\"userId\":${userid}}")).asJson
          .check(status.is(201))
        ).pause(1)

    }
  }

  def getAllBooking={
    repeat(1){
      feed(customFeeder)
      exec(http("get all today booking")
        .get("/booking?user=${userid}&date=${date}")
        .header("content-type",value = "application/json")
        .check(status is 200))

    }
  }


  def getBooking={
    repeat(1){
      exec(http("choose one booking")
        .get("/booking/${userid}")
        .header("content-type",value = "application/json")
        .check(status is 200))

    }
  }

  val scn=scenario("Test Flow")
    .exec(createUser)
    .pause(2)
    .exec(createBookingPast)
    .pause(2)
    .exec(createBookingCurrent)
    .pause(2)
    .exec(getAllBooking)
    .pause(2)
    .exec(getBooking)
    .pause(2)

  //Unit test scenario
    //setUp(scn.inject(atOnceUsers(1))).protocols(httpConfg)


  //2.Load test scenario
     //setUp(scn.inject(atOnceUsers(10))).protocols(httpConfg).maxDuration(5 minutes)


  //3.Distributed load test scenario:
    setUp(
   scn.inject(
     atOnceUsers(1),
     rampUsers(3)during(60 second),
     nothingFor(60 second),
     rampUsers(1)during(1 minutes)

   ).protocols(httpConfg)
 )


}
