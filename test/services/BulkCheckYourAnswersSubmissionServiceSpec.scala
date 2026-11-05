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

package services

import base.{FileUploadFixtures, SpecBase}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.AnyContent
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.StcDataRequest
import uk.gov.hmrc.securitiestransferchargefrontend.domain.TransferType
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedStcRowsDocument, ValidatedStcRow}
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.BulkConfirmationDetails
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.SessionRepository
import uk.gov.hmrc.securitiestransferchargefrontend.services.{BulkCheckYourAnswersSubmissionService, TransactionSubmissionService}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class BulkCheckYourAnswersSubmissionServiceSpec extends SpecBase with MockitoSugar with FileUploadFixtures with BeforeAndAfterEach {

  private val mockSessionRepository = mock[SessionRepository]
  private val mockTransactionSubmissionService = mock[TransactionSubmissionService]
  private val mockRequest = mock[StcDataRequest[AnyContent]]

  private val service = new BulkCheckYourAnswersSubmissionService(mockSessionRepository, mockTransactionSubmissionService)(using ExecutionContext.global)

  private val doc = ParsedStcRowsDocument(_id = reference, rows = Seq(parsedStcRow(1), parsedStcRow(2)), fileName = "test-file.csv")
  private val expectedRows: Seq[ValidatedStcRow] = doc.rows.map(row => ValidatedStcRow(row, Seq.empty))

  private val paymentDueBy = LocalDate.of(2026, 3, 22)
  private val taxDue = BigDecimal(100.00)
  private val agentReference = Some("AGENT-REF-1")

  private given StcDataRequest[?] = mockRequest

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSessionRepository, mockTransactionSubmissionService, mockRequest)
    when(mockRequest.userAnswers).thenReturn(emptyUserAnswers)
  }

  private def capturedSavedAnswers(): UserAnswers = {
    val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
    verify(mockSessionRepository).set(captor.capture())
    captor.getValue
  }

  private def confirmationDetailsIn(answers: UserAnswers): BulkConfirmationDetails =
    (answers.data \ BulkConfirmationDetails.key).as[BulkConfirmationDetails]

  "BulkCheckYourAnswersSubmissionService.submit" - {

    "for an STF submission" - {

      "must save the confirmation details, start the STF submission and return the updated answers" in {
        when(mockSessionRepository.set(any())).thenReturn(Future.unit)
        when(mockTransactionSubmissionService.submitBulkStf(any())(using any[StcDataRequest[?]]())).thenReturn(Future.successful(true))

        val result = service.submit(doc, TransferType.STF, paymentDueBy, taxDue, agentReference).futureValue

        val saved = capturedSavedAnswers()
        confirmationDetailsIn(saved) mustEqual
          BulkConfirmationDetails(emptyUserAnswers.submissionId, paymentDueBy, taxDue, agentReference)

        result mustEqual saved

        verify(mockTransactionSubmissionService).submitBulkStf(eqTo(expectedRows))(using eqTo(mockRequest))
        verify(mockTransactionSubmissionService, never()).submitBulkSh03(any())(using any[StcDataRequest[?]]())
      }
    }

    "for an SH03 submission" - {

      "must save the confirmation details, start the SH03 submission and return the updated answers" in {
        when(mockSessionRepository.set(any())).thenReturn(Future.unit)
        when(mockTransactionSubmissionService.submitBulkSh03(any())(using any[StcDataRequest[?]]())).thenReturn(Future.successful(true))

        val result = service.submit(doc, TransferType.SH03, paymentDueBy, taxDue, agentReference).futureValue

        val saved = capturedSavedAnswers()
        confirmationDetailsIn(saved) mustEqual
          BulkConfirmationDetails(emptyUserAnswers.submissionId, paymentDueBy, taxDue, agentReference)

        result mustEqual saved

        verify(mockTransactionSubmissionService).submitBulkSh03(eqTo(expectedRows))(using eqTo(mockRequest))
        verify(mockTransactionSubmissionService, never()).submitBulkStf(any())(using any[StcDataRequest[?]]())
      }
    }

    "must save no agent reference when none is given" in {
      when(mockSessionRepository.set(any())).thenReturn(Future.unit)
      when(mockTransactionSubmissionService.submitBulkStf(any())(using any[StcDataRequest[?]]())).thenReturn(Future.successful(true))

      service.submit(doc, TransferType.STF, paymentDueBy, taxDue, agentReference = None).futureValue

      confirmationDetailsIn(capturedSavedAnswers()).agentReference mustBe None
    }

    "must keep the existing data in the user answers" in {
      val existingAnswers = emptyUserAnswers.setFileUploadReference(reference)
      when(mockRequest.userAnswers).thenReturn(existingAnswers)
      when(mockSessionRepository.set(any())).thenReturn(Future.unit)
      when(mockTransactionSubmissionService.submitBulkStf(any())(using any[StcDataRequest[?]]())).thenReturn(Future.successful(true))

      service.submit(doc, TransferType.STF, paymentDueBy, taxDue, agentReference).futureValue

      val saved = capturedSavedAnswers()
      saved.submissionId mustEqual existingAnswers.submissionId
      saved.fileUploadReference mustEqual existingAnswers.fileUploadReference
      confirmationDetailsIn(saved).submissionId mustEqual existingAnswers.submissionId
    }

    "must still return the updated answers if the submission itself fails, because it is fire and forget" in {
      when(mockSessionRepository.set(any())).thenReturn(Future.unit)
      when(mockTransactionSubmissionService.submitBulkStf(any())(using any[StcDataRequest[?]]()))
        .thenReturn(Future.failed(new RuntimeException("ETMP down")))

      val result = service.submit(doc, TransferType.STF, paymentDueBy, taxDue, agentReference).futureValue

      result mustEqual capturedSavedAnswers()
    }

    "must fail and not start the submission if saving the answers fails" in {
      when(mockSessionRepository.set(any())).thenReturn(Future.failed(new RuntimeException("Mongo down")))

      val result = service.submit(doc, TransferType.STF, paymentDueBy, taxDue, agentReference)

      result.failed.futureValue mustBe a[RuntimeException]
      verify(mockTransactionSubmissionService, never()).submitBulkStf(any())(using any[StcDataRequest[?]]())
      verify(mockTransactionSubmissionService, never()).submitBulkSh03(any())(using any[StcDataRequest[?]]())
    }

    "must fail with NotImplementedError for transfer type Other" in {
      when(mockSessionRepository.set(any())).thenReturn(Future.unit)

      val result = service.submit(doc, TransferType.Other, paymentDueBy, taxDue, agentReference)

      val error = result.failed.futureValue
      error mustBe a[java.util.concurrent.ExecutionException]
      error.getCause mustBe a[NotImplementedError]

      verify(mockTransactionSubmissionService, never()).submitBulkStf(any())(using any[StcDataRequest[?]]())
      verify(mockTransactionSubmissionService, never()).submitBulkSh03(any())(using any[StcDataRequest[?]]())
    }
  }
}