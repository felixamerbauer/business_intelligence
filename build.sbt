name := "business_intelligence"

version := "0.1.0"
 
scalaVersion := "2.10.1"

// SBT-Eclipse settings
EclipseKeys.executionEnvironment := Some(EclipseExecutionEnvironment.JavaSE17)

EclipseKeys.withSource := true
  
libraryDependencies ++= Seq(
  "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "org.slf4j" % "slf4j-log4j12" % "1.7.5",
  "joda-time" % "joda-time" % "2.2",
  "org.joda" % "joda-convert" % "1.3.1",
  "junit" % "junit" % "4.10" % "test",
  "org.scalatest" %% "scalatest" % "1.9.1" % "test->default"
)