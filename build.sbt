name := """akka-scala-jdbc-demo"""

version := "1.0"

scalaVersion := "2.11.1"

val akkaVersion = "2.3.9"

libraryDependencies ++= Seq(
  // Akka
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  //"com.typesafe.akka" %% "akka-remote" % "2.3.5",
  // test framework dependencies
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.mockito" % "mockito-all" % "1.10.19",
  "org.scalatest" %% "scalatest" % "2.1.6" % "test",
  //DB dependencies
  "org.postgresql" % "postgresql" % "9.4-1203-jdbc42",
  "com.zaxxer" % "HikariCP" % "2.4.1"
)
