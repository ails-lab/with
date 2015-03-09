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
 "org.apache.jena" % "apache-jena-libs" % "2.10.1"
)
