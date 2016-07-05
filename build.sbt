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
  filters,
  javaWs,
 "org.mongodb.morphia" % "morphia" % "1.0.1",
 "org.apache.jena" % "apache-jena-libs" % "2.10.1",
 "commons-io" % "commons-io" % "2.3",
 "com.google.code.gson" % "gson" % "2.2.4",
 "com.google.code.gson" % "gson" % "2.2.4",
 "commons-validator" % "commons-validator" % "1.4.0",
 "org.jsoup" % "jsoup" % "1.8.3",
 "com.optimaize.languagedetector" % "language-detector" % "0.4",
 "org.apache.httpcomponents" % "httpclient" % "4.5.2",
 "org.apache.httpcomponents" % "httpasyncclient" % "4.1.1",
  "org.apache.httpcomponents" % "httpmime" % "4.3.1",
  "org.elasticsearch" % "elasticsearch" % "1.5.1",
  "net.sourceforge.owlapi" % "owlapi-distribution" % "5.0.1",
// "com.yakaz.elasticsearch.plugins" % "elasticsearch-action-updatebyquery" % "2.5.1",
 // validate the token from the login on web browser
 // "com.google.api-client" % "google-api-client" % "1.19.1"
 "org.json" % "org.json" % "chargebee-1.0",
 "org.apache.commons" % "commons-email" % "1.3.3",
 "commons-collections" % "commons-collections" % "3.0",
 "com.jayway.jsonpath" % "json-path" % "2.0.0",
 "junit" % "junit" % "4.11",
 "org.im4java" % "im4java" % "1.4.0",
"io.dropwizard.metrics" % "metrics-core" % "3.1.2",
"io.dropwizard.metrics" % "metrics-graphite" % "3.1.2",
"org.elasticsearch" % "metrics-elasticsearch-reporter" % "2.0",
 "net.coobird" % "thumbnailator" % "0.4.8",
  "org.apache.opennlp" % "opennlp-tools" % "1.6.0",
 "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0",
 "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0" classifier "models",
 "de.julielab" % "aliasi-lingpipe" % "4.1.0",
 "com.codahale.metrics" % "metrics-core" % "3.0.2"
)

sources in doc in Compile := List() 
