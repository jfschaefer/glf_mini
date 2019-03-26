scalaVersion := "2.12.3"

// for json
libraryDependencies += "net.liftweb" % "lift-webkit_2.12" % "3.3.0"
// for http requests
libraryDependencies +=  "org.scalaj" %% "scalaj-http" % "2.4.1"

libraryDependencies += "info.kwarc.mmt" % "mmt" % "1.0" % "provided" from "file:///home/jfs/git/github_com/jfschaefer/glf_mini/mmt.jar"
