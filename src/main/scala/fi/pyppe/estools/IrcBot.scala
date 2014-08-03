package fi.pyppe.estools

import play.api.libs.json.{JsObject, Json, JsValue}

/** For personal use by pyppe */
object IrcBot {

  val UrlRegex = ("(?i)\\b(((ht|f)tp(s?)\\:\\/\\/|~\\/|\\/)|www.)" +
    "(\\w+:\\w+@)?(([-\\w]+\\.)+(com|org|net|gov" +
    "|mil|biz|info|mobi|name|aero|jobs|museum" +
    "|travel|[a-z]{2}))(:[\\d]{1,5})?" +
    "(((\\/([-\\w~!$+|.,=]|%[a-f\\d]{2})+)+|\\/)+|\\?|#)?" +
    "((\\?([-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?" +
    "([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)" +
    "(&(?:[-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?" +
    "([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)*)*" +
    "(#([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)?\\b").r

  def parseUrls(text: String): List[String] =
    UrlRegex.findAllMatchIn(text).map(_.group(0)).toList

  def modifyLinks(msg: JsValue): JsValue = {
    (msg \ "linkCount").as[Int] match {
      case 0 => msg
      case _ =>
        val links = parseUrls((msg \ "text").as[String])
        (msg.as[JsObject]) ++ Json.obj("links" -> links, "linkCount" -> links.size)
    }
  }

}
