package weeshy.sbtinbase

import sbt._
import Keys._
import Def.ScopedKey

object SbtInBase extends Plugin {
  import HiddenKeys._

  val inBase = settingKey[Boolean]("use sources from base directory")
  val inBaseSuffix = settingKey[Option[String]]("search for source files basing on suffix")
  val inBasePattern = settingKey[String]("search pattern for sources")
  val inBaseProxyDir = settingKey[File]("directory to store proxy files")
  val inBaseSanitize = taskKey[Unit]("clean all unused symbolic links")

  override lazy val projectSettings =
    baseSettings ++
    compileSettings ++
    testSettings ++ Seq(
      sourcesInBase := false
    )

  lazy val baseSettings : Seq[Setting[_]] = Seq(
    inBase := (inBase in Compile).value || (inBase in Test).value,
    inBaseSuffix := Some(".scala"),
    inBasePattern := "*%s".format( inBaseSuffix.value.getOrElse("") ),
    inBaseProxyDir := baseDirectory.value / ".inbase",
    inBaseSanitize <<= ( (inBaseSanitize in Compile), (inBaseSanitize in Test), inBaseProxyDir, inBase, sourcesInBase ) map { (cis, tis, pd, ib, sib) =>
      HiddenWays.sanitizeParent(pd, ib, sib)
    },
    basedFiles := {
      if ( inBase.value && (! sourcesInBase.value) )
        ( (baseDirectory.value * inBasePattern.value).get.toSet -- (basedFiles in Test).value  -- (basedFiles in Compile).value ).toSeq
      else Seq()
    },
    linkedFiles := HiddenWays.populateLinks( baseDirectory.value, inBaseProxyDir.value, basedFiles.value )
  )

  lazy val compileSettings : Seq[Setting[_]] = inConfig(Compile) ( Seq (
    inBase := false,
    inBaseSuffix := {
      ScopedKey( Scope(This, Global, Global, Global), inBaseSuffix.key).value match {
        case None => Some(".scala")
        case Some(sfx) => Some(sfx)
      }
    },
    inBasePattern := "*%s".format( inBaseSuffix.value.getOrElse("") ),
    inBaseProxyDir := ScopedKey( Scope(This, Global, Global, Global), inBaseProxyDir.key).value / "main",
    inBaseSanitize := HiddenWays.sanitizeChild(inBaseProxyDir.value, linkedFiles.value),
    basedFiles := {
      if ( inBase.value && (! sourcesInBase.value) ) {
        val totalFilter = ( (includeFilter in managedSources).value -- (excludeFilter in managedSources).value ) && inBasePattern.value
        ( (baseDirectory.value * totalFilter).get.toSet -- (basedFiles in Test).value ).toSeq
      } else Seq()
    },
    linkedFiles := HiddenWays.populateLinks( baseDirectory.value, inBaseProxyDir.value, basedFiles.value ),
    sourceGenerators <+= linkedFiles,
    managedSourceDirectories ++= {
      if ( inBase.value && (! sourcesInBase.value) )
        Seq(inBaseProxyDir.value)
      else Seq()
    },
    (includeFilter in managedSources) := (includeFilter in unmanagedSources).value,
    (excludeFilter in managedSources) := (excludeFilter in unmanagedSources).value
  ) )

  lazy val testSettings : Seq[Setting[_]] = inConfig(Test) ( Seq (
    inBase := false,
    inBaseSuffix := {
      ScopedKey( Scope(This, Global, Global, Global), inBaseSuffix.key).value match {
        case None => Some("Test.scala")
        case Some(sfx) => Some(s"Test${sfx}")
      }
    },
    inBasePattern := "*%s".format( inBaseSuffix.value.getOrElse("") ),
    inBaseProxyDir := ScopedKey( Scope(This, Global, Global, Global), inBaseProxyDir.key).value / "test",
    inBaseSanitize := HiddenWays.sanitizeChild(inBaseProxyDir.value, linkedFiles.value),
    basedFiles := {
      if ( inBase.value && (! sourcesInBase.value) ) {
        val totalFilter = ( (includeFilter in managedSources).value -- (excludeFilter in managedSources).value ) && inBasePattern.value
        (baseDirectory.value * totalFilter).get
      } else Seq()
    },
    linkedFiles := HiddenWays.populateLinks( baseDirectory.value, inBaseProxyDir.value, basedFiles.value ),
    sourceGenerators <+= linkedFiles,
    managedSourceDirectories ++= {
      if ( inBase.value && (! sourcesInBase.value) )
        Seq(inBaseProxyDir.value)
      else Seq()
    },
    (includeFilter in managedSources) := (includeFilter in unmanagedSources).value,
    (excludeFilter in managedSources) := (excludeFilter in unmanagedSources).value
  ) )

  private object HiddenKeys {
    val basedFiles = taskKey[Seq[File]]("files found in base")
    val linkedFiles = taskKey[Seq[File]]("files linked to the proxy folder")
  }

  private object HiddenWays {
    import scala.sys.process._

    def sanitizeParent(proxydir : File, inBase : Boolean, sourcesInBase : Boolean) : Unit = {
      // directory proxy is disabled if either inBase is disabled or sourcesInBase enabled (causing conflict)
      if ( proxydir.exists() && ( sourcesInBase || (! inBase) ) )
        IO.delete(proxydir)
    }
    def sanitizeChild(proxydir : File, legal : Seq[File]) : Unit = {
      if ( proxydir.exists() ) {
        val allFiles = IO.listFiles(proxydir)
        val illegal = allFiles.toSet -- legal.toSet
        IO.delete(illegal)
      }
    }
    def populateLinks(basedir : File, target : File, members : Seq[File]) : Seq[File] = {
      if ( members.nonEmpty )
        target.mkdirs()
      members.map( f => {
        val rel = f.getName
        val ft = target / rel
        Seq ("ln", "-s", f.getPath, ft.getPath).!
        ft
      } )
    }
  }
}
