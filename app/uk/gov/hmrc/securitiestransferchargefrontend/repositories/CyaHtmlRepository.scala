package uk.gov.hmrc.securitiestransferchargefrontend.repositories

import play.twirl.api.HtmlFormat
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId

import javax.inject.Inject
import scala.concurrent.Future

trait CyaHtmlRepository:
  def store(key: SubmissionId, value: HtmlFormat.Appendable): Future[Unit]
  def retrieve(key: SubmissionId): Future[HtmlFormat.Appendable]

// TODO: This needs to be implemented as part of the first NRS ticket.  
final class CyaHtmlRepositoryImpl @Inject() extends CyaHtmlRepository:

  def store(key: SubmissionId, value: HtmlFormat.Appendable): Future[Unit] = 
    Future.successful(())

  def retrieve(key: SubmissionId): Future[HtmlFormat.Appendable] =
    Future.successful(
      HtmlFormat.empty
    )
  