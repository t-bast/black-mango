name := "black-mango"

version := "0.1"

scalaVersion := "2.12.8"

lazy val akkaVersion = "2.5.21"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "com.github.stefano81" % "jpbc" % "v2.0.0-m",
  "com.google.crypto.tink" % "tink" % "1.2.2"
)

// We need jitpack to import jPBC (only packaged as a jar file).
resolvers += "jitpack" at "https://jitpack.io"