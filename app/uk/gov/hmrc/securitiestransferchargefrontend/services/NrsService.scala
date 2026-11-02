/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.securitiestransferchargefrontend.services

import org.apache.commons.codec.digest.DigestUtils.sha256Hex
import play.api.http.MimeTypes
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.registration.NrsClient
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.*
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.nrs.{NrsMetadata, NrsSingleSubmissionRequest}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.CyaHtmlData
import uk.gov.hmrc.securitiestransferchargefrontend.utils.CommonHelpers.authToken

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import javax.inject.Inject
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

trait NrsService:
  def singleSubmissionNotableEvent(
    cyaHtml     : CyaHtmlData,
    utrn        : String
  )(implicit
    request    : StcDataRequest[?],
    hc         : HeaderCarrier): Future[Unit]

  def bulkSubmissionNotableEvent(
    submissionId: SubmissionId,
    uploadRef   : String
  )(implicit hc : HeaderCarrier): Future[Unit]

class NrsServiceImpl @Inject()(
  nrsClient: NrsClient,
  config: FrontendAppConfig
) extends NrsService {

  override def singleSubmissionNotableEvent(
    cyaHtml     : CyaHtmlData,
    utrn        : String
  )(implicit
    request     : StcDataRequest[?],
    hc          : HeaderCarrier): Future[Unit] = {

    val htmlPayload = cyaHtml.html.toString
    val checksum = sha256Hex(htmlPayload)
    val encodedPayload: String = Base64.getEncoder.encodeToString(htmlPayload.getBytes(StandardCharsets.UTF_8))

    val nrsRequest = NrsSingleSubmissionRequest(
      payload  = encodedPayload,
      metadata = createMetadata(checksum, config.nrsNotableEventSingleSubmission, request, hc, utrn)
    )

    nrsClient.postSinglePayload(nrsRequest)
  }

  override def bulkSubmissionNotableEvent(submissionId: SubmissionId, uploadRef: String)
                                         (implicit hc: HeaderCarrier): Future[Unit] = {
    // TODO: Implementation
    Future.successful(())
  }

  private[services] def createMetadata(
    checksum    : String,
    notableEvent: String,
    request     : StcDataRequest[?],
    hc          : HeaderCarrier,
    utrn        : String
  ) = {
    NrsMetadata(
      businessId              = config.nrsBusinessId,
      notableEvent            = notableEvent,
      payloadContentType      = MimeTypes.HTML,
      payloadSha256Checksum   = checksum,
      userSubmissionTimestamp = Instant.now().toString,
      identityData            = request.request.identityData,
      userAuthToken           = authToken(hc),
      headerData              = request.headers.toSimpleMap,
      searchKeys              = searchKeys(request, utrn)
    )
  }

  private val taxIdentifier: PartialFunction[UniqueIdentifier, (String, String)] = {
    case AgentUniqueIdentifier(Some(arn))        => NrsSearchKeys.Arn -> arn
    case OrganisationUniqueIdentifier(Some(utr)) => NrsSearchKeys.Utr -> utr
    case IndividualUniqueIdentifier(nino)        => NrsSearchKeys.Nino -> nino
  }

  private[services] def searchKeys(request: StcDataRequest[?], utrn: String): Map[String, String] = {
    val pairs = ListBuffer.empty[(String, String)]
    pairs.addOne(NrsSearchKeys.SubscriptionId -> request.request.subscriptionId.value)
    pairs.addOne(NrsSearchKeys.SubmissionId   -> request.userAnswers.submissionId.value)
    pairs.addOne(NrsSearchKeys.Utrn           -> utrn)

    taxIdentifier
      .lift(request.request.uniqueIdentifier)
      .foreach(pair => pairs.addOne(pair))

    pairs.toMap
  }

}

object NrsSearchKeys:
  val SubscriptionId  = "sttId"
  val Nino            = "NINO"
  val Utr             = "UTR"
  val Arn             = "ARN"
  val SubmissionId    = "submissionID"
  val Utrn            = "transferRef"
