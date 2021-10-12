ThisBuild / organization := "com.jafpl"
ThisBuild / organizationName := "JAFPL"
ThisBuild / organizationHomepage := Some(url("https://jafpl.com/"))
ThisBuild / versionScheme := Some("semver-spec")

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/ndw/jafpl"),
    "scm:git@github.com:ndw/jafpl.git"
  )
)

ThisBuild / developers := List(
  Developer(
    id    = "ndw",
    name  = "Norman Walsh",
    email = "ndw@nwalsh.com",
    url   = url("https://nwalsh.com/")
  )
)

ThisBuild / description := "Just Another Fine Pipeline Language."
ThisBuild / licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / homepage := Some(url("https://jafpl.com"))

// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true

credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
credentials += Credentials(
            "GnuPG Key ID",
            "gpg",
            "4C5F68D09D42BA7FAC888DF9A929EA2321FDBF8F",
            System.getenv("SONATYPE_GPG_PASSPHRASE"))

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

