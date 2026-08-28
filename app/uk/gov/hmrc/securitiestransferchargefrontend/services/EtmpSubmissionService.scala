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

import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.EtmpSubmissionClient
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.models.shared.AgentReference
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.DeclarationRole.Director
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.AgentReferencePage

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

type ChargeReference = String

trait SubmissionCreateResponse

final case class SubmissionCreateResponseSuccess(
  submissionId: SubmissionId,
  chargeReferences: Seq[ChargeReference],
  taxDue: BigDecimal,
  paymentDueBy: LocalDate,
  agentReference: Option[String]) extends SubmissionCreateResponse

case object SubmissionCreateResponseFailure extends SubmissionCreateResponse

val submissionFailure = Future.successful(SubmissionCreateResponseFailure)

trait EtmpSubmissionService:
  def submitSingleStf(userAnswers: UserAnswers, affinityData: AffinityData)(implicit hc: HeaderCarrier): Future[SubmissionCreateResponse]

class EtmpSubmissionServiceImpl @Inject() (etmpSubmissionsClient: EtmpSubmissionClient)(implicit ec: ExecutionContext) extends EtmpSubmissionService with Logging:

  private val formatter  = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.UK)

  private val toSubmissionCreateResponse: UserAnswers => StcTransactionCreateResponse => SubmissionCreateResponse = userAnswers => {
    case StcTransactionCreateProcessed(body) => toSubmissionCreateResponseSuccess(userAnswers, body)
    case _                                   => SubmissionCreateResponseFailure
  }

  private def toSubmissionCreateResponseSuccess(userAnswers: UserAnswers, processed: StcTransactionCreateProcessedBody): SubmissionCreateResponse = {
    val charges = processed.charges.collect(getStcChargeSuccess)
    if charges.length != processed.charges.length then
      logger.warn(s"${userAnswers.submissionId}: Non-success charges in successful ETMP response")
      return SubmissionCreateResponseFailure

    SubmissionCreateResponseSuccess(
      submissionId     = userAnswers.submissionId,
      chargeReferences = charges.map(_.chargeReference),
      taxDue           = charges.map(_.chargeAmount).sum,
      paymentDueBy     = charges.collect(getDueBy).min.plusDays(30),
      agentReference   = userAnswers.get(AgentReferencePage).flatMap(_.agentReference)
    )
  }

  private val getStcChargeSuccess: PartialFunction[StcCharge, StcChargeSuccess] = {
    case c: StcChargeSuccess => c
  }

  private val getDueBy: PartialFunction[StcCharge, LocalDate] = {
    case c: StcChargeSuccess => LocalDate.parse(c.chargeDueDate, formatter)
  }

  // ToDo: We do not currently have the information to create a delaration.
  private val createDeclaration: UserAnswers => SingleTransferDeclaration = _ =>
    SingleTransferDeclaration(
      Some(Director),
      None,
      "John Bull",
      "10 High Street",
      Some("Bolton"),
      None,
      None,
      "BL2 5TT",
      "UK",
      None,
      true
    )

  def submitSingleStf(userAnswers: UserAnswers, affinityData: AffinityData)(implicit hc: HeaderCarrier): Future[SubmissionCreateResponse] = {
    userAnswers.get(StfTransaction).map { stfSingleReq =>
      val stfReq = UserAnswersTransforms.toStfRequest(stfSingleReq, affinityData)
      val declaration = createDeclaration(userAnswers)
      val etmpPayload = SubmissionBatchPayload(declaration, List(stfReq))

      etmpSubmissionsClient
        .singleStfSubmission(etmpPayload)
        .map(toSubmissionCreateResponse(userAnswers))
    }.getOrElse(submissionFailure)
  }
