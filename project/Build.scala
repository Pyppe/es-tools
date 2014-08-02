import sbt._
import Keys._
import com.github.retronym.SbtOneJar

object ESToolsBuild extends Build {

  lazy val buildSettings = Seq(
    organization := "fi.pyppe.estools",
    version      := "0.1-SNAPSHOT",
    scalaVersion := "2.11.1",
    exportJars   := true
    //offline := true
  )

  lazy val dependencies = Seq(
    "net.databinder.dispatch"    %% "dispatch-core"         % "0.11.1",
    "io.argonaut"                %% "argonaut"              % "6.0.4",
    "org.rogach"                 %% "scallop"               % "0.9.5",

    // Logging
    "com.typesafe.scala-logging" %% "scala-logging"         % "3.0.0",
    "ch.qos.logback"             %  "logback-classic"       % "1.1.2",

    "org.specs2"              %% "specs2"                % "2.3.12" % "test"
  )

  lazy val root = Project(
    id = "akka-ircbot",
    base = file("."),
    settings = buildSettings ++
      Seq(libraryDependencies ++= dependencies) ++ SbtOneJar.oneJarSettings
  )

}

