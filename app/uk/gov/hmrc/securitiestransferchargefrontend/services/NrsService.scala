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
import uk.gov.hmrc.securitiestransferchargefrontend.models.nrs.{NrsMetadata, NrsSingleSubmissionRequest}
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.*
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.CyaHtmlData
import uk.gov.hmrc.securitiestransferchargefrontend.utils.CommonHelpers.authToken

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future


trait NrsService:
  def singleSubmissionNotableEvent(cyaHtml     : CyaHtmlData,
                                   affinityData: AffinityData,
                                   utrn        : String
                                  )(using
                                    request    : StcDataRequest[?],
                                    hc         : HeaderCarrier): Future[Unit]

class NrsServiceImpl @Inject()(
  nrsClient: NrsClient,
  config: FrontendAppConfig
) extends NrsService {

  override def singleSubmissionNotableEvent(
    cyaHtml     : CyaHtmlData,
    affinityData: AffinityData,
    utrn        : String
  )(using
    request     : StcDataRequest[?],
    hc          : HeaderCarrier): Future[Unit] = {

    val htmlPayload = cyaHtml.html.toString
    val nrsRequest = NrsSingleSubmissionRequest(
      payload  = htmlPayload,
      metadata = NrsMetadata(
        businessId              = config.nrsBusinessId,
        notableEvent            = config.nrsNotableEventSingleSubmission,
        payloadContentType      = MimeTypes.HTML,
        payloadSha256Checksum   = sha256Hex(htmlPayload),
        userSubmissionTimestamp = LocalDate.now().format(DateTimeFormatter.ISO_DATE_TIME),
        identityData            = request.request.identityData,
        userAuthToken           = authToken(hc),
        headerData              = request.headers.toSimpleMap,
        searchKeys              = searchKeys(request, affinityData, utrn)
      )
    )

    nrsClient.postSinglePayload(nrsRequest)
  }

  private val taxIdentifier: PartialFunction[AffinityData, (String, String)] = {
    case i: Individual   => NrsSearchKeys.Nino -> i.nino
    case o: Organisation => NrsSearchKeys.Utr  -> o.utr
  }

  private val agentTaxIdentifier: StcDataRequest[?] => Option[(String, String)] = req => {
    req
      .request
      .maybeArn
      .map(arn => NrsSearchKeys.Arn -> arn)
  }

  private def searchKeys(request: StcDataRequest[?], affinityData: AffinityData, utrn: String): Map[String, String] = {
    val pairs = ListBuffer.empty[(String, String)]
    pairs.addOne(NrsSearchKeys.SubscriptionId -> request.request.subscriptionId.value)
    pairs.addOne(NrsSearchKeys.SubmissionId -> request.userAnswers.submissionId.value)
    pairs.addOne(NrsSearchKeys.Utrn -> utrn)

    val taxId = taxIdentifier.lift(affinityData).orElse(agentTaxIdentifier(request))
    taxId.foreach(pair => pairs.addOne(pair))

    pairs.toMap
  }

}

object NrsSearchKeys:
  val SubscriptionId = "sttId"
  val Nino = "NINO"
  val Utr = "UTR"
  val Arn = "ARN"
  val SubmissionId = "submissionId"
  val Utrn = "transferRef"
