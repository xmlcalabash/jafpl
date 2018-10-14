name := "jafpl"

organization := "com.jafpl"
homepage     := Some(url("https://github.com/ndw/jafpl"))
version      := "0.0.63"
scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  "org.apache.logging.log4j" % "log4j-api" % "2.11.0",
  "org.apache.logging.log4j" % "log4j-core" % "2.11.0",
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.11.0",
  "org.slf4j" % "jcl-over-slf4j" % "1.7.25",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "com.typesafe.akka" %% "akka-actor" % "2.5.13",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.13" % Test,
  "org.scalactic" %% "scalactic" % "3.0.5",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "org.scala-lang.modules" %% "scala-xml" % "1.1.0",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.0",
  "org.scala-lang.modules" %% "scala-swing" % "2.0.3"
)

// Yes, this is an odd place for local use, but it's where the website
// needs them. I should figure out how to parameterize the location...
target in Compile in doc := baseDirectory.value / "build/pages/apidocs"
scalacOptions in (Compile, doc) ++= Seq(
  "-doc-root-content", baseDirectory.value+"/docs/apidocs/root.md",
  "-no-link-warnings"
)

// I'm publishing the informal pre-release builds on my own repo
publishTo := Some(Resolver.file("file",
  new File("/space/websites/nwalsh.com/build/website/maven/repo")))
