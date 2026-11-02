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
import uk.gov.hmrc.securitiestransferchargefrontend.domain.TransferType.{SH03, STF}
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{SubmissionId, SubscriptionId, TransferType}
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.bulk.CompanyDetails
import uk.gov.hmrc.securitiestransferchargefrontend.models.shared.AgentReference
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedStcRow, ValidatedStcRow}
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.RoleAtPurchasingCompanyPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.bulk.BulkCompanyDetailsPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.bulk.BulkAgentReferencePage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.AgentReferencePage

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait EtmpSubmissionService:
  def submitSingleStf(subscriptionId: SubscriptionId, userAnswers: UserAnswers, affinityData: AffinityData)(implicit hc: HeaderCarrier): Future[SubmissionCreateResponse]
  def submitSingleSh03(subscriptionId: SubscriptionId, userAnswers: UserAnswers, affinityData: AffinityData)(implicit hc: HeaderCarrier): Future[SubmissionCreateResponse]
  def submitBulkStf(subscriptionId: SubscriptionId, rows: Seq[ValidatedStcRow], userAnswers: UserAnswers, affinityData: AffinityData)(implicit hc: HeaderCarrier): Future[SubmissionCreateResponse]
  def submitBulkSH03(subscriptionId: SubscriptionId, rows: Seq[ValidatedStcRow], userAnswers: UserAnswers, affinityData: AffinityData)(implicit hc: HeaderCarrier): Future[SubmissionCreateResponse]


class EtmpSubmissionServiceImpl @Inject() (etmpSubmissionsClient: EtmpSubmissionClient)(implicit ec: ExecutionContext) extends EtmpSubmissionService with Logging:
  override def submitSingleStf (subscriptionId: SubscriptionId, userAnswers: UserAnswers, affinityData: AffinityData)
                               (implicit hc: HeaderCarrier): Future[SubmissionCreateResponse] = {
    userAnswers.get(StfTransaction).map { stfSingleReq =>
      val stfReq = UserAnswersTransforms.toStfRequest(stfSingleReq, affinityData)
      submitSingle(stfReq, subscriptionId, userAnswers)
    }.getOrElse(handleSubmitSingleFailure(STF, userAnswers.submissionId))
  }

  override def submitSingleSh03(subscriptionId: SubscriptionId, userAnswers: UserAnswers, affinityData: AffinityData)
                               (implicit hc: HeaderCarrier): Future[SubmissionCreateResponse] = {
    userAnswers.get(Sh03Transaction).map { sh03SingleReq =>
      val sh03Req = UserAnswersTransforms.toSh03Request(sh03SingleReq, affinityData)
      submitSingle(sh03Req, subscriptionId, userAnswers)
    }.getOrElse(handleSubmitSingleFailure(SH03, userAnswers.submissionId))
  }

  private def submitSingle(req: SingleTransferRequest, subscriptionId: SubscriptionId, userAnswers: UserAnswers)
                  (implicit hc: HeaderCarrier): Future[SubmissionCreateResponse] = {
    val declaration = createDeclaration(userAnswers)
    val etmpPayload = SubmissionBatchPayload(declaration, List(req))
    submitBatchTransfer(subscriptionId, userAnswers, etmpPayload)
  }

  private val submissionFailure = Future.successful(SubmissionCreateResponseFailure)
  
  private def handleSubmitSingleFailure(transferType: TransferType, submissionId: SubmissionId): Future[SubmissionCreateResponse] = {
    logger.warn(s"Failed to submit single ${transferType.toString} transaction $submissionId - could not parse user answers.")
    submissionFailure
  }
  
  override def submitBulkStf(subscriptionId: SubscriptionId, rows: Seq[ValidatedStcRow], userAnswers: UserAnswers, affinityData: AffinityData)
                            (implicit hc: HeaderCarrier): Future[SubmissionCreateResponse] =
    submitBulk(RowTransforms.fromValidatedStcRowToStfRequest)(subscriptionId, rows, userAnswers, affinityData)

  override def submitBulkSH03(subscriptionId: SubscriptionId, rows: Seq[ValidatedStcRow], userAnswers: UserAnswers, affinityData: AffinityData)
                             (implicit hc: HeaderCarrier): Future[SubmissionCreateResponse] = {
    getCompanyDetails(userAnswers).map { companyDetails =>
      submitBulk(RowTransforms.fromValidatedStcRowToSh03Request(companyDetails))(subscriptionId, rows, userAnswers, affinityData)
    }.getOrElse {
      logger.warn(s"Failed to submit bulks SH03 transaction ${userAnswers.submissionId} - could not get company details.")
      submissionFailure
    }
  }

  private type TransformFunction = (ParsedStcRow, AffinityData, Option[String]) =>  SingleTransferRequest
  private def submitBulk(transformFunction: TransformFunction)(subscriptionId: SubscriptionId, rows: Seq[ValidatedStcRow], userAnswers: UserAnswers, affinityData: AffinityData)
                        (implicit hc: HeaderCarrier): Future[SubmissionCreateResponse] = {
    val agentReference = getAgentReference(userAnswers)
    val requests = rows.map { row => transformFunction(row.parsedRow, affinityData, agentReference) }
    val declaration = createDeclaration(userAnswers)
    val etmpPayload = SubmissionBatchPayload(declaration, requests)
    submitBatchTransfer(subscriptionId, userAnswers, etmpPayload)
  }
 
  private def getAgentReference(userAnswers: UserAnswers): Option[String] =
    (userAnswers.data \ BulkAgentReferencePage.toString).asOpt[String]

  private def getCompanyDetails(userAnswers: UserAnswers): Option[CompanyDetails] = {
    (userAnswers.data \ BulkCompanyDetailsPage.toString).asOpt[CompanyDetails]
  }
    
  private val formatter  = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.UK)

  private val toSubmissionCreateResponse: UserAnswers => StcTransactionCreateResponse => SubmissionCreateResponse = userAnswers => {
    case StcTransactionCreateProcessed(body) => toSubmissionCreateResponseSuccess(userAnswers, body)
    case _                                   => SubmissionCreateResponseFailure
  }

  private def toSubmissionCreateResponseSuccess(userAnswers: UserAnswers, processed: StcTransactionCreateProcessedBody): SubmissionCreateResponse = {
    val charges   = processed.charges
    val successes = charges.collect(getStcChargeSuccess)
    val failures  = charges.collect(getStcChargeFailure)

    lazy val failedRecords = failures.map(_.recordId)

    if successes.isEmpty then
      return SubmissionCreateResponseFailure
    end if

    val submissionId     = userAnswers.submissionId
    val utrn             = successes.head.utrn
    val chargeReferences = successes.map(_.chargeReference)
    val taxDue           = successes.map(_.chargeAmount).sum
    val paymentDueBy     = successes.collect(getDueBy).min
    val agentReference   = userAnswers.get(AgentReferencePage).flatMap(_.agentReference)

    if failures.isEmpty then
      SubmissionCreateResponseSuccess(submissionId, utrn, chargeReferences, taxDue, paymentDueBy, agentReference)
    else
      SubmissionCreateResponsePartialFailure(submissionId, chargeReferences, failedRecords, taxDue, paymentDueBy, agentReference)
  }

  private val getStcChargeFailure: PartialFunction[StcCharge, StcChargeFailure] = {
    case c: StcChargeFailure => c
  }

  private val getStcChargeSuccess: PartialFunction[StcCharge, StcChargeSuccess] = {
    case c: StcChargeSuccess => c
  }

  private val getDueBy: PartialFunction[StcCharge, LocalDate] = {
    case c: StcChargeSuccess => LocalDate.parse(c.chargeDueDate, formatter)
  }

  // ToDo: We do not currently have the information to create a declaration.
  private val createDeclaration: UserAnswers => SingleTransferDeclaration = userAnswers =>
    SingleTransferDeclaration(
      userAnswers.get(RoleAtPurchasingCompanyPage).map(_.role).flatMap(DeclarationRole.fromString),
      userAnswers.get(RoleAtPurchasingCompanyPage).flatMap(_.uksOrgan),
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

  private def submitBatchTransfer(subscriptionId: SubscriptionId, userAnswers: UserAnswers, etmpPayload: SubmissionBatchPayload)(implicit hc: HeaderCarrier): Future[SubmissionCreateResponse] =
    etmpSubmissionsClient
      .submitBatchTransfer(subscriptionId, userAnswers.submissionId, etmpPayload)
      .map(toSubmissionCreateResponse(userAnswers))
