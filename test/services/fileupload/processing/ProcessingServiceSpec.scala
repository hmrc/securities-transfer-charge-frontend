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

import base.{AuditTestSupport, FileUploadFixtures, Fixtures, SpecBase}
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
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.StcAuthorisedRequest
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{CredentialId, SubscriptionId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.STF
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType
import uk.gov.hmrc.securitiestransferchargefrontend.models.audit.AuditType.BulkUploadProcessed
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{FileParseError, ParsedStcRow, StcRowValidationError}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.UpscanJourneyStatus.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.{FileUpload, UpscanJourneyStatus}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.{ParsedStcRowsRepository, UpscanJourneyRepository, ValidationErrorRepository}
import uk.gov.hmrc.securitiestransferchargefrontend.services.AuditService
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcUpscanProcessingService
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.processing.ProcessingService

import scala.concurrent.Future

class ProcessingServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach with FileUploadFixtures with AuditTestSupport {

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockUpscanProcessingService, mockValidationErrorRepository, mockUpscanJourneyRepository, mockSubscriptionConnector, mockAuditService)
  }

  private val mockUpscanProcessingService = mock[StcUpscanProcessingService]

  private val mockValidationErrorRepository = mock[ValidationErrorRepository]

  private val mockUpscanJourneyRepository = mock[UpscanJourneyRepository]

  private val mockSubscriptionConnector = mock[SubscriptionConnector]

  private val mockParsedStcRowsRepository = mock[ParsedStcRowsRepository]

  private val mockAuditService = mock[AuditService]

  private val service = new ProcessingService(mockUpscanProcessingService, mockValidationErrorRepository, mockUpscanJourneyRepository, mockSubscriptionConnector, mockParsedStcRowsRepository, mockAuditService)

  private val reference = "reference"
  private val affinityKey = "affinity-key"

  private val fileUpload = readyFileUpload(reference = reference)

  private def getFileType(fileUpload: FileUpload): String = {
    fileUpload.uploadDetails.map(_.fileMimeType).get match {
      case "text/csv" => "csv"
      case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" => "excel"
      case _ => ""
    }
  }

  def fakeApplication(): Application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

  private def stubStatusUpdates(): Unit =
    when(
      mockUpscanJourneyRepository.updateStatus(
        any[String],
        any[UpscanJourneyStatus]
      )
    ).thenReturn(Future.unit)

  implicit val request: StcAuthorisedRequest[AnyContentAsEmpty.type] =
    StcAuthorisedRequest(
      FakeRequest(),
      internalId = testUserId.value,
      groupIdentifier = testGroupIdentifier.value,
      affinityGroup = individualAffinity,
      subscriptionId = SubscriptionId("STC-GFGF"),
      credentialId = CredentialId("some id"),
      identityData = Fixtures.testIdentityData,
      maybeArn = None
    )

  "processReadyUpload" - {

    "must mark upload as Processing before processing begins" in {
      stubStatusUpdates()

      when(
        mockUpscanProcessingService.process(any[FileUpload], any[String], any[JourneyType])(any)
      ).thenReturn(
        Future.successful(
          Left((FileParseError.EmptyFile, 0L))
        )
      )

      running(fakeApplication()) {

        await(
          service.processReadyUpload(
            reference,
            fileUpload,
            affinityKey,
            STF
          )
        )

        verify(mockUpscanJourneyRepository)
          .updateStatus(reference, Processing)
      }
    }

    "must update status to RowLimitExceeded when row limit exceeded" in {

      stubStatusUpdates()

      when(
        mockUpscanProcessingService.process(any[FileUpload], any[String], any[JourneyType])(any())
      ).thenReturn(
        Future.successful(
          Left((FileParseError.RowLimitExceeded(100, 10), 0L))
        )
      )

      running(fakeApplication()) {

        await(
          service.processReadyUpload(
            reference,
            fileUpload,
            affinityKey,
            STF
          )
        )

        verify(mockUpscanJourneyRepository)
          .updateStatus(reference, RowLimitExceeded)
      }
    }

    "must update status to EmptyFile" in {

      stubStatusUpdates()

      when(
        mockUpscanProcessingService.process(any[FileUpload], any[String], any[JourneyType])(any())
      ).thenReturn(
        Future.successful(
          Left((FileParseError.EmptyFile, 0L))
        )
      )

      running(fakeApplication()) {

        await(
          service.processReadyUpload(
            reference,
            fileUpload,
            affinityKey,
            STF
          )
        )

        verify(mockUpscanJourneyRepository)
          .updateStatus(reference, EmptyFile)
      }
    }

    "must update status to InvalidTemplate" in {

      stubStatusUpdates()

      when(
        mockUpscanProcessingService.process(any[FileUpload], any[String], any[JourneyType])(any())
      ).thenReturn(
        Future.successful(
          Left((FileParseError.InvalidTemplate, 0L))
        )
      )

      running(fakeApplication()) {

        await(
          service.processReadyUpload(
            reference,
            fileUpload,
            affinityKey,
            STF
          )
        )

        verify(mockUpscanJourneyRepository)
          .updateStatus(reference, InvalidTemplate)
      }
    }

    "must update status to FileParseError for any other parse error" in {

      stubStatusUpdates()

      when(
        mockUpscanProcessingService.process(any[FileUpload], any[String], any[JourneyType])(any())
      ).thenReturn(
        Future.successful(
          Left((FileParseError.MissingWorksheet("test"), 0L))
        )
      )

      running(fakeApplication()) {

        await(
          service.processReadyUpload(
            reference,
            fileUpload,
            affinityKey,
            STF
          )
        )

        verify(mockUpscanJourneyRepository)
          .updateStatus(reference, UpscanJourneyStatus.FileParseError)
      }
    }

    "must update status to TooManyErrors when validation response contains too many blocking errors" in {

      stubStatusUpdates()
      val validationResponse = validationResponseWithErrors(withBlockingErrors(26))


      when(
        mockUpscanProcessingService.process(any[FileUpload], any[String], any[JourneyType])(any())
      ).thenReturn(
        Future.successful(
          Right((validationResponse, 0L))
        )
      )

      running(fakeApplication()) {

        await(
          service.processReadyUpload(
            reference,
            fileUpload,
            affinityKey,
            STF
          )
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
        mockUpscanProcessingService.process(any[FileUpload], any[String], any[JourneyType])(any())
      ).thenReturn(
        Future.successful(
          Right((validationResponse, 0L))
        )
      )

      running(fakeApplication()) {

        await(
          service.processReadyUpload(
            reference,
            fileUpload,
            affinityKey,
            STF
          )
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
          internalId = testUserId.value,
          groupIdentifier = testGroupIdentifier.value,
          affinityGroup = individualAffinity,
          subscriptionId = SubscriptionId("STC-GFGF"),
          credentialId = CredentialId("some id"),
          identityData = Fixtures.testIdentityData,
          maybeArn = None
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
        mockParsedStcRowsRepository.save(
          any[String],
          any[Seq[ParsedStcRow]],
          any[String])
      ).thenReturn(Future.successful(()))

      when(
        mockUpscanProcessingService.process(
          any[FileUpload],
          any[String],
          any[JourneyType]
        )(any())
      ).thenReturn(
        Future.successful(
          Right((validationResponse, 0L))
        )
      )

      running(fakeApplication()) {

        await(
          service.processReadyUpload(
            reference,
            fileUpload,
            affinityKey,
            STF
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
        mockUpscanProcessingService.process(any[FileUpload], any[String], any[JourneyType])(any())
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
            STF
          )
        )

        verify(mockUpscanJourneyRepository)
          .updateStatus(reference, UpscanDownloadError)
      }
    }

    "must update statuses in the correct order" in {

      stubStatusUpdates()

      when(
        mockUpscanProcessingService.process(any[FileUpload], any[String], any[JourneyType])(any())
      ).thenReturn(
        Future.successful(
          Left((FileParseError.EmptyFile, 0L))
        )
      )

      running(fakeApplication()) {

        await(
          service.processReadyUpload(
            reference,
            fileUpload,
            affinityKey,
            STF
          )
        )

        val inOrderVerifier = mockitoInOrder(mockUpscanJourneyRepository)

        inOrderVerifier.verify(mockUpscanJourneyRepository)
          .updateStatus(reference, Processing)

        inOrderVerifier.verify(mockUpscanJourneyRepository)
          .updateStatus(reference, EmptyFile)
      }
    }

    "must send a bulk upload success audit event if upload succeeds without errors" in {
      stubStatusUpdates()

      when(
        mockUpscanProcessingService.process(any[FileUpload], any[String], any[JourneyType])(any())
      ).thenReturn(
        Future.successful(
          Right((successfulValidationResponse, validationTime))
        )
      )
      when(mockParsedStcRowsRepository.save(any[String], any[Seq[ParsedStcRow]], any[String]))
        .thenReturn(Future.successful(()))

      when(mockSubscriptionConnector.getAndStoreSubscription(any[SubscriptionId])(any()))
        .thenReturn(Future.successful((subscription)))

      running(fakeApplication()) {

        val outcome = service.processReadyUpload(reference, fileUpload, affinityKey, STF)

        whenReady(outcome) { _ =>
          verifyBulkUploadAudit(
            auditService = mockAuditService,
            uploadJourney = "STF",
            affinityGroup = request.affinityGroup,
            subscriptionId = request.subscriptionId,
            credentialId = request.credentialId,
            fileType = getFileType(fileUpload),
            fileUploadStatus = "Success",
            fileSize = fileUpload.uploadDetails.map(_.size.toString).getOrElse(""),
            fileValidationTime = validationTime,
            fileName = fileUpload.uploadDetails.map(_.fileName).getOrElse(""),
            fileReference = fileUpload.reference,
            numberOfEntries = Some(successfulValidationResponse.rows.size),
            errorType = None,
            volume = None,
            stcAuditType = BulkUploadProcessed
          )
        }
      }
    }

    "must send a bulk upload failure audit event when a file parse error occurs" in {
      stubStatusUpdates()

      when(
        mockUpscanProcessingService.process(any[FileUpload], any[String], any[JourneyType])(any())
      ).thenReturn(
        Future.successful(
          Left((FileParseError.RowLimitExceeded(actual = 15000, max = 10000), 0L))
        )
      )

      running(fakeApplication()) {

        val outcome = service.processReadyUpload(reference, fileUpload, affinityKey, STF)

        whenReady(outcome) { _ =>
          verifyBulkUploadAudit(
            auditService = mockAuditService,
            uploadJourney = "STF",
            affinityGroup = request.affinityGroup,
            subscriptionId = request.subscriptionId,
            credentialId = request.credentialId,
            fileType = getFileType(fileUpload),
            fileUploadStatus = "Failure",
            fileSize = fileUpload.uploadDetails.map(_.size.toString).getOrElse(""),
            fileValidationTime = 0L,
            fileName = fileUpload.uploadDetails.map(_.fileName).getOrElse(""),
            fileReference = fileUpload.reference,
            numberOfEntries = None,
            errorType = Some("rowLimitExceeded - 15000 total rows, but only 10000 allowed"),
            volume = Some("0"),
            stcAuditType = BulkUploadProcessed
          )
        }
      }
    }

    "must send a bulk upload failure audit event when validation response contains errors" in {
      stubStatusUpdates()

      val validationResponse = validationResponseWithErrors(withBlockingErrors(1))

      when(
        mockUpscanProcessingService.process(any[FileUpload], any[String], any[JourneyType])(any())
      ).thenReturn(
        Future.successful(
          Right((validationResponse, validationTime))
        )
      )
      when(mockValidationErrorRepository.save(any[String], any[Seq[StcRowValidationError]]))
        .thenReturn(Future.successful(()))

      running(fakeApplication()) {

        val outcome = service.processReadyUpload(reference, fileUpload, affinityKey, STF)

        whenReady(outcome) { _ =>
          verifyBulkUploadAudit(
            auditService = mockAuditService,
            uploadJourney = "STF",
            affinityGroup = request.affinityGroup,
            subscriptionId = request.subscriptionId,
            credentialId = request.credentialId,
            fileType = getFileType(fileUpload),
            fileUploadStatus = "Failure",
            fileSize = fileUpload.uploadDetails.map(_.size.toString).getOrElse(""),
            fileValidationTime = validationTime,
            fileName = fileUpload.uploadDetails.map(_.fileName).getOrElse(""),
            fileReference = fileUpload.reference,
            numberOfEntries = None,
            errorType = Some("sellerName - Error 1"),
            volume = Some("1"),
            stcAuditType = BulkUploadProcessed
          )
        }
      }
    }

    "must send a bulk upload failure audit event with errorType 'multiple' and volume as 'morethan25' when validation response contains more than 25 errors" in {
      stubStatusUpdates()

      val validationResponse = validationResponseWithErrors(withBlockingErrors(26))

      when(
        mockUpscanProcessingService.process(any[FileUpload], any[String], any[JourneyType])(any())
      ).thenReturn(
        Future.successful(
          Right((validationResponse, validationTime))
        )
      )
      when(mockValidationErrorRepository.save(any[String], any[Seq[StcRowValidationError]]))
        .thenReturn(Future.successful(()))

      running(fakeApplication()) {

        val outcome = service.processReadyUpload(reference, fileUpload, affinityKey, STF)

        whenReady(outcome) { _ =>
          verifyBulkUploadAudit(
            auditService = mockAuditService,
            uploadJourney = "STF",
            affinityGroup = request.affinityGroup,
            subscriptionId = request.subscriptionId,
            credentialId = request.credentialId,
            fileType = getFileType(fileUpload),
            fileUploadStatus = "Failure",
            fileSize = fileUpload.uploadDetails.map(_.size.toString).getOrElse(""),
            fileValidationTime = validationTime,
            fileName = fileUpload.uploadDetails.map(_.fileName).getOrElse(""),
            fileReference = fileUpload.reference,
            numberOfEntries = None,
            errorType = Some("multiple"),
            volume = Some("moreThan25"),
            stcAuditType = BulkUploadProcessed
          )
        }
      }
    }
  }
}
