import play.Project._

name := """reactive-java8-play"""

version := "1.0-SNAPSHOT"

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

libraryDependencies ++= Seq(
  "mysql" % "mysql-connector-java" % "5.1.31",
  "com.sun.mail" % "javax.mail" % "1.5.2",
  "org.webjars" %% "webjars-play" % "2.2.0",
  "org.webjars" % "bootstrap" % "2.3.1")

playJavaSettings

initialize := {
  val _ = initialize.value
  if (sys.props("java.specification.version") != "1.8")
    sys.error("Java 8 is required for this project.")
}