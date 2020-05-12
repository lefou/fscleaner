import mill._
import mill.scalalib._

def baseDir = build.millSourcePath

object fscleaner extends SbtModule {
  override def scalaVersion = "2.11.7"
  override def millSourcePath = baseDir
  override def ivyDeps = Agg(
    ivy"com.typesafe:config:1.3.0",
    ivy"de.tototec:de.tototec.cmdoption:0.4.2"
  )
}
