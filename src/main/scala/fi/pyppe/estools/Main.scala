package fi.pyppe.estools

import java.io.File

import org.rogach.scallop.{ScallopOption, Subcommand, ScallopConf}
import play.api.libs.json.{JsObject, Json}

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

  private val queryDescription = """E.g. {range: {"time":{"gt":"now-1w"}}} (depends on your data), defaults to {"match_all":{}}"""

  val opts = new ScallopConf(args) {
    banner(
      s"""
        |$Title
        |Helpful tools for Elasticsearch.
        |See https://github.com/Pyppe/es-tools for details
        |""".stripMargin)

    val reIndex = new Subcommand(ReIndex) {
      val query  = opt[String]("query", descr = queryDescription, required = false)
      val source = trailArg[String]("source-url", descr = "E.g. http://localhost:9200/twitter", required = true)
      val target = trailArg[String]("target-url", descr = "E.g. http://localhost:9200/twitter_v2", required = true)
    }
    val saveToFile = new Subcommand(SaveToFile) {
      descr("Save given source index to a temporary file")
      val query  = opt[String]("query", descr = queryDescription, required = false)
      val source = trailArg[String]("source-url", descr = "E.g. http://localhost:9200/twitter", required = true)
      val file = trailArg[String]("target-file", descr = "E.g. /tmp/myfile.json (if omitted, one will be created to temp directory)", required = false)
    }
    val indexFromFile = new Subcommand(IndexFromFile) {
      descr("Index json-rows from a file")
      val file = trailArg[String]("file", descr = "E.g. /tmp/rows.json (json objects separated by newlines)", required = true)
      val target = trailArg[String]("target-url", descr = "E.g. http://localhost:9200/twitter/tweet", required = true)
      val id = trailArg[String](descr = "If set, will use given field as a _id when indexing", required = false)
    }
  }

  def parseQuery(q: Option[String]): Option[JsObject] = {
    val query: Option[JsObject] = q.map(Json.parse).map(_.as[JsObject])
    query.foreach(q => println(s"Using query: ${Json.prettyPrint(q)}"))
    query
  }

  args.head match {
    case ReIndex =>
      val o = opts.reIndex
      val query = parseQuery(o.query.get)
      Elasticsearch.reIndex(o.source(), o.target(), query)

    case SaveToFile =>
      val o = opts.saveToFile
      val query = parseQuery(o.query.get)
      val file = new File(o.file.get.getOrElse(File.createTempFile("es-tools_", ".json").getAbsolutePath))
      Elasticsearch.saveIndexToFile(o.source(), file, query)

    case IndexFromFile =>
      val o = opts.indexFromFile
      val file = new File(o.file())
      require(file.isFile, s"$file is not an existing file")
      Elasticsearch.indexFromFile(o.target(), file, o.id.get)
  }
  dispatch.Http.shutdown()

}
