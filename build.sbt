import java.io.{BufferedReader, InputStreamReader}

name := "jafpl"

organization := "com.jafpl"
homepage     := Some(url("https://jafpl.com/"))
version      := "0.3.44"
scalaVersion := "2.13.5"

Global / excludeLintKeys += homepage
Global / excludeLintKeys += organization

buildInfoUsePackageAsPath := true

buildInfoKeys ++= Seq[BuildInfoKey](
  BuildInfoKey.action("buildTime") {
    System.currentTimeMillis
  },
  // Hat tip to: https://stackoverflow.com/questions/24191469/how-to-add-commit-hash-to-play-templates
  "gitHash" -> new java.lang.Object() {
    override def toString: String = {
      try {
        val extracted = new InputStreamReader(
          java.lang.Runtime.getRuntime.exec("git rev-parse HEAD").getInputStream
        )
        new BufferedReader(extracted).readLine
      } catch {
        case _: Exception => "FAILED"
      }
    }}.toString()
)

lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion),
    buildInfoPackage := "com.jafpl.sbt"
  )

lazy val failTask = taskKey[Unit]("Force the build to fail")
failTask := {
  throw new sbt.MessageOnlyException("No build for you.")
}

// Redefine publish so that it will fail if the repo is dirty
publish := Def.taskDyn {
  val default = publish.taskValue

  val shortstat = {
    try {
      val extracted = new InputStreamReader(
        java.lang.Runtime.getRuntime.exec("git diff --shortstat").getInputStream
      )
      var diff = ""
      val reader = new BufferedReader(extracted)
      var line = reader.readLine
      while (line != null) {
        diff = line
        line = reader.readLine
      }
      reader.close()
      diff
    } catch {
      case _: Exception => "FAILED"
    }
  }

  val status = {
    try {
      val extracted = new InputStreamReader(
        java.lang.Runtime.getRuntime.exec("git status --porcelain").getInputStream
      )
      var newFile = ""
      val reader = new BufferedReader(extracted)
      var line = reader.readLine
      while (line != null) {
        if (line.startsWith("??")) {
          newFile = line
        }
        line = reader.readLine
      }
      reader.close()
      newFile
    } catch {
      case _: Exception => "FAILED"
    }
  }

  val message = if (shortstat != "") {
    if (status != "") {
      Some("Repository has changed and untracked files.")
    } else {
      Some("Repository has changed files.")
    }
  } else if (status != "") {
    Some("Repository has untracked files.")
  } else {
    None
  }

  if (message.isDefined) {
    println(message.get)
  }

  if (message.isDefined) {
    Def.taskDyn {
      failTask
    }
  } else {
    Def.task(default.value)
  }
}.value

libraryDependencies ++= Seq(
  "org.apache.logging.log4j" % "log4j-api" % "2.12.1",
  "org.apache.logging.log4j" % "log4j-core" % "2.12.1",
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.12.1",
  "org.slf4j" % "jcl-over-slf4j" % "1.7.25",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  //"org.scalactic" %% "scalactic" % "3.2.3",
  "org.scalatest" %% "scalatest" % "3.2.3" % "test",
  "org.scala-lang.modules" %% "scala-xml" % "1.3.0",
  //"org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
  //"org.scala-lang.modules" %% "scala-swing" % "2.1.1"
)

// Yes, this is an odd place for local use, but it's where the website
// needs them. I should figure out how to parameterize the location...
Compile / target / doc := baseDirectory.value / "build/pages/apidocs"
//scalacOptions in (Compile, doc) ++= Seq(
//  "-doc-root-content", baseDirectory.value+"/docs/apidocs/root.md",
//  "-no-link-warnings", "-deprecation"
//)

scalacOptions := Seq("-unchecked", "-deprecation")
