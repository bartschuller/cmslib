import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "cmslib"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "org.webjars" %% "webjars-play" % "2.1.0-3",
    "org.webjars" % "bootstrap" % "3.0.0",
    "org.webjars" % "ckeditor" % "4.1.2",
    "org.reactivemongo" %% "play2-reactivemongo" % "0.9",
    "be.objectify" %% "deadbolt-scala" % "2.1-RC3",
    "org.mindrot" % "jbcrypt" % "0.3m"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    resolvers += Resolver.url("Objectify Play Repository", url("http://schaloner.github.com/releases/"))(Resolver.ivyStylePatterns)
  )

}
