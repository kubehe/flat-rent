package pl.kubehe

import net.ruippeixotog.scalascraper.model.Document
import javax.ws.rs.core.MediaType
import javax.ws.rs.{GET, Path, Produces}
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
import javax.inject.Inject

@Path("/otodom")
class ScrapeController {

  final val log: Logger = Logger.getLogger(classOf[ScrapeController])

  @Inject
  var scrapeService: ScrapeService = _

  @GET
  @Produces(Array[String](MediaType.APPLICATION_JSON))
  def startScraping(): String = {

    scrapeService.startScraping()

    "started"
  }


}


