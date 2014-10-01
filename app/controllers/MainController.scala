package controllers

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Promise
import play.api.templates.HtmlFormat

import ui.HtmlStreamImplicits._
import ui.{Pagelet, HtmlStream}

object CartService {
  def cart = Promise.timeout("5 items in your cart", 3000)
}

object WaitlistService {
  def waitlist = Promise.timeout("10 items on your waitlist", 6000)
}

object MainController extends Controller {

  def index = Action.async {
    val cartF: Future[String] = CartService.cart
    val waitlistF: Future[String] = WaitlistService.waitlist

    for {
      cart <- cartF
      waitlist <- waitlistF
    } yield Ok(views.html.index(cart, waitlist))
  }

  def andThen = Action {

    val cartF: Future[String] = CartService.cart
    val waitlistF: Future[String] = WaitlistService.waitlist

    val cartStream = HtmlStream(cartF.map(s => views.html.partial(s)))
    val waitlistStream = HtmlStream(waitlistF.map(s => views.html.partial(s)))

    val body = cartStream.andThen(waitlistStream)

    Ok.chunked(views.stream.index(body))
  }

  def interleaved = Action {

    val cartF: Future[String] = CartService.cart
    val waitlistF: Future[String] = WaitlistService.waitlist

    val cartS = HtmlStream(cartF.map(s => views.html.partial(s)))
    val waitlistS = HtmlStream(waitlistF.map(s => views.html.partial(s)))

    val body = HtmlStream.interleave(cartS, waitlistS)

    Ok.chunked(views.stream.index(body))
  }

  def full = Action {

    val cartF: Future[String] = CartService.cart
    val waitlistF: Future[String] = WaitlistService.waitlist

    val cartHtml: Future[HtmlFormat.Appendable] = cartF.map(s => views.html.partial(s))
    val waitlistHtml: Future[HtmlFormat.Appendable] = waitlistF.map(s => views.html.partial(s))

    val cartS: HtmlStream = Pagelet.renderStream(cartHtml, "cart")
    val waitlistS: HtmlStream = Pagelet.renderStream(waitlistHtml, "waitlist")

    val body: HtmlStream = HtmlStream.interleave(cartS, waitlistS)

    Ok.chunked(views.stream.index(body))
  }
}