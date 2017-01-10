name := """akka-scala-jdbc-demo"""

version := "1.0"

scalaVersion := "2.12.0"

val akkaVersion = "2.4.16"

libraryDependencies ++= Seq(
  // Akka
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  //"com.typesafe.akka" %% "akka-remote" % akkaVersion,
  // test framework dependencies
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.mockito" % "mockito-all" % "1.10.19",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  //DB dependencies
  "org.postgresql" % "postgresql" % "9.4-1203-jdbc42",
  "com.zaxxer" % "HikariCP" % "2.4.1"
)
