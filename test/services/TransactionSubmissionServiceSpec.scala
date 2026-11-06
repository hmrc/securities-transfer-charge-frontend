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

import base.stubs.StubBulkStatusService
import base.{AuditTestSupport, Fixtures, SpecBase}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContent, RequestHeader, Session}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SaveAndReturnClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.{IndividualUniqueIdentifier, OrganisationUniqueIdentifier, StcAuthorisedRequest, StcDataRequest}
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{SubmissionId, SubscriptionId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.ValidatedStcRow
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.AffinityData
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.{CyaHtmlData, CyaHtmlRepository, TransactionResponseRepository}
import uk.gov.hmrc.securitiestransferchargefrontend.services.*
import uk.gov.hmrc.securitiestransferchargefrontend.utils.HeaderCarrierCreator

import java.time.LocalDate
import scala.concurrent.Future

class TransactionSubmissionServiceSpec extends AnyFreeSpec with Matchers with SpecBase with ScalaFutures with MockitoSugar with BeforeAndAfterEach with AuditTestSupport:

  trait TestSetup {
    implicit val dataRequest: StcDataRequest[AnyContent] = mock[StcDataRequest[AnyContent]]

    private val unitF = Future.successful(())

    val mockNrsService: NrsService = mock[NrsService]
    when(mockNrsService.singleSubmissionNotableEvent(any[CyaHtmlData], any[String])(any[StcDataRequest[?]], any[HeaderCarrier]))
      .thenReturn(unitF)

    when(mockNrsService.bulkSubmissionNotableEvent(any[SubmissionId], any[String])(any[HeaderCarrier]))
      .thenReturn(unitF)

    val mockCyaHtmlRepository: CyaHtmlRepository = mock[CyaHtmlRepository]
    when(mockCyaHtmlRepository.retrieve(any[SubmissionId])).thenReturn(
      Future.successful(
        Option(
          CyaHtmlData(
            submissionId = Fixtures.testSubmissionId,
            html = HtmlFormat.empty
          )
        )
      )
    )

    val mockHeaderCarrierCreator: HeaderCarrierCreator = mock[HeaderCarrierCreator]
    when(mockHeaderCarrierCreator.create(any[RequestHeader])).thenReturn(hc)

    val mockUserAnswers: UserAnswers = mock[UserAnswers]
    val mockStcAuthorisedRequest: StcAuthorisedRequest[AnyContent] = mock[StcAuthorisedRequest[AnyContent]]
    val mockSession: Session = mock[Session]
    val stubBulkStatusService = new StubBulkStatusService
    val mockAuditService: AuditService = mock[AuditService]

    when(dataRequest.userAnswers).thenReturn(mockUserAnswers)
    when(dataRequest.request).thenReturn(mockStcAuthorisedRequest)
    when(dataRequest.request.affinityGroup).thenReturn(AffinityGroup.Individual)
    when(dataRequest.session).thenReturn(mockSession)
    when(mockUserAnswers.submissionId).thenReturn(submissionId)
    when(mockUserAnswers.fileUploadReference).thenReturn(Some(Fixtures.testFileUploadReference))
    when(mockStcAuthorisedRequest.subscriptionId).thenReturn(subscriptionId)


    val success: SubmissionCreateResponse = SubmissionCreateResponseSuccess(
      submissionId = submissionId,
      utrn = testUtrn,
      chargeReferences = List.empty[ChargeReference],
      taxDue = 123.45,
      paymentDueBy = LocalDate.now(),
      agentReference = None
    )

    val failure: SubmissionCreateResponse = SubmissionCreateResponseFailure

    val mockEtmpSubmissionService: EtmpSubmissionService = mock[EtmpSubmissionService]
    val mockSaveAndReturnClient: SaveAndReturnClient = mock[SaveAndReturnClient]
    val mockTransactionResponseRepository: TransactionResponseRepository = mock[TransactionResponseRepository]
    val mockSubscriptionService: SubscriptionService = mock[SubscriptionService]

    when(mockSaveAndReturnClient.deleteDraft(any[SubmissionId])(any[HeaderCarrier])).thenReturn(Future.successful(()))
    when(mockTransactionResponseRepository.store(any[SubmissionId], any[SubmissionCreateResponseSuccess])).thenReturn(Future.successful(()))
    when(mockSubscriptionService.getSubscription(any[SubscriptionId])(any[RequestHeader]))
      .thenReturn(Future.successful(Fixtures.testSubscription))
    
    when(mockEtmpSubmissionService.getSubmissionDataStf(any[UserAnswers], any[AffinityData]))
      .thenReturn(None)
    when(mockEtmpSubmissionService.getSubmissionDataSh03(any[UserAnswers], any[AffinityData]))
      .thenReturn(None)
  }

  "The service should" - {
    "Call NRS if ETMP succeeds" in new TestSetup {
      when(mockStcAuthorisedRequest.uniqueIdentifier).thenReturn(IndividualUniqueIdentifier(Fixtures.testNino))
      when(mockEtmpSubmissionService.submitSingleStf(any[SubscriptionId], any[UserAnswers], any[AffinityData])(any[HeaderCarrier])).thenReturn(Future.successful(success))
      val service = new TransactionSubmissionServiceImpl(mockHeaderCarrierCreator, mockEtmpSubmissionService, mockSaveAndReturnClient, mockNrsService, mockCyaHtmlRepository, mockTransactionResponseRepository, stubBulkStatusService, mockSubscriptionService, mockAuditService)
      val outcome = service.submitSingleStf
      whenReady(outcome) { r =>
        verify(mockNrsService, times(1)).singleSubmissionNotableEvent(any[CyaHtmlData], any[String])(any[StcDataRequest[?]], any[HeaderCarrier])
        r mustBe true
      }
    }
    "Do not call NRS if ETMP fails" in new TestSetup {
      when(mockStcAuthorisedRequest.uniqueIdentifier).thenReturn(IndividualUniqueIdentifier(Fixtures.testNino))
      when(mockEtmpSubmissionService.submitSingleStf(any[SubscriptionId], any[UserAnswers], any[AffinityData])(any[HeaderCarrier])).thenReturn(Future.successful(failure))
      val service = new TransactionSubmissionServiceImpl(mockHeaderCarrierCreator, mockEtmpSubmissionService, mockSaveAndReturnClient, mockNrsService, mockCyaHtmlRepository, mockTransactionResponseRepository, stubBulkStatusService, mockSubscriptionService, mockAuditService)
      val outcome = service.submitSingleStf
      whenReady(outcome) { r =>
        verify(mockNrsService, never).singleSubmissionNotableEvent(any[CyaHtmlData], any[String])(any[StcDataRequest[?]], any[HeaderCarrier])
        r mustBe false
      }
    }
    "Call NRS if ETMP succeeds (SH03)" in new TestSetup {
      when(dataRequest.request.affinityGroup).thenReturn(AffinityGroup.Organisation)
      when(mockStcAuthorisedRequest.uniqueIdentifier).thenReturn(OrganisationUniqueIdentifier(Some(Fixtures.testUtr)))
      when(mockEtmpSubmissionService.submitSingleSh03(any[SubscriptionId], any[UserAnswers], any[AffinityData])(any[HeaderCarrier])).thenReturn(Future.successful(success))
      val service = new TransactionSubmissionServiceImpl(mockHeaderCarrierCreator, mockEtmpSubmissionService, mockSaveAndReturnClient, mockNrsService, mockCyaHtmlRepository, mockTransactionResponseRepository, stubBulkStatusService, mockSubscriptionService, mockAuditService)
      val outcome = service.submitSingleSh03
      whenReady(outcome) { r =>
        verify(mockNrsService, times(1)).singleSubmissionNotableEvent(any[CyaHtmlData], any[String])(any[StcDataRequest[?]], any[HeaderCarrier])
        r mustBe true
      }
    }
    "Do not call NRS if ETMP fails (SH03)" in new TestSetup {
      when(dataRequest.request.affinityGroup).thenReturn(AffinityGroup.Organisation)
      when(mockStcAuthorisedRequest.uniqueIdentifier).thenReturn(OrganisationUniqueIdentifier(Some(Fixtures.testUtr)))
      when(mockEtmpSubmissionService.submitSingleSh03(any[SubscriptionId], any[UserAnswers], any[AffinityData])(any[HeaderCarrier])).thenReturn(Future.successful(failure))
      val service = new TransactionSubmissionServiceImpl(mockHeaderCarrierCreator, mockEtmpSubmissionService, mockSaveAndReturnClient, mockNrsService, mockCyaHtmlRepository, mockTransactionResponseRepository, stubBulkStatusService, mockSubscriptionService, mockAuditService)
      val outcome = service.submitSingleSh03
      whenReady(outcome) { r =>
        verify(mockNrsService, never).singleSubmissionNotableEvent(any[CyaHtmlData], any[String])(any[StcDataRequest[?]], any[HeaderCarrier])
        r mustBe false
      }
    }
    "Call NRS if ETMP succeeds (bulk)" in new TestSetup {
      when(mockStcAuthorisedRequest.uniqueIdentifier).thenReturn(IndividualUniqueIdentifier(Fixtures.testNino))
      when(mockEtmpSubmissionService.submitBulkStf(any[SubscriptionId], any[Seq[ValidatedStcRow]], any[UserAnswers], any[AffinityData])(any[HeaderCarrier])).thenReturn(Future.successful(success))
      val service = new TransactionSubmissionServiceImpl(mockHeaderCarrierCreator, mockEtmpSubmissionService, mockSaveAndReturnClient, mockNrsService, mockCyaHtmlRepository, mockTransactionResponseRepository, stubBulkStatusService, mockSubscriptionService, mockAuditService)
      val outcome = service.submitBulkStf(List(Fixtures.testValidatedStcRow))
      whenReady(outcome) { r =>
        verify(mockNrsService, times(1)).bulkSubmissionNotableEvent(any[SubmissionId], any[String])(any[HeaderCarrier])
        r mustBe true
      }
    }
    "Do not all NRS if ETMP succeeds (bulk)" in new TestSetup {
      when(mockStcAuthorisedRequest.uniqueIdentifier).thenReturn(IndividualUniqueIdentifier(Fixtures.testNino))
      when(mockEtmpSubmissionService.submitBulkStf(any[SubscriptionId], any[Seq[ValidatedStcRow]], any[UserAnswers], any[AffinityData])(any[HeaderCarrier])).thenReturn(Future.successful(failure))
      val service = new TransactionSubmissionServiceImpl(mockHeaderCarrierCreator, mockEtmpSubmissionService, mockSaveAndReturnClient, mockNrsService, mockCyaHtmlRepository, mockTransactionResponseRepository, stubBulkStatusService, mockSubscriptionService, mockAuditService)
      val outcome = service.submitBulkStf(List(Fixtures.testValidatedStcRow))
      whenReady(outcome) { r =>
        verify(mockNrsService, never).bulkSubmissionNotableEvent(any[SubmissionId], any[String])(any[HeaderCarrier])
        r mustBe false
      }
    }
    "Call NRS if ETMP succeeds (SH03 bulk)" in new TestSetup {
      when(mockStcAuthorisedRequest.uniqueIdentifier).thenReturn(IndividualUniqueIdentifier(Fixtures.testNino))
      when(mockEtmpSubmissionService.submitBulkSH03(any[SubscriptionId], any[Seq[ValidatedStcRow]], any[UserAnswers], any[AffinityData])(any[HeaderCarrier])).thenReturn(Future.successful(success))
      val service = new TransactionSubmissionServiceImpl(mockHeaderCarrierCreator, mockEtmpSubmissionService, mockSaveAndReturnClient, mockNrsService, mockCyaHtmlRepository, mockTransactionResponseRepository, stubBulkStatusService, mockSubscriptionService, mockAuditService)
      val outcome = service.submitBulkSh03(List(Fixtures.testValidatedStcRow))
      whenReady(outcome) { r =>
        verify(mockNrsService, times(1)).bulkSubmissionNotableEvent(any[SubmissionId], any[String])(any[HeaderCarrier])
        r mustBe true
      }
    }
    "Do not all NRS if ETMP succeeds (SH03 bulk)" in new TestSetup {
      when(mockStcAuthorisedRequest.uniqueIdentifier).thenReturn(IndividualUniqueIdentifier(Fixtures.testNino))
      when(mockEtmpSubmissionService.submitBulkSH03(any[SubscriptionId], any[Seq[ValidatedStcRow]], any[UserAnswers], any[AffinityData])(any[HeaderCarrier])).thenReturn(Future.successful(failure))
      val service = new TransactionSubmissionServiceImpl(mockHeaderCarrierCreator, mockEtmpSubmissionService, mockSaveAndReturnClient, mockNrsService, mockCyaHtmlRepository, mockTransactionResponseRepository, stubBulkStatusService, mockSubscriptionService, mockAuditService)
      val outcome = service.submitBulkSh03(List(Fixtures.testValidatedStcRow))
      whenReady(outcome) { r =>
        verify(mockNrsService, never).bulkSubmissionNotableEvent(any[SubmissionId], any[String])(any[HeaderCarrier])
        r mustBe false
      }
    }
    "Call audit service if ETMP succeeds" in new TestSetup {
      when(mockStcAuthorisedRequest.uniqueIdentifier).thenReturn(IndividualUniqueIdentifier(Fixtures.testNino))
      when(mockEtmpSubmissionService.submitSingleStf(any[SubscriptionId], any[UserAnswers], any[AffinityData])(any[HeaderCarrier])).thenReturn(Future.successful(success))
      val service = new TransactionSubmissionServiceImpl(mockHeaderCarrierCreator, mockEtmpSubmissionService, mockSaveAndReturnClient, mockNrsService, mockCyaHtmlRepository, mockTransactionResponseRepository, stubBulkStatusService, mockSubscriptionService, mockAuditService)
      val outcome = service.submitSingleStf
      whenReady(outcome) { r =>
        verify(mockAuditService, atLeastOnce).audit(any())(any())
        r mustBe true
      }
    }
    "Do not call audit service if ETMP fails" in new TestSetup {
      when(mockStcAuthorisedRequest.uniqueIdentifier).thenReturn(IndividualUniqueIdentifier(Fixtures.testNino))
      when(mockEtmpSubmissionService.submitSingleStf(any[SubscriptionId], any[UserAnswers], any[AffinityData])(any[HeaderCarrier])).thenReturn(Future.successful(failure))
      val service = new TransactionSubmissionServiceImpl(mockHeaderCarrierCreator, mockEtmpSubmissionService, mockSaveAndReturnClient, mockNrsService, mockCyaHtmlRepository, mockTransactionResponseRepository, stubBulkStatusService, mockSubscriptionService, mockAuditService)
      val outcome = service.submitSingleStf
      whenReady(outcome) { r =>
        verify(mockAuditService, times(1)).audit(any())(any())
        r mustBe false
      }
    }
  }
