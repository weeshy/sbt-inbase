lazy val requireSourcesInBase = taskKey[Unit]("require sourcesInBase be true")
lazy val requireProjectInBase = taskKey[Unit]("require inBase in current projct be true")
lazy val requireCompileInBase = taskKey[Unit]("require inBase in Compile be true")
lazy val requireTestInBase = taskKey[Unit]("require inBase in Test be true")
lazy val requireCompileProxy = taskKey[Unit]("require compile sources to contain proxy")
lazy val requireTestProxy = taskKey[Unit]("require test sources to contain proxy")
lazy val requireFindCompile = taskKey[Unit]("require compile to find files")
lazy val requireFindTest = taskKey[Unit]("require test to find files")

requireSourcesInBase := {
  if (! sourcesInBase.value)
    error("expected sourcesInBase to be true")
}

requireProjectInBase := {
  if (! inBase.value)
    error("expected inBase to be true")
}

requireCompileInBase := {
  if (! (inBase in Compile).value)
    error("expected compile:inBase be true")
}

requireTestInBase := {
  if (! (inBase in Test).value)
    error("expected test:inBase be true")
}

requireCompileProxy := {
  if ( ! (managedSourceDirectories in Compile).value.exists(_.getPath.endsWith(".inbase/main")) )
    error("compile:managedSourceDirectories should contain proxy")
}

requireTestProxy := {
  if ( ! (managedSourceDirectories in Test).value.exists(_.getPath.endsWith(".inbase/test")) )
    error("test:managedSourceDirectories should contain proxy")
}

requireFindCompile := {
  if ( ! (managedSources in Compile).value.forall(_.getPath.endsWith(".inbase/main/A.scala")) )
    error("compile:managedSources should contain only .inbase/main/A.scala")
}

requireFindTest := {
  if ( ! (managedSources in Test).value.forall(_.getPath.endsWith(".inbase/test/BTest.scala")) )
    error("test::managedSources should contain only .inbase/test/BTest.scala")
}
