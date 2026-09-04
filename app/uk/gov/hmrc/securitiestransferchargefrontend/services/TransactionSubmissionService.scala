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

import play.twirl.api.HtmlFormat
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SaveAndReturnClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.StcDataRequest
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.Address
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.{AffinityData, Individual}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.TransactionResponseRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait TransactionSubmissionService:
  def storeSubmissionData(html: HtmlFormat.Appendable)(implicit request: StcDataRequest[?]): Future[Unit]
  def submitSingleStf(implicit request: StcDataRequest[?]): Future[Boolean]
  
final class TransactionSubmissionServiceImpl @Inject() (
  etmpSubmissionService: EtmpSubmissionService,
  saveAndReturnClient: SaveAndReturnClient,
  transactionResponseRepository: TransactionResponseRepository)(implicit ec: ExecutionContext) extends TransactionSubmissionService {

  // TODO: Some of this should come from new screens not yet developed.
  val getIndividualAffinityData: AffinityData =
    Individual(
      name = "John Smith",
      address = Address(
        addressLine1 = "10 High Street",
        addressLine2 = Some("Bolton"),
        addressLine3 = None,
        postcode = "BL1 2TG",
        countryCode = "UK"
      ),
      phone = "01234 567890",
      email = "jsmith@foo.com",
      nino = "NX787356B" // Can come from subscription
    )
    
  def storeSubmissionData(html: HtmlFormat.Appendable)(implicit request: StcDataRequest[?]): Future[Unit] = {
    // TODO: NRS ticket - need to store this in a repository
    Future.successful(())
  }

  def submitSingleStf(implicit request: StcDataRequest[?]): Future[Boolean] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    lazy val submissionId = request.userAnswers.submissionId
    etmpSubmissionService
      .submitSingleStf(request.userAnswers, getIndividualAffinityData)
      .map {
        _.fold(false) { stfResponse =>
          transactionResponseRepository.store(submissionId, stfResponse)
          saveAndReturnClient.deleteDraft(submissionId)
          sendSubmissionDataToNRS(submissionId)
          true
        }
      }
  }
  
  // TODO: Needs to be implemented as part of the NRS ticket.
  def sendSubmissionDataToNRS(submissionId: SubmissionId): Future[Unit] = {
    transactionResponseRepository.retrieve(submissionId)
    Future.failed(new NotImplementedError())
  }
}

object TransactionSubmissionService:
  val clearedUserAnswers: StcDataRequest[?] => UserAnswers = { req =>
    val currentUserAnswers = req.userAnswers
    UserAnswers.empty(currentUserAnswers.userId)(currentUserAnswers.groupIdentifier)(currentUserAnswers.submissionId)
  }
    
