import sbt._
import Keys._

object SbtCaliperBuild extends Build {
  lazy val root = Project(
    id = "sbtInbase",
    base = file("."),
    settings = Project.defaultSettings ++ ScriptedPlugin.scriptedSettings ++
      Seq(
        ScriptedPlugin.scriptedBufferLog := false ,
        ScriptedPlugin.scriptedLaunchOpts += ("-Dplugin.version=" + version.value),
        ScriptedPlugin.sbtTestDirectory := baseDirectory.value / "sbt-test"
      )
  )
}
