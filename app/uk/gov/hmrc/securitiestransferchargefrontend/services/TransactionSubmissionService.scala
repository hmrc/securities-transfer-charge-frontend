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

import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SaveAndReturnClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.StcDataRequest
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.models.audit.AuditModel
import uk.gov.hmrc.securitiestransferchargefrontend.models.audit.AuditType.Sh03
import uk.gov.hmrc.securitiestransferchargefrontend.models.audit.JourneyStatus.{SubmissionFailure, SubmissionSuccess}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.Address
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.{AffinityData, Agent, Individual, Organisation}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.{CyaHtmlRepository, TransactionResponseRepository}
import uk.gov.hmrc.securitiestransferchargefrontend.utils.HeaderCarrierCreator

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import TransactionSubmissionService.*
import play.api.Logging

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
  implicit ec: ExecutionContext
) extends TransactionSubmissionService with Logging {
  
  override def submitSingleStf(using request: StcDataRequest[?]): Future[Boolean] = {
    implicit val hc: HeaderCarrier = headerCarrierCreator.create(request)
    lazy val submissionId = request.userAnswers.submissionId

    etmpSubmissionService
      .submitSingleStf(
        subscriptionId  = request.request.subscriptionId,
        userAnswers     = request.userAnswers,
        affinityData    = getAffinityData(request.request.affinityGroup)
      ).flatMap {
        case stfResponse: SubmissionCreateResponseSuccess =>
          handleSingleStfSuccess(stfResponse, submissionId).map(_ => true)

        case _ =>
          handleSingleStfFailure(submissionId).map(_ => false)
      }
    }

  private def handleSingleStfSuccess(
    stfResponse: SubmissionCreateResponseSuccess,
    submissionId: SubmissionId)(
    implicit hc: HeaderCarrier,
    request: StcDataRequest[?]
  ): Future[Unit] = {
    logger.info(s"Successful response from ETMP for submission $submissionId")
    transactionResponseRepository.store(submissionId, stfResponse)
    saveAndReturnClient.deleteDraft(submissionId)
    sendSingleSubmissionDataToNRS(submissionId, stfResponse.utrn)
    // ToDo: Audit success
  }

  private def handleSingleStfFailure(
    submissionId: SubmissionId
  ): Future[Unit] = {
    logger.warn(s"Failure response from ETMP for submission $submissionId")
    // ToDo: Audit failure
    Future.successful(())
  }
  
  override def submitSingleSh03(using request: StcDataRequest[?]): Future[Boolean] = {
    implicit val hc: HeaderCarrier = headerCarrierCreator.create(request)
    lazy val submissionId = request.userAnswers.submissionId
    val innerRequest = request.request
    lazy val subscriptionId = innerRequest.subscriptionId
    lazy val affinityGroup = innerRequest.affinityGroup
    lazy val credentialId = innerRequest.credentialId
    etmpSubmissionService
      .submitSingleSh03(subscriptionId, request.userAnswers, getAffinityData(affinityGroup))
      .map {
        case sh03Response: SubmissionCreateResponseSuccess =>
          auditService.audit(AuditModel(SubmissionSuccess, subscriptionId, affinityGroup, credentialId, Some(submissionId), Sh03))
          transactionResponseRepository.store(submissionId, sh03Response)
          saveAndReturnClient.deleteDraft(submissionId)
          Some(sh03Response.utrn)

        case _ =>
          auditService.audit(AuditModel(SubmissionFailure, innerRequest.subscriptionId, innerRequest.affinityGroup, innerRequest.credentialId, Some(request.userAnswers.submissionId), Sh03))
          None
      }
      .flatMap { maybeUtrn =>
        maybeUtrn.foreach(utrn => sendSingleSubmissionDataToNRS(submissionId, utrn))
        Future {
          if maybeUtrn.isDefined then true else false
        }
      }
  }

  private def sendSingleSubmissionDataToNRS( submissionId: SubmissionId,
                                             utrn: String
                                           )(
                                             implicit
                                             request: StcDataRequest[?],
                                             hc: HeaderCarrier): Future[Unit] = {
    cyaHtmlRepository
      .retrieve(submissionId)
      .collect {
        case Some(data) =>
          nrsService.singleSubmissionNotableEvent(data, utrn)
      }
  }

  private val getAffinityData: AffinityGroup => AffinityData = {
    case AffinityGroup.Agent        => getAgentAffinityData
    case AffinityGroup.Individual   => getIndividualAffinityData
    case AffinityGroup.Organisation => getOrganisationAffinityData
  }
}

object TransactionSubmissionService {
  val clearedUserAnswers: StcDataRequest[?] => UserAnswers = { req =>
    val currentUserAnswers = req.userAnswers
    UserAnswers.empty(currentUserAnswers.userId)(currentUserAnswers.groupIdentifier)(currentUserAnswers.submissionId)(currentUserAnswers.journeyType)
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
