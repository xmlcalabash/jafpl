name := "jafpl"

organization := "com.jafpl"
version      := "0.0.7"
scalaVersion := "2.12.3"

resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"

libraryDependencies ++= Seq(
  "org.apache.logging.log4j" % "log4j-api" % "2.1",
  "org.apache.logging.log4j" % "log4j-core" % "2.1",
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.1",
  "org.slf4j" % "jcl-over-slf4j" % "1.7.10",
  "org.slf4j" % "slf4j-api" % "1.7.0",
  "com.typesafe.akka" %% "akka-actor" % "2.5.4",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.4" % Test,
  "org.scalactic" %% "scalactic" % "3.0.1",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
  "org.scala-lang.modules" %% "scala-swing" % "2.0.0-M2"
)

// Yes, this is an odd place for local use, but it's where the website
// needs them. I should figure out how to parameterize the location...
target in Compile in doc := baseDirectory.value / "build/pages/apidocs"
scalacOptions in (Compile, doc) ++= Seq(
  "-doc-root-content", baseDirectory.value+"/docs/apidocs/root.md"
)

publishTo := Some(Resolver.file("file",
  new File(Path.userHome.absolutePath+"/.m2/repository")))
