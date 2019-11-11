enablePlugins(org.nlogo.build.NetLogoExtension)

netLogoExtName := "factbase"

netLogoClassManager := "org.cfpm.factbaseExtension.FactBaseExtension"

netLogoTarget := org.nlogo.build.NetLogoExtension.directoryTarget(baseDirectory.value / "factbase")

netLogoVersion := "6.1.0"

netLogoZipSources := false

version := "2.0.0-SNAPSHOT"

scalaVersion := "2.12.8"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings", "-encoding", "utf8")
