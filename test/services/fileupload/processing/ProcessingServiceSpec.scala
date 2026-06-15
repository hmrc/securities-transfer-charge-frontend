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

package services.fileupload.processing

import base.{FileUploadFixtures, SpecBase}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{inOrder as mockitoInOrder, *}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout, running}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.{SubscriptionConnector, UpscanDownloadException}
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.StcAuthorisedRequest
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{CredentialId, SubscriptionId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.FileParseError
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.UpscanJourneyStatus.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.{FileUpload, UpscanJourneyStatus}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.{UpscanJourneyRepository, ValidationErrorRepository}
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcUpscanProcessingService
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.processing.ProcessingService

import scala.concurrent.Future

class ProcessingServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach with FileUploadFixtures {

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockUpscanProcessingService, mockValidationErrorRepository, mockUpscanJourneyRepository, mockSubscriptionConnector)
  }

  private val mockUpscanProcessingService = mock[StcUpscanProcessingService]

  private val mockValidationErrorRepository = mock[ValidationErrorRepository]

  private val mockUpscanJourneyRepository = mock[UpscanJourneyRepository]

  private val mockSubscriptionConnector = mock[SubscriptionConnector]

  private val service = new ProcessingService(mockUpscanProcessingService, mockValidationErrorRepository, mockUpscanJourneyRepository, mockSubscriptionConnector)

  private val reference = "reference"
  private val affinityKey = "affinity-key"

  private val fileUpload = FileUpload(reference = reference, status = UpscanJourneyStatus.Ready)

  def fakeApplication(): Application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

  private def stubStatusUpdates(): Unit =
    when(
      mockUpscanJourneyRepository.updateStatus(
        any[String],
        any[UpscanJourneyStatus]
      )
    ).thenReturn(Future.unit)

  "processReadyUpload" - {

    "must mark upload as Processing before processing begins" in {

      stubStatusUpdates()

      when(
        mockUpscanProcessingService.process(any[FileUpload], any[String], any[String])(any)
      ).thenReturn(
        Future.successful(
          Left(FileParseError.EmptyFile)
        )
      )

      running(fakeApplication()) {

        await(
          service.processReadyUpload(
            reference,
            fileUpload,
            affinityKey,
            "stf"
          )(any(), any())
        )

        verify(mockUpscanJourneyRepository)
          .updateStatus(reference, Processing)
      }
    }

    "must update status to RowLimitExceeded when row limit exceeded" in {

      stubStatusUpdates()

      when(
        mockUpscanProcessingService.process(any[FileUpload], any[String], any[String])(any())
      ).thenReturn(
        Future.successful(
          Left(FileParseError.RowLimitExceeded(100, 10))
        )
      )

      running(fakeApplication()) {

        await(
          service.processReadyUpload(
            reference,
            fileUpload,
            affinityKey,
            "stf"
          )(any(), any())
        )

        verify(mockUpscanJourneyRepository)
          .updateStatus(reference, RowLimitExceeded)
      }
    }

    "must update status to EmptyFile" in {

      stubStatusUpdates()

      when(
        mockUpscanProcessingService.process(any[FileUpload], any[String], any[String])(any())
      ).thenReturn(
        Future.successful(
          Left(FileParseError.EmptyFile)
        )
      )

      running(fakeApplication()) {

        await(
          service.processReadyUpload(
            reference,
            fileUpload,
            affinityKey,
            "stf"
          )(any(), any())
        )

        verify(mockUpscanJourneyRepository)
          .updateStatus(reference, EmptyFile)
      }
    }

    "must update status to InvalidTemplate" in {

      stubStatusUpdates()

      when(
        mockUpscanProcessingService.process(any[FileUpload], any[String], any[String])(any())
      ).thenReturn(
        Future.successful(
          Left(FileParseError.InvalidTemplate)
        )
      )

      running(fakeApplication()) {

        await(
          service.processReadyUpload(
            reference,
            fileUpload,
            affinityKey,
            "stf"
          )(any(), any())
        )

        verify(mockUpscanJourneyRepository)
          .updateStatus(reference, InvalidTemplate)
      }
    }

    "must update status to FileParseError for any other parse error" in {

      stubStatusUpdates()

      when(
        mockUpscanProcessingService.process(any[FileUpload], any[String], any[String])(any())
      ).thenReturn(
        Future.successful(
          Left(FileParseError.MissingWorksheet("test"))
        )
      )

      running(fakeApplication()) {

        await(
          service.processReadyUpload(
            reference,
            fileUpload,
            affinityKey,
            "stf"
          )(any(), any())
        )

        verify(mockUpscanJourneyRepository)
          .updateStatus(reference, UpscanJourneyStatus.FileParseError)
      }
    }

    "must update status to TooManyErrors when validation response contains too many blocking errors" in {

      stubStatusUpdates()
      val validationResponse = validationResponseWithErrors(withBlockingErrors(26))


      when(
        mockUpscanProcessingService.process(any[FileUpload], any[String], any[String])(any())
      ).thenReturn(
        Future.successful(
          Right(validationResponse)
        )
      )

      running(fakeApplication()) {

        await(
          service.processReadyUpload(
            reference,
            fileUpload,
            affinityKey,
            "stf"
          )(any(), any())
        )

        verify(mockUpscanJourneyRepository)
          .updateStatus(reference, TooManyErrors)
      }
    }

    "must save validation errors and update status to FormatingErrors when blocking errors exist" in {

      stubStatusUpdates()
      val validationResponse = validationResponseWithErrors(blockingValidationErrors)


      when(
        mockValidationErrorRepository.save(
          reference,
          blockingValidationErrors
        )
      ).thenReturn(Future.unit)

      when(
        mockUpscanProcessingService.process(any[FileUpload], any[String], any[String])(any())
      ).thenReturn(
        Future.successful(
          Right(validationResponse)
        )
      )

      running(fakeApplication()) {

        await(
          service.processReadyUpload(
            reference,
            fileUpload,
            affinityKey,
            "stf"
          )(any(), any())
        )

        verify(mockValidationErrorRepository)
          .save(reference, blockingValidationErrors)

        verify(mockUpscanJourneyRepository)
          .updateStatus(reference, FormatingErrors)
      }
    }

    "must store subscription and mark upload Completed when validation succeeds" in {

      implicit val request: StcAuthorisedRequest[AnyContentAsEmpty.type] =
        StcAuthorisedRequest(
          FakeRequest(),
          internalId = "some-id",
          affinityGroup = individualAffinity,
          subscriptionId = SubscriptionId("STC-GFGF"),
          credentialId = CredentialId("some id")
        )

      implicit val hc: HeaderCarrier = HeaderCarrier()

      stubStatusUpdates()

      val validationResponse = successfulValidationResponse

      when(
        mockSubscriptionConnector.getAndStoreSubscription(
          any[SubscriptionId]
        )(any[HeaderCarrier])
      ).thenReturn(
        Future.successful(subscription)
      )

      when(
        mockUpscanProcessingService.process(
          any[FileUpload],
          any[String],
          any[String]
        )(any())
      ).thenReturn(
        Future.successful(
          Right(validationResponse)
        )
      )

      running(fakeApplication()) {

        await(
          service.processReadyUpload(
            reference,
            fileUpload,
            affinityKey,
            "stf"
          )
        )

        verify(mockSubscriptionConnector)
          .getAndStoreSubscription(
            ArgumentMatchers.eq(SubscriptionId("STC-GFGF"))
          )(any[HeaderCarrier])

        verify(mockUpscanJourneyRepository)
          .updateStatus(reference, Completed)
      }
    }

    "must update status to UpscanDownloadError when processing throws UpscanDownloadException" in {

      val exception = new RuntimeException

      stubStatusUpdates()

      when(
        mockUpscanProcessingService.process(any[FileUpload], any[String], any[String])(any())
      ).thenReturn(
        Future.failed(
          UpscanDownloadException("download failed", exception)
        )
      )

      running(fakeApplication()) {

        await(
          service.processReadyUpload(
            reference,
            fileUpload,
            affinityKey,
            "stf"
          )(any(), any())
        )

        verify(mockUpscanJourneyRepository)
          .updateStatus(reference, UpscanDownloadError)
      }
    }

    "must update statuses in the correct order" in {

      stubStatusUpdates()

      when(
        mockUpscanProcessingService.process(any[FileUpload], any[String], any[String])(any())
      ).thenReturn(
        Future.successful(
          Left(FileParseError.EmptyFile)
        )
      )

      running(fakeApplication()) {

        await(
          service.processReadyUpload(
            reference,
            fileUpload,
            affinityKey,
            "stf"
          )(any(), any())
        )

        val inOrderVerifier = mockitoInOrder(mockUpscanJourneyRepository)

        inOrderVerifier.verify(mockUpscanJourneyRepository)
          .updateStatus(reference, Processing)

        inOrderVerifier.verify(mockUpscanJourneyRepository)
          .updateStatus(reference, EmptyFile)
      }
    }
  }
}