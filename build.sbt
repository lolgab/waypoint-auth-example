scalaVersion := "2.13.4"

enablePlugins(ScalaJSPlugin)

libraryDependencies ++= Seq(
  "com.raquo" %%% "laminar" % "0.11.0",
  "com.raquo" %%% "waypoint" % "0.2.0",
  "com.lihaoyi" %%% "upickle" % "1.2.2"
)

scalaJSUseMainModuleInitializer := true
