name := "cahp-pearl"

version := "0.1"

scalaVersion := "2.13.12"
addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % "3.6.1" cross CrossVersion.full)

resolvers ++= Resolver.sonatypeOssRepos("releases")

libraryDependencies ++= Seq(
    "edu.berkeley.cs" %% "chisel3" % "3.6.1",
)

scalacOptions ++= Seq(
      "-Xsource:2.13",
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit"
    )
