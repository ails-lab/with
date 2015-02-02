import sbt._
import Keys._
import IO._
import Seq._
import com.typesafe.config._

name := """with"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  javaWs
)


resourceGenerators in Compile <+= resourceManaged in Compile map { dir =>
    val targetConfig = dir / "META-INF" / "persistent.xml"
    val inputConfig = new File("conf/persistent.xml")
    val conf = ConfigFactory.parseFile(new File("conf/local.conf")).resolve()
    val answer = conf.getString("app.answer")
    val inputContents = IO.read(inputConfig, utf8)
    IO.write(targetConfig, inputContents)
    Seq(targetConfig)
}       
        
