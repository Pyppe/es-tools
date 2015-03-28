package fi.pyppe.estools

import java.io.File
import java.io.FileWriter
import java.net.URL

import com.ning.http.client.Response
import play.api.libs.json._

import scala.annotation.tailrec
import scala.io.Source
import scala.util.Try

/**
 * Elasticsearch
 *
 * @see http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/reindex.html#reindex
 * @see http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/bulk.html
 */
object Elasticsearch extends LoggerSupport {

  import dispatch._, Defaults._
  import scala.concurrent.Await
  import scala.concurrent.duration._

  private def hitIndexTypeAndId(hit: JsValue, indexOpt: Option[String] = None) = {
    val index = indexOpt.getOrElse((hit \ "_index").as[String])
    Json.obj(
      "_index" -> index,
      "_type" -> hit \ "_type",
      "_id" -> hit \ "_id"
    )
  }

  def reIndex(sourceUrl: String,
              targetUrl: String,
              query: Option[JsObject] = None,
              mapSourceDoc: JsValue => JsValue = identity): Unit = {
    val targetIndex = urlIndex(targetUrl)
    val targetHost = urlHost(targetUrl)
    loggableIndexIterate(sourceUrl, s"Re-indexing to $targetIndex", query) { hits: Seq[JsValue] =>
      val bulkBodyLines: Seq[JsValue] = hits.flatMap { hit =>
        val createJson = hitIndexTypeAndId(hit, Some(targetIndex))
        Seq(Json.obj("index" -> createJson), mapSourceDoc(hit \ "_source"))
      }
      val bulkResponse = {
        val bulkFuture = postText(s"$targetHost/_bulk", bulkBodyLines.mkString("", "\n", "\n"))
        Await.result(bulkFuture, 1.minute)
      }
      require((bulkResponse \ "errors").as[Boolean] == false, s"Errors occurred: ${bulkResponse \\ "error"}")
    }
  }

  def indexFromFile(targetUrl: String, file: File, idField: Option[String]): Unit = {
    require(file.isFile)
    val targetIndex = urlIndex(targetUrl)
    val targetHost = urlHost(targetUrl)
    val targetType = urlIndexType(targetUrl)
    Source.fromFile(file).getLines.grouped(1000).foreach { batch =>
      val bulkBodyLines = batch.flatMap { line =>
        val doc = Json.parse(line)
        val createJson = Json.obj(
          "_index" -> targetIndex,
          "_type" -> targetType
        ) ++ idField.map(id => Json.obj("_id" -> doc \ id)).getOrElse(Json.obj())
        Seq(Json.obj("create" -> createJson), doc)
      }
      val bulkResponse = {
        val bulkFuture = postText(s"$targetHost/_bulk", bulkBodyLines.mkString("", "\n", "\n"))
        Await.result(bulkFuture, 1.minute)
      }
      require((bulkResponse \ "errors").as[Boolean] == false, s"Errors occurred: ${bulkResponse \\ "error"}")
    }
  }

  def updateDocuments(sourceUrl: String, mapSourceDoc: JsValue => JsValue): Unit = {
    var updateCount = 0
    loggableIndexIterate(sourceUrl, s"Updating $sourceUrl", None) { hits: Seq[JsValue] =>
      val bulkBodyLines = hits.flatMap { hit =>
        val doc = hit \ "_source"
        val updatedDoc = mapSourceDoc(doc)
        if (updatedDoc != doc) {
          /*
          println((hit \ "_id").as[String] + " | " + (doc \ "text").as[String])
          println("OLD: " + doc \ "links")
          println("NEW: " + updatedDoc \ "links")
          println("=============")
          */
          Seq(Json.obj("index" -> hitIndexTypeAndId(hit)), updatedDoc)
        } else Nil
      }
      if (bulkBodyLines.nonEmpty) {
        val bulkResponse = {
          val bulkFuture = postText(s"${urlHost(sourceUrl)}/_bulk", bulkBodyLines.mkString("", "\n", "\n"))
          Await.result(bulkFuture, 1.minute)
        }
        require((bulkResponse \ "errors").as[Boolean] == false, s"Errors occurred: ${bulkResponse \\ "error"}")
      }
      updateCount += (bulkBodyLines.size / 2)
    }
    logger.info(s"Updated $updateCount documents")
  }

  def saveIndexToFile(sourceIndex: String, file: File, query: Option[JsObject]): Unit = {
    val fw = new FileWriter(file)
    loggableIndexIterate(sourceIndex, s"Saving to $file", query) { hits =>
      hits.foreach { hit =>
        fw.write(hit.toString + "\n")
      }
    }
    fw.close()
  }

  private def urlHost(url: String) = {
    val u = new URL(url)
    s"${u.getProtocol}://${u.getAuthority}"
  }

  private def urlIndex(url: String) = {
    val u = new URL(url)
    u.getPath.split('/').filter(_.nonEmpty).head
  }

  private def urlIndexType(url: String) = {
    val u = new URL(url)
    u.getPath.split('/').filter(_.nonEmpty)(1)
  }

  private def loggableIndexIterate(sourceUrl: String, action: String, query: Option[JsObject])(handleHits: Seq[JsValue] => Unit): Unit = {
    val queryJs = query.getOrElse(Json.obj("match_all" -> Json.obj()))
    val bodyJson = Json.obj(
      "query" -> queryJs,
      "size" -> 1000
    )
    val sourceHost = urlHost(sourceUrl)

    val future = postJson(s"$sourceUrl/_search?search_type=scan&scroll=1m", bodyJson)
    val res = Await.result(future, 1.minute)
    val totalHits = (res \ "hits" \ "total").as[Long]
    val scrollId = (res \ "_scroll_id").as[String]

    val progress = new ProgressLogger(s"$action [total: $totalHits, source: $sourceUrl]", totalHits)

    @tailrec
    def iterateScroll(accCount: Long, scrollId: String): Unit = {
      progress.logProgress(accCount)
      val future = postText(s"$sourceHost/_search/scroll?scroll=1m", scrollId)
      val responseJson = Await.result(future, 1.minute)
      val hits = (responseJson \ "hits" \ "hits").as[JsArray].value
      hits.size match {
        case 0 =>
          () // Done

        case hitCount =>
          handleHits(hits)
          iterateScroll(accCount + hitCount, (responseJson \ "_scroll_id").as[String])
      }
    }
    iterateScroll(0, scrollId)
  }

  private def postJson(u: String, j: JsValue): Future[JsValue] = {
    Http(url(u).setContentType("application/json", "UTF-8").setBody(j.toString).POST).map(asJson)
  }

  private def postText(u: String, t: String): Future[JsValue] =
    Http(url(u).setContentType("text/plain", "UTF-8").setBody(t).POST).map(asJson)

  private def asJson(r: Response): JsValue =
    Try(Json.parse(r.getResponseBodyAsBytes)).getOrElse {
      throw new Exception(s"Invalid response from ${r.getUri}: ${r.getResponseBody}")
    }

}
