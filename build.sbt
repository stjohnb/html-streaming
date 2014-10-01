name := "html-streaming"

version := "1.0-SNAPSHOT"

play.Project.playScalaSettings

play.Keys.templatesTypes ++= Map("stream" -> "ui.HtmlStreamFormat")
