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

import base.SpecBase
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SaveAndReturnClient
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.{CyaHtmlRepository, TransactionResponseRepository}
import uk.gov.hmrc.securitiestransferchargefrontend.services.{ChargeReference, EtmpSubmissionService, SubmissionCreateResponse, SubmissionCreateResponseFailure, SubmissionCreateResponseSuccess, TransactionSubmissionService, TransactionSubmissionServiceImpl}
import org.mockito.Mockito.*
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{SubmissionId, SubscriptionId}
import org.mockito.ArgumentMatchers.any
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{AnyContent, RequestHeader, Session}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.registration.NrsClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.StcAuthorisedRequest
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.StcDataRequest
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.AffinityData
import uk.gov.hmrc.securitiestransferchargefrontend.utils.HeaderCarrierCreator

import java.time.LocalDate
import scala.concurrent.Future

class TransactionSubmissionServiceSpec extends AnyFreeSpec with Matchers with SpecBase with ScalaFutures with MockitoSugar with BeforeAndAfterEach:

  implicit val dataRequest: StcDataRequest[AnyContent] = mock[StcDataRequest[AnyContent]]

  val mockNrsClient: NrsClient = mock[NrsClient]
  when(mockNrsClient.postHtmlPayload(any[HtmlFormat.Appendable])).thenReturn(Future.successful(()))

  val mockCyaHtmlRepository: CyaHtmlRepository = mock[CyaHtmlRepository]
  when(mockCyaHtmlRepository.retrieve(any[SubmissionId])).thenReturn(Future.successful(HtmlFormat.empty))

  val mockHeaderCarrierCreator: HeaderCarrierCreator = mock[HeaderCarrierCreator]
  when(mockHeaderCarrierCreator.create(any[RequestHeader])).thenReturn(hc)

  val mockUserAnswers: UserAnswers = mock[UserAnswers]
  val mockStcAuthorisedRequest: StcAuthorisedRequest[AnyContent] = mock[StcAuthorisedRequest[AnyContent]]
  val mockSession: Session = mock[Session]

  when(dataRequest.userAnswers).thenReturn(mockUserAnswers)
  when(dataRequest.request).thenReturn(mockStcAuthorisedRequest)
  when(dataRequest.request.affinityGroup).thenReturn(AffinityGroup.Individual)
  when(dataRequest.session).thenReturn(mockSession)
  when(mockUserAnswers.submissionId).thenReturn(submissionId)
  when(mockStcAuthorisedRequest.subscriptionId).thenReturn(subscriptionId)

  val success: SubmissionCreateResponse = SubmissionCreateResponseSuccess(
    submissionId = submissionId,
    chargeReferences = List.empty[ChargeReference],
    taxDue = 123.45,
    paymentDueBy = LocalDate.now(),
    agentReference = None
  )

  val failure: SubmissionCreateResponse = SubmissionCreateResponseFailure

  val mockEtmpSubmissionService: EtmpSubmissionService = mock[EtmpSubmissionService]
  val mockSaveAndReturnClient: SaveAndReturnClient = mock[SaveAndReturnClient]
  val mockTransactionResponseRepository: TransactionResponseRepository = mock[TransactionResponseRepository]

  when(mockSaveAndReturnClient.deleteDraft(any[SubmissionId])(any[HeaderCarrier])).thenReturn(Future.successful(()))
  when(mockTransactionResponseRepository.store(any[SubmissionId], any[SubmissionCreateResponseSuccess])).thenReturn(Future.successful(()))

  override def afterEach(): Unit = {
    reset(mockEtmpSubmissionService)
    reset(mockSaveAndReturnClient)
    reset(mockNrsClient)
    reset(mockTransactionResponseRepository)
    super.afterEach()
  }

  def testSetup(etmpSuccess: Boolean): TransactionSubmissionService = {
    if etmpSuccess then
      when(mockEtmpSubmissionService.submitSingleStf(any[SubscriptionId], any[UserAnswers], any[AffinityData])(any[HeaderCarrier])).thenReturn(Future.successful(success))
      when(mockEtmpSubmissionService.submitSingleSh03(any[SubscriptionId], any[UserAnswers], any[AffinityData])(any[HeaderCarrier])).thenReturn(Future.successful(success))
    else
      when(mockEtmpSubmissionService.submitSingleStf(any[SubscriptionId], any[UserAnswers], any[AffinityData])(any[HeaderCarrier])).thenReturn(Future.successful(failure))
      when(mockEtmpSubmissionService.submitSingleSh03(any[SubscriptionId], any[UserAnswers], any[AffinityData])(any[HeaderCarrier])).thenReturn(Future.successful(failure))
    end if
    new TransactionSubmissionServiceImpl(mockHeaderCarrierCreator, mockEtmpSubmissionService, mockSaveAndReturnClient, mockNrsClient, mockCyaHtmlRepository, mockTransactionResponseRepository)
  }

  "The service should" - {
    "always call ETMP" in {
      val service = testSetup(false)
      val outcome = service.submitSingleStf(dataRequest)
      whenReady(outcome) { r =>
        verify(mockEtmpSubmissionService).submitSingleStf(any[SubscriptionId], any[UserAnswers], any[AffinityData])(any[HeaderCarrier])
        r mustBe false
      }
    }
    "Call NRS if ETMP succeeds" in {
      val service = testSetup(true)
      val outcome = service.submitSingleStf(dataRequest)
      whenReady(outcome) { r =>
        verify(mockNrsClient, times(1)).postHtmlPayload(any[HtmlFormat.Appendable])
        r mustBe true
      }
    }
    "Do not call NRS if ETMP fails" in {
      val service = testSetup(false)
      val outcome = service.submitSingleStf(dataRequest)
      whenReady(outcome) { r =>
        verify(mockNrsClient, never).postHtmlPayload(any[HtmlFormat.Appendable])
        r mustBe false
      }
    }
    "Delete the draft if ETMP succeeds" in {
      val service = testSetup(true)
      val outcome = service.submitSingleStf(dataRequest)
      whenReady(outcome) { r =>
        verify(mockSaveAndReturnClient, times(1)).deleteDraft(any[SubmissionId])(any[HeaderCarrier])
        r mustBe true
      }
    }
    "Do not delete the draft if ETMP fails" in {
      val service = testSetup(false)
      val outcome = service.submitSingleStf(dataRequest)
      whenReady(outcome) { r =>
        verify(mockSaveAndReturnClient, never).deleteDraft(any[SubmissionId])(any[HeaderCarrier])
        r mustBe false
      }
    }
    "Store successful responses" in {
      val service = testSetup(true)
      val outcome = service.submitSingleStf(dataRequest)
      whenReady(outcome) { r =>
        verify(mockTransactionResponseRepository, times(1)).store(any[SubmissionId], any[SubmissionCreateResponseSuccess])
        r mustBe true
      }
    }
    "Do not store failure responses" in {
      val service = testSetup(false)
      val outcome = service.submitSingleStf(dataRequest)
      whenReady(outcome) { r =>
        verify(mockTransactionResponseRepository, never).store(any[SubmissionId], any[SubmissionCreateResponseSuccess])
        r mustBe false
      }
    }

    "always call ETMP (SH03)" in {
      val service = testSetup(false)
      val outcome = service.submitSingleSh03(dataRequest)
      whenReady(outcome) { r =>
        verify(mockEtmpSubmissionService).submitSingleSh03(any[SubscriptionId], any[UserAnswers], any[AffinityData])(any[HeaderCarrier])
        r mustBe false
      }
    }
    "Call NRS if ETMP succeeds (SH03)" in {
      val service = testSetup(true)
      val outcome = service.submitSingleSh03(dataRequest)
      whenReady(outcome) { r =>
        verify(mockNrsClient, times(1)).postHtmlPayload(any[HtmlFormat.Appendable])
        r mustBe true
      }
    }
    "Do not call NRS if ETMP fails (SH03)" in {
      val service = testSetup(false)
      val outcome = service.submitSingleSh03(dataRequest)
      whenReady(outcome) { r =>
        verify(mockNrsClient, never).postHtmlPayload(any[HtmlFormat.Appendable])
        r mustBe false
      }
    }
    "Delete the draft if ETMP succeeds (SH03)" in {
      val service = testSetup(true)
      val outcome = service.submitSingleSh03(dataRequest)
      whenReady(outcome) { r =>
        verify(mockSaveAndReturnClient, times(1)).deleteDraft(any[SubmissionId])(any[HeaderCarrier])
        r mustBe true
      }
    }
    "Do not delete the draft if ETMP fails (SH03)" in {
      val service = testSetup(false)
      val outcome = service.submitSingleSh03(dataRequest)
      whenReady(outcome) { r =>
        verify(mockSaveAndReturnClient, never).deleteDraft(any[SubmissionId])(any[HeaderCarrier])
        r mustBe false
      }
    }
    "Store successful responses (SH03)" in {
      val service = testSetup(true)
      val outcome = service.submitSingleSh03(dataRequest)
      whenReady(outcome) { r =>
        verify(mockTransactionResponseRepository, times(1)).store(any[SubmissionId], any[SubmissionCreateResponseSuccess])
        r mustBe true
      }
    }
    "Do not store failure responses (SH03)" in {
      val service = testSetup(false)
      val outcome = service.submitSingleSh03(dataRequest)
      whenReady(outcome) { r =>
        verify(mockTransactionResponseRepository, never).store(any[SubmissionId], any[SubmissionCreateResponseSuccess])
        r mustBe false
      }
    }

  }
