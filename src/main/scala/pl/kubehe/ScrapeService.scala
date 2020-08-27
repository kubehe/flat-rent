package pl.kubehe

import java.util

import net.ruippeixotog.scalascraper.model.Document
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import org.jboss.logging.Logger
import org.jsoup.nodes.DocumentType

import scala.collection.JavaConverters._
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.jackson.ElasticJackson.Implicits._
import javax.enterprise.context.ApplicationScoped

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

case class Flat(url: String, location: String, pricePerMonth: Double, details: Map[String, String], description: String, additionalInformation: List[String])

@ApplicationScoped
class ScrapeService {
  final val log: Logger = Logger.getLogger(classOf[ScrapeController])
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  def startScraping(): Future[util.List[Flat]] = {
    Future {

    val browser = JsoupBrowser()
    val page1 = browser.get("https://www.otodom.pl/wynajem/mieszkanie/warszawa/?search%5Bcity_id%5D=26&nrAdsPerPage=72&page=1")
    val pagination = page1 >> element(".pager") >> elementList("a")
    log.info(pagination)

    val lastPage = pagination.map(page => {
      val value = try {
        (page >> allText).toInt
      } catch {
        case _: Throwable => 0
      }
      log.info(value)
      value
    }).max

    log.info(lastPage)

    val list = (1 until lastPage).flatMap(pageNumber => {
      log.info(pageNumber)
      try {
        val page = browser.get(s"https://www.otodom.pl/wynajem/mieszkanie/warszawa/?search%5Bcity_id%5D=26&nrAdsPerPage=72&page=$pageNumber")
        page >> elementList("article")
      } catch {
        case e: Throwable =>
          log.info(e.getMessage)
          List()
      }
    }).map(article => article >> attr("data-url"))

      .map(flatUrl => {
        try {
          val flat = browser.get(flatUrl)
          Flat(
            url = flatUrl,
            location = flat >> ".css-12hd9gg" >> allText,
            pricePerMonth = (flat >> ".css-1vr19r7" >> allText).split("zł").head.replaceAll("\\s", "").toDouble,
            details = (flat >> ".css-1ci0qpi" >> elementList("li")).map(element => {
              val detail = (element >> allText).split(":")
              (detail(0).trim, detail(1).trim)
            }).toMap,
            description = flat >> ".css-1bi3ib9" >> allText,
            additionalInformation = flat >> ".css-1bpegon" >> elementList("li") >> allText
          )
        } catch {
          case e: Throwable =>
            log.info(e.getMessage)
            Flat(flatUrl, null, 0, Map(), null, List())
        }
      }).toList


    val client = ElasticClient(JavaClient(ElasticProperties("http://localhost:9200")))

    client.execute {
      deleteIndex("flatrent")
    }

    client.execute {
      bulk(
        list.map(flat => indexInto("flatrent").id(flat.url).doc(flat))

      ).refresh(RefreshPolicy.WaitFor)
    }.await

    client.close()

    list.asJava
  }
  }

}
