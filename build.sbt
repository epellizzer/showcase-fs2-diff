organization := "fr.ericpellizzer"
name := "showcase-fs2-diff"
version := "dev-SNAPSHOT"

val catsVersion = "2.0.0"
val fs2Version = "2.0.0"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "cats-effect" % catsVersion,

  "co.fs2" %% "fs2-core" % fs2Version,
  "co.fs2" %% "fs2-io" % fs2Version,

  "com.lihaoyi" %% "utest" % "0.7.1" % "test"
)

testFrameworks += new TestFramework("utest.runner.Framework")
