import sbt._
import Keys._
import IO._
import Seq._
import scala.xml._
import scala.xml.transform._
import com.typesafe.config._

name := """with"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
//  javaJdbc,
//  javaEbean,
//  cache,
  javaWs,
 "org.mongodb.morphia" % "morphia" % "0.110",
 "org.apache.jena" % "apache-jena-libs" % "2.10.1",
 "commons-io" % "commons-io" % "2.3",
 "com.google.code.gson" % "gson" % "2.2.4"
)

