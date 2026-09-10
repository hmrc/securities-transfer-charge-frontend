package clients

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.EtmpSubmissionClient
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{SubmissionId, SubscriptionId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.{StcTransactionCreateResponse, SubmissionBatchPayload}

import scala.concurrent.Future

class FakeEtmpSubmissionClient(toReturn: StcTransactionCreateResponse) extends EtmpSubmissionClient:
  def submitSingleStf(
                       subscriptionId: SubscriptionId,
                       submissionId: SubmissionId,
                       payload: SubmissionBatchPayload
                     )(implicit hc: HeaderCarrier): Future[StcTransactionCreateResponse] = Future.successful(toReturn)
