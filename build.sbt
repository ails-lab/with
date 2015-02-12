name := """with"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
//  javaJdbc,
//  javaEbean,
//  cache,
//  javaWs
  "com.impetus.kundera.core" % "kundera-core" % "2.15",
 "com.impetus.kundera.client" % "kundera-mongo" % "2.15",
 "org.mongodb.morphia" % "morphia" % "0.110"
)
