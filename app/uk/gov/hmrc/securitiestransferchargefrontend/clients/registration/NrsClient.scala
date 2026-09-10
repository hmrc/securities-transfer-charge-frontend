package uk.gov.hmrc.securitiestransferchargefrontend.clients.registration

import play.twirl.api.HtmlFormat

import javax.inject.Inject
import scala.concurrent.Future
// TODO: This class needs implementing as part of the NRS ticket.
// TODO: It will need to collect the metadata too.

trait NrsClient:
  def postHtmlPayload(html: HtmlFormat.Appendable): Future[Unit]

final class NrsClientImpl @Inject() extends NrsClient:
  override def postHtmlPayload(html: HtmlFormat.Appendable): Future[Unit] =
    Future.successful(())
