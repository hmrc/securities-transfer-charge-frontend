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
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.StcDataRequest
import uk.gov.hmrc.securitiestransferchargefrontend.models.nrs.{IdentityData, NrsMetadata, NrsSingleSubmissionRequest}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.CyaHtmlData
import uk.gov.hmrc.securitiestransferchargefrontend.utils.CommonHelpers.authToken

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import java.util.Base64
import java.nio.charset.StandardCharsets

trait NrsService:
  def singleSubmissionNotableEvent(
    cyaHtml     : CyaHtmlData,
    utrn        : String
  )(implicit
    request    : StcDataRequest[?],
    hc         : HeaderCarrier): Future[Unit]

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
      userSubmissionTimestamp = LocalDate.now().format(DateTimeFormatter.ISO_DATE_TIME),
      identityData            = request.request.identityData,
      userAuthToken           = authToken(hc),
      headerData              = request.headers.toSimpleMap,
      searchKeys              = searchKeys(request, request.request.identityData, utrn)
    )
  }

  private val taxIdentifier: PartialFunction[IdentityData, (String, String)] = {
    Function.unlift { (data: IdentityData) =>
      data.nino.map(NrsSearchKeys.Nino -> _) orElse
        data.saUtr.map(NrsSearchKeys.Utr -> _)
    }
  }

  private val agentTaxIdentifier: StcDataRequest[?] => Option[(String, String)] = req => {
    req
      .request
      .maybeArn
      .map(arn => NrsSearchKeys.Arn -> arn)
  }

  private[services] def searchKeys(request: StcDataRequest[?], identityData: IdentityData, utrn: String): Map[String, String] = {
    val pairs = ListBuffer.empty[(String, String)]
    pairs.addOne(NrsSearchKeys.SubscriptionId -> request.request.subscriptionId.value)
    pairs.addOne(NrsSearchKeys.SubmissionId   -> request.userAnswers.submissionId.value)
    pairs.addOne(NrsSearchKeys.Utrn           -> utrn)

    val taxId = taxIdentifier.lift(identityData).orElse(agentTaxIdentifier(request))
    taxId.foreach(pair => pairs.addOne(pair))

    pairs.toMap
  }

}

object NrsSearchKeys:
  val SubscriptionId  = "sttId"
  val Nino            = "NINO"
  val Utr             = "UTR"
  val Arn             = "ARN"
  val SubmissionId    = "submissionId"
  val Utrn            = "transferRef"
