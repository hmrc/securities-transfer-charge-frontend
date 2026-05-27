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

package controllers.stf.shared.bulk

import base.{FileUploadFixtures, SpecBase}
import org.mockito.ArgumentMatchers.{any as anyArg, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.{Application, inject}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.{SubscriptionConnector, SubscriptionStatusErrorException, UpscanDownloadException}
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.bulk.routes as bulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{FileParseError, StcRowValidationError}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.{FileUpload, UpscanJourneyStatus}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.UpscanJourneyRepository
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcUpscanProcessingService
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.shared.bulk.FileUploadedView

import scala.concurrent.Future

class FileUploadedControllerSpec extends SpecBase with MockitoSugar with FileUploadFixtures with BeforeAndAfterEach {

  private val mockRepository = mock[UpscanJourneyRepository]
  private val mockStcUpscanProcessingService = mock[StcUpscanProcessingService]
  private val mockSubscriptionConnector = mock[SubscriptionConnector]

  private val testKey = "test-key"

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRepository, mockStcUpscanProcessingService, mockSubscriptionConnector)
  }

  private val testFileUpload = FileUpload(
    reference = "ref123",
    status = UpscanJourneyStatus.Ready,
    downloadUrl = Some("http://download"),
    uploadDetails = None,
    failureReason = None,
    message = None
  )

  private def application: Application =
    applicationBuilder()
      .overrides(
        inject.bind[UpscanJourneyRepository].toInstance(mockRepository),
        inject.bind[StcUpscanProcessingService].toInstance(mockStcUpscanProcessingService),
        inject.bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
      )
      .build()

  private def fileUploadedRoute(key: String): String =
    bulkRoutes.FileUploadedController.onPageLoad(key).url

  "FileUploadedController" - {

    "must return OK and the correct view when file upload is found and processing succeeds" in {

      when(mockRepository.find(anyArg()))
        .thenReturn(Future.successful(Some(testFileUpload)))

      when(mockStcUpscanProcessingService.process(eqTo(testFileUpload),eqTo(affinityGroupKeyInd))(using anyArg[HeaderCarrier]))
        .thenReturn(Future.successful(Right(successfulValidationResponse)))

      when(mockSubscriptionConnector.getAndStoreSubscription(anyArg())(using anyArg[HeaderCarrier]))
        .thenReturn(Future.successful(subscription))

      val app = application

      running(app) {
        val request = FakeRequest(GET, fileUploadedRoute(testKey))
        val result = route(app, request).value

        val view = app.injector.instanceOf[FileUploadedView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(testFileUpload)(request, messages(app)).toString

        verify(mockSubscriptionConnector).getAndStoreSubscription(anyArg())(using anyArg[HeaderCarrier])
      }
    }

    "must redirect to the uploaded file error page when blocking errors <= 25" in {

      val validationResponse = validationResponseWithErrors(
        Seq(
          StcRowValidationError(
            rowNumber = 6,
            fieldName = "sellerName",
            columnIndex = 7,
            message = "Enter the seller's full name",
            blocking = true
          )
        )
      )

      when(mockRepository.find(anyArg()))
        .thenReturn(Future.successful(Some(testFileUpload)))

      when(mockStcUpscanProcessingService.process(eqTo(testFileUpload),eqTo(affinityGroupKeyInd))(using anyArg[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validationResponse)))

      val app = application

      running(app) {
        val request = FakeRequest(GET, fileUploadedRoute(testKey))
        val result = route(app, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe bulkRoutes.UploadedFileErrorController.onPageLoad(testKey).url

        verify(mockSubscriptionConnector, never()).getAndStoreSubscription(anyArg())(using anyArg[HeaderCarrier])
      }
    }

    "must redirect to the formatting error page when blocking errors > 25" in {

      val validationResponse = validationResponseWithErrors(withBlockingErrors(26))

      when(mockRepository.find(anyArg()))
        .thenReturn(Future.successful(Some(testFileUpload)))

      when(mockStcUpscanProcessingService.process(eqTo(testFileUpload),eqTo(affinityGroupKeyInd))(using anyArg[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validationResponse)))

      val app = application

      running(app) {
        val request = FakeRequest(GET, fileUploadedRoute(testKey))
        val result = route(app, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe bulkRoutes.FormattingErrorController.onPageLoad().url

        verify(mockSubscriptionConnector, never()).getAndStoreSubscription(anyArg())(using anyArg[HeaderCarrier])
      }
    }

    //TODO we need the correct  page for this it should not go to formatting error page
    "must redirect to the journey recovery page when file upload is found and processing returns a parse error" in {

      when(mockRepository.find(anyArg()))
        .thenReturn(Future.successful(Some(testFileUpload)))

      when(mockStcUpscanProcessingService.process(eqTo(testFileUpload),eqTo(affinityGroupKeyInd))(using anyArg[HeaderCarrier]))
        .thenReturn(Future.successful(Left(FileParseError.UnsupportedMimeType("application/pdf"))))

      val app = application

      running(app) {
        val request = FakeRequest(GET, fileUploadedRoute(testKey))
        val result = route(app, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url

        verify(mockSubscriptionConnector, never()).getAndStoreSubscription(anyArg())(using anyArg[HeaderCarrier])
      }
    }

    "must return OK and the correct view when file upload is found but is not ready" in {

      val initiatedUpload = testFileUpload.copy(status = UpscanJourneyStatus.Initiated)

      when(mockRepository.find(anyArg()))
        .thenReturn(Future.successful(Some(initiatedUpload)))

      val app = application

      running(app) {
        val request = FakeRequest(GET, fileUploadedRoute(testKey))
        val result = route(app, request).value

        val view = app.injector.instanceOf[FileUploadedView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(initiatedUpload)(request, messages(app)).toString

        verify(mockStcUpscanProcessingService, never()).process(anyArg(),anyArg())(using anyArg[HeaderCarrier])
      }
    }

    "must redirect to JourneyRecovery when file upload is not found" in {

      when(mockRepository.find(anyArg()))
        .thenReturn(Future.successful(None))

      val app = application

      running(app) {
        val request = FakeRequest(GET, fileUploadedRoute(testKey))
        val result = route(app, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must  redirect to JourneyRecovery for an expired subscription" in {

      when(mockRepository.find(anyArg()))
        .thenReturn(Future.successful(Some(testFileUpload)))

      when(mockStcUpscanProcessingService.process(eqTo(testFileUpload),eqTo(affinityGroupKeyInd))(using anyArg[HeaderCarrier]))
        .thenReturn(Future.successful(Right(successfulValidationResponse)))

      when(mockSubscriptionConnector.getAndStoreSubscription(anyArg())(using anyArg[HeaderCarrier]))
        .thenReturn(Future.failed(new SubscriptionStatusErrorException("expired")))

      val app = application

      running(app) {
        val request = FakeRequest(GET, fileUploadedRoute(testKey))
        val result = route(app, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to the EncryptedFileErrorController when an encrypted file is quarantined" in {

      val encryptedUpload = testFileUpload.copy(
        status = UpscanJourneyStatus.Failed,
        failureReason = Some("QUARANTINE"),
        message = Some("PUA.Doc.Packed.EncryptedDoc-6563700-0")
      )

      when(mockRepository.find(anyArg()))
        .thenReturn(Future.successful(Some(encryptedUpload)))

      val app = application

      running(app) {
        val request = FakeRequest(GET, fileUploadedRoute(testKey))
        val result = route(app, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe bulkRoutes.EncryptedFileErrorController.onPageLoad().url

        verify(mockStcUpscanProcessingService, never()).process(anyArg(),anyArg())(using anyArg[HeaderCarrier])
      }
    }

    "must redirect to the BulkUploadVirusErrorController when an uploaded file has a virus" in {

      val encryptedUpload = testFileUpload.copy(
        status = UpscanJourneyStatus.Failed,
        failureReason = Some("QUARANTINE"),
        message = Some("e.g. This file has a virus")
      )

      when(mockRepository.find(anyArg()))
        .thenReturn(Future.successful(Some(encryptedUpload)))

      val app = application

      running(app) {
        val request = FakeRequest(GET, fileUploadedRoute(testKey))
        val result = route(app, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe bulkRoutes.BulkUploadVirusErrorController.onPageLoad().url

        verify(mockStcUpscanProcessingService, never()).process(anyArg(),anyArg())(using anyArg[HeaderCarrier])
      }
    }

    "must redirect to the empty file error page when file upload is found and processing returns an EmptyFile error" in {

      when(mockRepository.find(anyArg()))
        .thenReturn(Future.successful(Some(testFileUpload)))

      when(mockStcUpscanProcessingService.process(eqTo(testFileUpload),eqTo(affinityGroupKeyInd))(using anyArg[HeaderCarrier]))
        .thenReturn(Future.successful(Left(FileParseError.EmptyFile)))

      val app = application

      running(app) {
        val request = FakeRequest(GET, fileUploadedRoute(testKey))
        val result = route(app, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe bulkRoutes.BulkUploadFileEmptyController.onPageLoad().url

        verify(mockSubscriptionConnector, never()).getAndStoreSubscription(anyArg())(using anyArg[HeaderCarrier])
      }
    }
  }

  "must redirect to the too many rows error page when the uploaded file contains too many rows" in {

    when(mockRepository.find(anyArg()))
      .thenReturn(Future.successful(Some(testFileUpload)))

    when(mockStcUpscanProcessingService.process(eqTo(testFileUpload),eqTo(affinityGroupKeyInd))(using anyArg[HeaderCarrier]))
      .thenReturn(Future.successful(Left(FileParseError.RowLimitExceeded(actual = 1000, max = 50))))

    val app = application

    running(app) {
      val request = FakeRequest(GET, fileUploadedRoute(testKey))
      val result = route(app, request).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe bulkRoutes.BulkRowsErrorController.onPageLoad().url

      verify(mockSubscriptionConnector, never()).getAndStoreSubscription(anyArg())(using anyArg[HeaderCarrier])
    }
  }


  "must redirect to the upload error page for an upscan download exception" in {

    when(mockRepository.find(anyArg()))
      .thenReturn(Future.successful(Some(testFileUpload)))

    when(mockStcUpscanProcessingService.process(eqTo(testFileUpload),eqTo(affinityGroupKeyInd))(using anyArg[HeaderCarrier]))
      .thenReturn(Future.failed(
        UpscanDownloadException("Failed to download uploaded file",
          new RuntimeException("Connection timeout")))
      )

    val app = application

    running(app) {
      val request = FakeRequest(GET, fileUploadedRoute(testKey))
      val result = route(app, request).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe bulkRoutes.BulkUploadErrorController.onPageLoad().url

      verify(mockSubscriptionConnector, never()).getAndStoreSubscription(anyArg())(using anyArg[HeaderCarrier])
    }
  }
}