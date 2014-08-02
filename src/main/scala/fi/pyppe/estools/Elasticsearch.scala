package fi.pyppe.estools

import java.io.File
import java.io.FileWriter
import java.net.URL

import com.ning.http.client.Response
import com.typesafe.scalalogging.StrictLogging

import scala.annotation.tailrec

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
  import scalaz._, Scalaz._
  import argonaut._, Argonaut._

  private val scrollIdLens = jObjectPL >=> jsonObjectPL("_scroll_id") >=> jStringPL
  private val totalHitsLens =
    jObjectPL >=> jsonObjectPL("hits") >=>
      jObjectPL >=> jsonObjectPL("total") >=> jNumberPL
  private val hitsArrayLens =
    jObjectPL >=> jsonObjectPL("hits") >=>
      jObjectPL >=> jsonObjectPL("hits") >=> jArrayPL

  def reIndex(sourceIndex: String, target: String): Unit = {
    loggableIndexIterate(sourceIndex, "Re-indexing") { hits: List[Json] =>
      println(hits.size + ": TODO")
    }
  }

  def saveIndexToFile(sourceIndex: String, file: File): Unit = {
    val fw = new FileWriter(file)
    loggableIndexIterate(sourceIndex, s"Saving to $file") { hits =>
      hits.foreach { hit =>
        fw.write(hit.nospaces + "\n")
      }
    }
    fw.close()
  }

  private def loggableIndexIterate(sourceIndex: String, action: String)(handleHits: List[Json] => Unit): Unit = {
    val sourceHost = {
      val u = new URL(sourceIndex)
      s"${u.getProtocol}://${u.getAuthority}"
    }

    val bodyJson = Json.obj(
      "query" -> Json.obj("match_all" -> jEmptyObject),
      "size" -> jNumber(1000)
    )

    val future = Http(url(s"$sourceIndex/_search?search_type=scan&scroll=1m").POST.setBody(bodyJson.nospaces)).map(asJson)
    val res = Await.result(future, 1.minute)
    val totalHits = totalHitsLens.get(res).get.toLong
    val scrollId = scrollIdLens.get(res).get

    val progress = new ProgressLogger(s"$action $totalHits entries from $sourceIndex", totalHits)

    @tailrec
    def iterateScroll(accCount: Long, scrollId: String): Unit = {
      progress.logProgress(accCount)
      val future = Http(url(s"$sourceHost/_search/scroll?scroll=1m").POST.setBody(scrollId)).map(asJson)
      val responseJson = Await.result(future, 1.minute)
      val hits = hitsArrayLens.get(responseJson).get
      hits.size match {
        case 0 =>
          () // Done

        case hitCount =>
          handleHits(hits)
          iterateScroll(accCount + hitCount, scrollIdLens.get(responseJson).get)
      }
    }
    iterateScroll(0, scrollId)
  }

  private def asJson(r: Response): Json =
    Parse.parseOption(r.getResponseBody).get

}
