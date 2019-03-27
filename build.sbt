name := "akka-quickstart-scala"

version := "1.0"

scalaVersion := "2.12.6"

lazy val akkaVersion = "2.5.21"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "de.aktey.akka.visualmailbox" %% "collector" % "1.1.0",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)
