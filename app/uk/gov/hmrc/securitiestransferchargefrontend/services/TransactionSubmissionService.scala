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
import uk.gov.hmrc.securitiestransferchargefrontend.clients.registration.Subscription
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.{StcDataRequest, UniqueIdentifier}
import uk.gov.hmrc.securitiestransferchargefrontend.domain.TransferType.*
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{CredentialId, GroupIdentifier, SubmissionId, SubscriptionId, TransferType}
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.models.audit.JourneyStatus.{SubmissionFailure, SubmissionSuccess}
import uk.gov.hmrc.securitiestransferchargefrontend.models.audit.{AuditModel, AuditType}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.Address
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.ValidatedStcRow
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.{AffinityData, Agent, Individual, Organisation}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.{CyaHtmlRepository, TransactionResponseRepository}
import uk.gov.hmrc.securitiestransferchargefrontend.services.TransactionSubmissionService.*
import uk.gov.hmrc.securitiestransferchargefrontend.utils.HeaderCarrierCreator
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.*

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait TransactionSubmissionService:
  def submitSingleStf(implicit request: StcDataRequest[?]): Future[Boolean]
  def submitSingleSh03(implicit request: StcDataRequest[?]): Future[Boolean]
  def submitBulkStf(rows: Seq[ValidatedStcRow])(using request: StcDataRequest[?]): Future[Boolean]
  def submitBulkSh03(rows: Seq[ValidatedStcRow])(using request: StcDataRequest[?]): Future[Boolean]

final class TransactionSubmissionServiceImpl @Inject()(
  headerCarrierCreator          : HeaderCarrierCreator,
  etmpSubmissionService         : EtmpSubmissionService,
  saveAndReturnClient           : SaveAndReturnClient,
  nrsService                    : NrsService,
  cyaHtmlRepository             : CyaHtmlRepository,
  transactionResponseRepository : TransactionResponseRepository,
  bulkStatusService             : BulkStatusService,
  subscriptionService           : SubscriptionService,
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
    val submit = submitFunctionFor(transferType)
    for {
      subscription <- subscriptionService.getSubscription(request.request.subscriptionId)
      response <- submit(
        request.request.subscriptionId,
        request.userAnswers,
        getAffinityData(subscription)(request.request.affinityGroup)
      )

      _ = response.fold(
        onSuccess        = handleSingleSuccess(transferType),
        onPartialFailure = handleSinglePartialFailure(transferType),
        onFailure        = handleSingleFailure(transferType, submissionId)
      )
    } yield response.isSuccessful
  }

  override def submitBulkStf(rows: Seq[ValidatedStcRow])(using request: StcDataRequest[?]): Future[Boolean] = {
    submitBulk(STF, rows)
  }

  override def submitBulkSh03(rows: Seq[ValidatedStcRow])(using request: StcDataRequest[?]): Future[Boolean] =
    submitBulk(SH03, rows)

  private def submitBulk(transferType: TransferType, rows: Seq[ValidatedStcRow])(using request: StcDataRequest[?]): Future[Boolean] = {
    implicit val hc: HeaderCarrier = headerCarrierCreator.create(request)
    val submissionId = request.userAnswers.submissionId
    val submit = bulkSubmitFunctionFor(transferType)
    val group = GroupIdentifier(request.request.groupIdentifier)
    for {
      _ <- bulkStatusService.markAsProcessing(group, submissionId)
      subscription <- subscriptionService.getSubscription(request.request.subscriptionId)
      response <- submit(
        request.request.subscriptionId,
        rows,
        request.userAnswers,
        getAffinityData(subscription)(request.request.affinityGroup)
      )

      _ = response.fold(
        onSuccess        = handleBulkSuccess(transferType),
        onPartialFailure = handleBulkPartialFailure(transferType),
        onFailure        = handleBulkFailure(transferType, submissionId)
      )
    } yield response.isSuccessful
  }

  private type SubmitFunction = (SubscriptionId, UserAnswers, AffinityData) => Future[SubmissionCreateResponse]

  private val submitFunctionFor: TransferType => HeaderCarrier ?=> SubmitFunction = {
    case STF   => etmpSubmissionService.submitSingleStf
    case SH03  => etmpSubmissionService.submitSingleSh03
    case Other => throw new NotImplementedError("Transaction type Other is not supported.")
  }

  private type BulkSubmitFunction = (SubscriptionId, Seq[ValidatedStcRow], UserAnswers, AffinityData) => Future[SubmissionCreateResponse]

  private val bulkSubmitFunctionFor: TransferType => HeaderCarrier ?=> BulkSubmitFunction = {
    case STF   => etmpSubmissionService.submitBulkStf
    case SH03  => etmpSubmissionService.submitBulkSH03
    case Other => throw new NotImplementedError("Transaction type Other is not supported.")
  }

  private val extractComponentsFrom: StcDataRequest[?] => (SubscriptionId, AffinityGroup, CredentialId) = req => (
    req.request.subscriptionId,
    req.request.affinityGroup,
    req.request.credentialId
  )

  private def handleSingleSuccess(transferType: TransferType)(successResponse: SubmissionCreateResponseSuccess)
                                 (implicit hc: HeaderCarrier, request: StcDataRequest[?]): Unit = {
    val (subscriptionId, affinityGroup, credentialId) = extractComponentsFrom(request)
    val submissionId = successResponse.submissionId
    logger.info(s"Successful response from ETMP for ${transferType.toString} submission ${submissionId}")
    auditService.audit(AuditModel(SubmissionSuccess, subscriptionId, affinityGroup, credentialId, Some(submissionId), AuditType.fromTransferType(transferType)))
    transactionResponseRepository.store(submissionId, successResponse)
    saveAndReturnClient.deleteDraft(submissionId)
    sendSingleSubmissionDataToNRS(submissionId, successResponse.utrn)
  }

  private def handleSinglePartialFailure(transferType: TransferType)(pf: SubmissionCreateResponsePartialFailure): Unit = {
    logger.warn(s"Unexpected partial failure response for ${transferType.toString} submission ${pf.submissionId}: $pf")
  }

  private def handleSingleFailure(transferType: TransferType, submissionId: SubmissionId)(implicit request: StcDataRequest[?], headerCarrier: HeaderCarrier): Unit = {
    logger.warn(s"Failure response from ETMP for ${transferType.toString} submission $submissionId")
    val (subscriptionId, affinityGroup, credentialId) = extractComponentsFrom(request)
    auditService.audit(AuditModel(SubmissionFailure, subscriptionId, affinityGroup, credentialId, Some(request.userAnswers.submissionId), AuditType.fromTransferType(transferType)))
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

  private def handleBulkSuccess(transferType: TransferType)(successResponse: SubmissionCreateResponseSuccess)
                                 (implicit hc: HeaderCarrier, request: StcDataRequest[?]): Unit = {
    val (subscriptionId, affinityGroup, credentialId) = extractComponentsFrom(request)
    val submissionId = successResponse.submissionId
    logger.info(s"Successful response from ETMP for ${transferType.toString} bulk submission ${submissionId}")
    bulkStatusService.markAsSuccessful(submissionId)
    auditService.audit(AuditModel(SubmissionSuccess, subscriptionId, affinityGroup, credentialId, Some(submissionId), AuditType.fromTransferType(transferType)))
    saveAndReturnClient.deleteDraft(submissionId)
    sendBulkSubmissionDataToNRS(submissionId)
  }
  
  private def sendBulkSubmissionDataToNRS(submissionId: SubmissionId)(using request: StcDataRequest[?], hc: HeaderCarrier): Future[Unit] = {
    request.userAnswers.fileUploadReference.map { uploadRef =>
      nrsService.bulkSubmissionNotableEvent(submissionId, uploadRef)
    }.getOrElse {
      Future {
        logger.warn(s"Failed to send bulk data to NRS for submission $submissionId - no file reference found in user answers.")
      }
    }
  }

  private def handleBulkPartialFailure(transferType: TransferType)(pf: SubmissionCreateResponsePartialFailure)(using req: StcDataRequest[?]) : Future[Unit] = {
    val submissionId = pf.submissionId
    val failures = extractFailures(pf)
    logger.warn(s"Partial failure for ${transferType.toString} submission $submissionId - errors stored in bulk service status repository.")
    val group = GroupIdentifier(req.request.groupIdentifier)
    bulkStatusService.markAsPartiallyFailed(group, submissionId, failures)
  }

  private def extractFailures(pf: SubmissionCreateResponsePartialFailure): Map[Int, String] = {
    pf.failedRecords.map(_ -> "Failed").toMap // TODO: PF type needs error messages.
  }

  private def handleBulkFailure(transferType: TransferType, submissionId: SubmissionId)(using req: StcDataRequest[?]): Future[Unit] = Future {
    logger.warn(s"Submission failure for ${transferType.toString} submission ${submissionId}")
    val group = GroupIdentifier(req.request.groupIdentifier)
    bulkStatusService.markAsFailed(group, submissionId)
  }
}


object TransactionSubmissionService {
  val clearedUserAnswers: StcDataRequest[?] => UserAnswers = { req =>
    val currentUserAnswers = req.userAnswers
    UserAnswers.empty(currentUserAnswers.userId)(currentUserAnswers.groupIdentifier)(currentUserAnswers.submissionId)(currentUserAnswers.journeyType)
  }

  private[services] def getAffinityData(subscription: Subscription)(affinityGroup: AffinityGroup)(using req: StcDataRequest[?]): AffinityData = affinityGroup match {
    case AffinityGroup.Agent        => getAgentAffinityData(subscription)
    case AffinityGroup.Individual   => getIndividualAffinityData(subscription, getNino(req.request.uniqueIdentifier))
    case AffinityGroup.Organisation => getOrganisationAffinityData(subscription, getUtr(req.request.uniqueIdentifier))
  }

  val getNino: PartialFunction[UniqueIdentifier, String] = {
    case IndividualUniqueIdentifier(nino) => nino
  }

  val getUtr: PartialFunction[UniqueIdentifier, Option[String]] = {
    case OrganisationUniqueIdentifier(utr) => utr
  }
  
  private[services] def getIndividualAffinityData(sub: Subscription, nino: String): AffinityData =
    Individual(
      name = sub.contactName,
      address = Address(
        addressLine1 = sub.addressLine1,
        addressLine2 = sub.addressLine2,
        addressLine3 = sub.addressLine3,
        postcode = sub.postcode,
        countryCode = sub.countryCode
      ),
      email = sub.emailAddress,
      nino = nino
    )

  private[services] def getAgentAffinityData(sub: Subscription): AffinityData =
    Agent(
      name = sub.contactName,
      address = Address(
        addressLine1 = sub.addressLine1,
        addressLine2 = sub.addressLine2,
        addressLine3 = sub.addressLine3,
        postcode = sub.postcode,
        countryCode = sub.countryCode
      ),
      phone = sub.telephoneNumber,
      email = sub.emailAddress
    )

  private[services] def getOrganisationAffinityData(sub: Subscription, maybeUtr: Option[String]): AffinityData =
    Organisation(
      name = sub.contactName,
      address = Address(
        addressLine1 = sub.addressLine1,
        addressLine2 = sub.addressLine2,
        addressLine3 = sub.addressLine3,
        postcode = sub.postcode,
        countryCode = sub.countryCode
      ),
      email = sub.emailAddress,
      utr = maybeUtr
    )
}
