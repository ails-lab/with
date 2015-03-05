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
 "commons-io" % "commons-io" % "2.3",
 "com.google.code.gson" % "gson" % "2.2.4"
)


resourceGenerators in Compile <+= resourceManaged in Compile map { dir =>
    val targetConfig    = dir / "META-INF" / "persistent.xml"
    val inputConfig     = XML.loadFile("conf/persistent.xml")
    val configResource = 
             if(System.getProperties().containsKey("config.file")) 
             {
                System.getProperty("config.file")
            }else{
                "application.conf"
            }
    val localConfig = ConfigFactory.parseFile(new File(configResource)).resolve()
    var nodes = ""
    var port  = "" 
    if(localConfig.hasPath("db.nodes"))
    {
        nodes = localConfig.getString("db.nodes")
    }
    if(localConfig.hasPath("db.port"))
    {
        port  = localConfig.getString("db.port")
    }
    val inputContents = 
        if(nodes != "" && port != "")
        {
            // function that replaces values in the prersistent.xml
            def replaceParams(dom: scala.xml.Node, nodes: String, port: String): scala.xml.Node = {
            object replaceParams extends RewriteRule {
                override def transform(n: scala.xml.Node): Seq[scala.xml.Node] = 
                    // this pattern match actually does the job
                    n match {
                        case Elem(prefix, "property", attribs, scope, _*) 
                              if(attribs.asAttrMap("name") == "kundera.nodes") =>
                                <property name="kundera.nodes" value={nodes} />
                        case Elem(prefix, "property", attribs, scope, _*) 
                              if(attribs.asAttrMap("name") == "kundera.port") =>
                                <property name="kundera.port" value={port} />
                        case other => 
                            other
                    }
            }   
            object transform extends RuleTransformer(replaceParams)
            transform(inputConfig)
            }
            replaceParams(inputConfig, nodes, port).toString()
        }else{
            inputConfig.toString()
        }
        IO.write(targetConfig, inputContents)
    Seq(targetConfig)
}       
        
