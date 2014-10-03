package controllers

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Promise
import play.api.libs.iteratee.Enumerator
import play.api.templates.HtmlFormat

import ui.HtmlStreamImplicits._
import ui.{Pagelet, HtmlStream}

object CartService {
  val delay = 500
  def cart = Promise.timeout(s"5 items in your cart ($delay ms to load)", delay)
}

object WaitlistService {
  val delay = 3000
  def waitlist = Promise.timeout(s"10 items on your waitlist ($delay ms to load)", delay)
}

object MainController extends Controller {

  def index = Action.async {
    val cartF: Future[String] = CartService.cart
    val waitlistF: Future[String] = WaitlistService.waitlist

    for {
      cart <- cartF
      waitlist <- waitlistF
    } yield Ok(views.html.index(views.html.cart(cart), views.html.waitlist(waitlist)))
  }

  def helloEnumerator = Action {
    Ok.chunked(Enumerator("Hello\n", "world\n", "enumerator\n"))
  }

  def repeat = Action {
    Ok.chunked(Enumerator.repeatM(Promise.timeout("Hello\n", 200)))
  }

  def enumerators = Action {
    val hello = Enumerator.repeatM(Promise.timeout("Hello\n", 200))
    val goodbye = Enumerator.repeatM(Promise.timeout("goodbye\n", 1000))

    Ok.chunked(hello.interleave(goodbye))
  }

  def andThen = Action {

    val cartF: Future[String] = CartService.cart
    val waitlistF: Future[String] = WaitlistService.waitlist

    val cartStream = HtmlStream(cartF.map(s => views.html.cart(s)))
    val waitlistStream = HtmlStream(waitlistF.map(s => views.html.waitlist(s)))

    val body = cartStream.andThen(waitlistStream)

    Ok.chunked(views.stream.index(body))
  }

  def interleaved = Action {

    val cartF: Future[String] = CartService.cart
    val waitlistF: Future[String] = WaitlistService.waitlist

    val cartS = HtmlStream(cartF.map(s => views.html.cart(s)))
    val waitlistS = HtmlStream(waitlistF.map(s => views.html.waitlist(s)))

    val body = HtmlStream.interleave(cartS, waitlistS)

    Ok.chunked(views.stream.index(body))
  }

  def full = Action {

    val cartF: Future[String] = CartService.cart
    val waitlistF: Future[String] = WaitlistService.waitlist

    val cartHtml: Future[HtmlFormat.Appendable] = cartF.map(s => views.html.cart(s))
    val waitlistHtml: Future[HtmlFormat.Appendable] = waitlistF.map(s => views.html.waitlist(s))

    val cartS: HtmlStream = Pagelet.renderStream(cartHtml, "cart")
    val waitlistS: HtmlStream = Pagelet.renderStream(waitlistHtml, "waitlist")

    val body: HtmlStream = HtmlStream.interleave(cartS, waitlistS)

    Ok.chunked(views.stream.full(body))
  }
}