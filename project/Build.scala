import sbt._
import Keys._
import com.github.retronym.SbtOneJar

object ESToolsBuild extends Build {

  lazy val buildSettings = Seq(
    organization := "fi.pyppe.estools",
    version      := "0.1",
    scalaVersion := "2.11.1",
    exportJars   := true
    //offline := true
  )

  lazy val dependencies = Seq(
    "net.databinder.dispatch"    %% "dispatch-core"         % "0.11.1", // Http
    "org.rogach"                 %% "scallop"               % "0.9.5",  // Commandline
    "com.typesafe.play"          %% "play-json"             % "2.3.2",  // Json
    "com.typesafe.scala-logging" %% "scala-logging"         % "3.0.0",  // Logging
    "ch.qos.logback"             %  "logback-classic"       % "1.1.2",  // Logging

    // Testing:
    "org.specs2"                 %% "specs2"                % "2.3.12" % "test"
  )

  lazy val root = Project(
    id = "es-tools",
    base = file("."),
    settings = buildSettings ++
      Seq(libraryDependencies ++= dependencies) ++ SbtOneJar.oneJarSettings
  )

}

