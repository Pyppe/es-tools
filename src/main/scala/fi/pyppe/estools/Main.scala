package fi.pyppe.estools

import java.io.File

import org.rogach.scallop.{ScallopOption, Subcommand, ScallopConf}

object Main extends App {

  private val ReIndex = "re-index"
  private val SaveToFile = "save-to-file"
  private val IndexFromFile = "index-from-file"
  val Version = getClass.getPackage.getImplementationVersion
  private val Title = {
    val title = s"es-tools $Version"
    val line = "="*title.size
    Seq(line,title,line).mkString("\n")
  }

  val opts = new ScallopConf(args) {
    banner(
      s"""
        |$Title
        |Helpful tools for Elasticsearch.
        |See https://github.com/Pyppe/es-tools for details
        |""".stripMargin)

    val reIndex = new Subcommand(ReIndex) {
      val source = trailArg[String]("source-url", descr = "E.g. http://localhost:9200/twitter", required = true)
      val target = trailArg[String]("target-url", descr = "E.g. http://localhost:9200/twitter_v2", required = true)
    }
    val saveToFile = new Subcommand(SaveToFile) {
      descr("Save given source index to a temporary file")
      val source = trailArg[String]("source-url", descr = "E.g. http://localhost:9200/twitter", required = true)
      val file = trailArg[String]("target-file", descr = "E.g. /tmp/myfile.json (if omitted, one will be created to temp directory)", required = false)
    }
    val indexFromFile = new Subcommand(IndexFromFile) {
      descr("Index json-rows from a file")
      val file = trailArg[String]("file", descr = "E.g. /tmp/rows.json (json objects separated by newlines)", required = true)
      val target = trailArg[String]("target-url", descr = "E.g. http://localhost:9200/twitter/tweet", required = true)
      val id = trailArg[String](descr = "If set, will user given field as a _id when indexing", required = false)
    }
  }

  args.head match {
    case ReIndex =>
      Elasticsearch.reIndex(opts.reIndex.source(), opts.reIndex.target())

    case SaveToFile =>
      val file = new File(opts.saveToFile.file.get.getOrElse(File.createTempFile("es-tools_", ".json").getAbsolutePath))
      Elasticsearch.saveIndexToFile(opts.saveToFile.source(), file)

    case IndexFromFile =>
      val file = new File(opts.indexFromFile.file())
      require(file.isFile, s"$file is not an existing file")
      Elasticsearch.indexFromFile(opts.indexFromFile.target(), file, opts.indexFromFile.id.get)
  }
  dispatch.Http.shutdown()

}
