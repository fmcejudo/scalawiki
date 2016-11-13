package org.scalawiki

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.io.IO
import akka.pattern.ask
import org.jsoup.Jsoup
import org.scalawiki.dto._
import org.scalawiki.dto.cmd.action.QueryAction
import org.scalawiki.http.{HttpClient, HttpClientSpray}
import org.scalawiki.json.MwReads._
import org.scalawiki.query.{DslQuery, PageQuery, SinglePageQuery}
import play.api.libs.json._
import spray.can.Http
import spray.http._
import spray.util._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait MwBot {

  def host: String

  def login(user: String, password: String): Future[String]

  def run(action: QueryAction, context: Map[String, String] = Map.empty): Future[Seq[Page]]

  //def read(action: Action, context: Map[String, String] = Map.empty): Future[ParseDefinition]

  def get(params: Map[String, String]): Future[String]

  def post(params: Map[String, String]): Future[String]

  def getByteArray(url: String): Future[Array[Byte]]

  def post[T](reads: Reads[T], params: (String, String)*): Future[T]

  def post[T](reads: Reads[T], params: Map[String, String]): Future[T]

  def postMultiPart[T](reads: Reads[T], params: Map[String, String]): Future[T]

  def postFile[T](reads: Reads[T], params: Map[String, String], fileParam: String, filename: String): Future[T]

  def page(title: String): SinglePageQuery

  def page(id: Long): SinglePageQuery

  def pageText(title: String): Future[String]

  def token: String

  def await[T](future: Future[T]): T

  def system: ActorSystem

  def log: LoggingAdapter
}

case class MediaWikiVersion(version: String) extends Ordered[MediaWikiVersion] {
  override def compare(that: MediaWikiVersion): Int =
    version.compareTo(that.version)
}

object MediaWikiVersion {

  val UNKNOWN = MediaWikiVersion("(UNKNOWN)")

  val MW_1_24 = MediaWikiVersion("1.24")

  def fromGenerator(generator: String) = {
    "MediaWiki (\\d+\\.\\d+)".r
      .findFirstMatchIn(generator)
      .map(_.group(1))
      .fold(UNKNOWN)(MediaWikiVersion.apply)
  }
}

class MwBotImpl(val site: Site,
                val http: HttpClient = new HttpClientSpray(MwBot.system),
                val system: ActorSystem = MwBot.system
               ) extends MwBot {

  def this(host: String) = this(Site.host(host))

  def this(host: String, http: HttpClient) = this(Site.host(host), http)

  implicit val sys = system

  import system.dispatcher

  def host = site.domain

  val baseUrl: String = site.protocol + "://" + host + site.scriptPath

  val indexUrl = baseUrl + "/index.php"

  val apiUrl = baseUrl + "/api.php"

  def encodeTitle(title: String): String = MwUtils.normalize(title)

  override def log = system.log

  override def login(user: String, password: String): Future[String] = {
    require(user != null, "User is null")
    require(password != null, "Password is null")

    log.info(s"$host login user: $user")

    tryLogin(user, password).flatMap {
      loginResponse =>
        tryLogin(user, password, loginResponse.token).map(_.result)
    }
  }

  def tryLogin(user: String, password: String, token: Option[String] = None): Future[LoginResponse] = {
    val loginParams = Map(
      "action" -> "login", "lgname" -> user, "lgpassword" -> password, "format" -> "json"
    ) ++ token.map("lgtoken" -> _)

    http.post(apiUrl, loginParams).map { resp =>
      val body = http.getBody(resp)

      resp.entity.toOption.map(_.contentType) match {
        case Some(HttpClient.JSON_UTF8) =>
          val jsResult = Json.parse(body).validate(loginResponseReads)
          if (jsResult.isError) {
            throw new RuntimeException("Parsing failed: " + jsResult)
          } else {
            jsResult.get
          }
        case x =>
          val html = Jsoup.parse(body)
          val details = html.select("code").first().text()
          throw new MwException(resp.status.toString(), details)
      }
    }
  }

  override lazy val token: String = await(getToken)

  lazy val mediaWikiVersion: MediaWikiVersion = await(getMediaWikiVersion)

  def getMediaWikiVersion: Future[MediaWikiVersion] =
    get(siteInfoReads, "action" -> "query", "meta" -> "siteinfo") map MediaWikiVersion.fromGenerator

  def getToken: Future[String] = {
    val isMW_1_24 = mediaWikiVersion >= MediaWikiVersion.MW_1_24

    if (isMW_1_24) {
      get(tokenReads, "action" -> "query", "meta" -> "tokens")
    } else {
      get(editTokenReads, "action" -> "query", "prop" -> "info", "intoken" -> "edit", "titles" -> "foo")
    }
  }

  def getTokens = get(tokensReads, "action" -> "tokens")

  override def run(action: QueryAction, context: Map[String, String] = Map.empty): Future[Seq[Page]] =
    new DslQuery(action, this, context).run()

  /*override def read(action: Action, context: Map[String, String]): Future[ParseDefinition] =
    new DslQuery(action, this, context).read()*/

  def get[T](reads: Reads[T], params: (String, String)*): Future[T] =
    http.get(getUri(params: _*)) map {
      body =>
        parseJson(reads, body).get
    }

  def parseJson[T](reads: Reads[T], body: String): JsResult[T] =
    Json.parse(body).validate(reads)

  def parseResponse[T](reads: Reads[T], response: Future[HttpResponse]): Future[T] =
    response map http.getBody map {
      body =>
        parseJson(reads, body).getOrElse {
          throw parseJson(errorReads, body).get
        }
    }

  override def getByteArray(url: String): Future[Array[Byte]] =
    http.getResponse(url) map {
      response => response.entity.data.toByteArray
    }

  override def post[T](reads: Reads[T], params: (String, String)*): Future[T] =
    post(reads, params.toMap)

  override def post[T](reads: Reads[T], params: Map[String, String]): Future[T] =
    parseResponse(reads, http.post(apiUrl, params))

  override def postMultiPart[T](reads: Reads[T], params: Map[String, String]): Future[T] =
    parseResponse(reads, http.postMultiPart(apiUrl, params))

  override def postFile[T](reads: Reads[T], params: Map[String, String], fileParam: String, filename: String): Future[T] =
    parseResponse(reads, http.postFile(apiUrl, params, fileParam, filename))

  def pagesByTitle(titles: Set[String]) = PageQuery.byTitles(titles, this)

  def pagesById(ids: Set[Long]) = PageQuery.byIds(ids, this)

  override def page(title: String) = PageQuery.byTitle(title, this)

  override def page(id: Long) = PageQuery.byId(id, this)

  override def pageText(title: String): Future[String] = {
    val url = getIndexUri("title" -> encodeTitle(title), "action" -> "raw")
    http.get(url)
  }

  def getIndexUri(params: (String, String)*) =
    Uri(indexUrl) withQuery (params ++ Seq("format" -> "json"): _*)

  def getUri(params: (String, String)*) =
    Uri(apiUrl) withQuery (params ++ Seq("format" -> "json"): _*)

  override def get(params: Map[String, String]): Future[String] = {
    val uri: Uri = getUri(params)

    log.info(s"$host GET url: $uri")
    http.get(uri)
  }

  override def post(params: Map[String, String]): Future[String] = {
    val uri: Uri = Uri(apiUrl)
    log.info(s"$host POST url: $uri, params: $params")
    http.postUri(uri, params ++ Map("format" -> "json")) map http.getBody
  }

  def getUri(params: Map[String, String]) =
    Uri(apiUrl) withQuery (params ++ Map("format" -> "json"))

  def shutdown(): Unit = {
    IO(Http).ask(Http.CloseAll)(1.second).await
    system.shutdown()
  }

  override def await[T](future: Future[T]) = Await.result(future, http.timeout)
}

object MwBot {

  import spray.caching.{Cache, LruCache}

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent._

  val system = ActorSystem()

  def create(site: Site,
             loginInfo: Option[LoginInfo],
             http: HttpClient = new HttpClientSpray(MwBot.system)
            ): MwBot = {
    val bot = new MwBotImpl(site, http)

    loginInfo.foreach {
      info =>
        bot.await(bot.login(info.login, info.password))
    }
    bot
  }

  val cache: Cache[MwBot] = LruCache()

  def fromHost(host: String,
               loginInfo: Option[LoginInfo] = LoginInfo.fromEnv(),
               http: HttpClient = new HttpClientSpray(MwBot.system)
              ): MwBot = {
    fromSite(Site.host(host), loginInfo, http)
  }

  def fromSite(site: Site,
               loginInfo: Option[LoginInfo] = LoginInfo.fromEnv(),
               http: HttpClient = new HttpClientSpray(MwBot.system)
              ): MwBot = {
    Await.result(cache(site.domain) {
      Future {
        create(site, loginInfo, http)
      }
    }, 1.minute)
  }

  val commons: String = Site.commons.domain

  val ukWiki: String = Site.ukWiki.domain

}


