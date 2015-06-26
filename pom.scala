import org.sonatype.maven.polyglot.scala.model._
import scala.collection.immutable.Seq

implicit val scalaVersion = System.getenv("SCALA_VERSION") match {
  case null => ScalaVersion("2.11.6")
  case v => ScalaVersion(v)
}

ScalaModel(
  "com.github.lefou" %% "fscleaner" % "0.0.1-SNAPSHOT",
  modelVersion = "4.0.0",
  properties = Map(
    "project.build.sourceEncoding" -> "UTF-8"
  ),
  dependencies = Seq(
    "com.typesafe" % "config" % "1.3.0",
    "de.tototec" % "de.tototec.cmdoption" % "0.4.2"
      
//    "junit" % "junit" % "3.8.1" % "test"
  ),
  build = Build(
    outputDirectory = "${project.build.directory}/classes_" + scalaVersion.binaryVersion
  )
)
