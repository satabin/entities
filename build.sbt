name := "gnieh-entities"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.3"

scalacOptions ++= Seq("-feature", "-deprecation")

libraryDependencies +=
  "com.typesafe.akka" %% "akka-actor" % "2.2.3"

libraryDependencies +=
  "org.scala-stm" %% "scala-stm" % "0.7"

