package ui

import concurrent.Future
import concurrent.ExecutionContext.Implicits.global

import play.api.templates.Html

object Pagelet {

  def render(html: Html, id: String): Html = {
    views.html.pagelet(html, id)
  }

  def renderStream(html: Html, id: String): HtmlStream = {
    HtmlStream(render(html, id))
  }

  def renderStream(htmlFuture: Future[Html], id: String): HtmlStream = {
    HtmlStream.flatten(htmlFuture.map(html => renderStream(html, id)))
  }
}
