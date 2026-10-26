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
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SaveAndReturnClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.StcDataRequest
import uk.gov.hmrc.securitiestransferchargefrontend.domain.TransferType.*
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{CredentialId, SubmissionId, SubscriptionId, TransferType}
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.models.audit.JourneyStatus.{SubmissionFailure, SubmissionSuccess}
import uk.gov.hmrc.securitiestransferchargefrontend.models.audit.{AuditModel, AuditType}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.Address
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.{AffinityData, Agent, Individual, Organisation}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.{CyaHtmlRepository, TransactionResponseRepository}
import uk.gov.hmrc.securitiestransferchargefrontend.services.TransactionSubmissionService.*
import uk.gov.hmrc.securitiestransferchargefrontend.utils.HeaderCarrierCreator

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait TransactionSubmissionService:
  def submitSingleStf(implicit request: StcDataRequest[?]): Future[Boolean]
  def submitSingleSh03(implicit request: StcDataRequest[?]): Future[Boolean]

final class TransactionSubmissionServiceImpl @Inject()(
  headerCarrierCreator          : HeaderCarrierCreator,
  etmpSubmissionService         : EtmpSubmissionService,
  saveAndReturnClient           : SaveAndReturnClient,
  nrsService                    : NrsService,
  cyaHtmlRepository             : CyaHtmlRepository,
  transactionResponseRepository : TransactionResponseRepository,
  auditService                  : AuditService)(
  implicit ec                   : ExecutionContext
) extends TransactionSubmissionService with Logging {

  override def submitSingleStf(using request: StcDataRequest[?]): Future[Boolean] =
    submitSingle(STF)

  override def submitSingleSh03(using request: StcDataRequest[?]): Future[Boolean] =
    submitSingle(SH03)

  private def submitSingle(transferType: TransferType)(using request: StcDataRequest[?]): Future[Boolean] = {
    implicit val hc: HeaderCarrier = headerCarrierCreator.create(request)
    val submissionId = request.userAnswers.submissionId
    for {
      response <- submitFunctionFor(transferType)(
        request.request.subscriptionId,
        request.userAnswers,
        getAffinityData(request.request.affinityGroup)
      )

      _ = response.fold(
        onSuccess        = handleSingleSuccess(transferType),
        onPartialFailure = handleSinglePartialFailure(transferType),
        onFailure        = handleSingleFailure(transferType, submissionId)
      )
    } yield response.isSuccessful
  }

  private type SubmitFunction = (SubscriptionId, UserAnswers, AffinityData) => Future[SubmissionCreateResponse]

  private val submitFunctionFor: TransferType => HeaderCarrier ?=> SubmitFunction = {
    case STF => etmpSubmissionService.submitSingleStf
    case _ => etmpSubmissionService.submitSingleSh03
  }

  private val extractComponentsFrom: StcDataRequest[?] => (SubscriptionId, AffinityGroup, CredentialId) = req =>
    val innerRequest = req.request
    (innerRequest.subscriptionId, innerRequest.affinityGroup, innerRequest.credentialId)

  private def handleSingleSuccess(transferType: TransferType)(successResponse: SubmissionCreateResponseSuccess)
                                 (implicit hc: HeaderCarrier, request: StcDataRequest[?]): Boolean = {
    val (subscriptionId, affinityGroup, credentialId) = extractComponentsFrom(request)
    val submissionId = successResponse.submissionId
    logger.info(s"Successful response from ETMP for ${transferType.toString} submission ${submissionId}")
    auditService.audit(AuditModel(SubmissionSuccess, subscriptionId, affinityGroup, credentialId, Some(submissionId), AuditType.fromTransferType(transferType)))
    transactionResponseRepository.store(submissionId, successResponse)
    saveAndReturnClient.deleteDraft(submissionId)
    sendSingleSubmissionDataToNRS(submissionId, successResponse.utrn)
    true
  }

  private def handleSinglePartialFailure(transferType: TransferType)(pf: SubmissionCreateResponsePartialFailure): Boolean = {
    logger.warn(s"Unexpected partial failure response for ${transferType.toString} submission ${pf.submissionId}: $pf")
    false
  }

  private def handleSingleFailure(transferType: TransferType, submissionId: SubmissionId)(implicit request: StcDataRequest[?], headerCarrier: HeaderCarrier): Boolean = {
    logger.warn(s"Failure response from ETMP for ${transferType.toString} submission $submissionId")
    val (subscriptionId, affinityGroup, credentialId) = extractComponentsFrom(request)
    auditService.audit(AuditModel(SubmissionFailure, subscriptionId, affinityGroup, credentialId, Some(request.userAnswers.submissionId), AuditType.fromTransferType(transferType)))
    false
  }

  private def sendSingleSubmissionDataToNRS(submissionId: SubmissionId, utrn: String)
                                           (implicit request: StcDataRequest[?], hc: HeaderCarrier): Future[Unit] = {
    cyaHtmlRepository
      .retrieve(submissionId)
      .map {
        case Some(data) => nrsService.singleSubmissionNotableEvent(data, utrn)
        case None       => logger.warn(s"Nothing to send to NRS for submission $submissionId - no data in CYA repository.")
      }
  }

}

object TransactionSubmissionService {
  val clearedUserAnswers: StcDataRequest[?] => UserAnswers = { req =>
    val currentUserAnswers = req.userAnswers
    UserAnswers.empty(currentUserAnswers.userId)(currentUserAnswers.groupIdentifier)(currentUserAnswers.submissionId)(currentUserAnswers.journeyType)
  }

  private[services] val getAffinityData: AffinityGroup => AffinityData = {
    case AffinityGroup.Agent => getAgentAffinityData
    case AffinityGroup.Individual => getIndividualAffinityData
    case AffinityGroup.Organisation => getOrganisationAffinityData
  }

  // TODO: Some of this should come from new screens not yet developed.
  private[services] val getIndividualAffinityData: AffinityData =
    Individual(
      name = "John Smith",
      address = Address(
        addressLine1 = "10 High Street",
        addressLine2 = Some("Bolton"),
        addressLine3 = None,
        postcode = "BL1 2TG",
        countryCode = "UK"
      ),
      email = "jsmith@foo.com",
      nino = "NX787356B"
    )

  private[services] val getAgentAffinityData: AffinityData =
    Agent(
      name = "Agent 1",
      address = Address(
        addressLine1 = "10 High Street",
        addressLine2 = Some("Bolton"),
        addressLine3 = None,
        postcode = "BL1 2TG",
        countryCode = "UK"
      ),
      phone = "07777777777",
      email = "someemal@foo.com"
    )

  private[services] val getOrganisationAffinityData: AffinityData =
    Organisation(
      name = "Business",
      address = Address(
        addressLine1 = "10 High Street",
        addressLine2 = Some("Bolton"),
        addressLine3 = None,
        postcode = "BL1 2TG",
        countryCode = "UK"
      ),
      email = "someemal@foo.com",
      utr = "some utr" // retrieve from Auth
    )
}
