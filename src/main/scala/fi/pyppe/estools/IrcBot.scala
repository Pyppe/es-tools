package fi.pyppe.estools

import play.api.libs.json.{JsObject, Json, JsValue}

/** For personal use by pyppe */
object IrcBot {

  def parseUrls(text: String): List[String] =
    text.split("\\s+").map(_.trim).filter(_.matches("(?i)^(ftp|https?)://.+")).
      map(_.replaceAll("(.*)[,!.:?()<>]$", "$1")).toList

  def modifyLinks(msg: JsValue): JsValue = {
    val text = (msg \ "text").as[String]
    val links = parseUrls(text)
    msg.as[JsObject] ++ Json.obj("links" -> links, "linkCount" -> links.size)
  }

}
