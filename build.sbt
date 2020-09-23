val http4sVersion = "0.21.6"

ThisBuild / scalaVersion := "2.13.3"
ThisBuild / organization := "com.github.danny02"
ThisBuild / maintainer := "github.com/danny02"

lazy val universe = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .in(file("universe"))
  .settings(
    version := "0.1-SNAPSHOT",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "upickle"   % "1.2.0",
      "com.lihaoyi" %%% "autowire"  % "0.3.2",
      "com.lihaoyi" %%% "scalatags" % "0.9.1"
    )
  )
  .jvmConfigure(_.enablePlugins(JavaAppPackaging))
  .jvmSettings(
    name := "universe-server",
    // Add JVM-specific settings here,
    libraryDependencies ++= Seq(
      "org.webjars"    % "bootstrap"           % "4.5.2",
      "org.http4s"    %% "http4s-dsl"          % http4sVersion,
      "org.http4s"    %% "http4s-blaze-server" % http4sVersion,
      "ch.qos.logback" % "logback-classic"     % "1.2.3"
    )
  )
  .jsSettings(
    name := "universe-client",
    // Add JS-specific settings here
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "1.1.0"
    )
  )

lazy val universeJS = universe.js
lazy val universeJVM = universe.jvm.settings(
  (resources in Compile) += {
    (fastOptJS in (universeJS, Compile)).value
    (artifactPath in (universeJS, Compile, fastOptJS)).value
  }
)
