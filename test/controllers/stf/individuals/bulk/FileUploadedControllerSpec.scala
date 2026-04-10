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

package controllers.stf.individuals.bulk

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.{Application, inject}
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.bulk.routes as individualRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.{FileUpload, UpscanJourneyStatus}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.UpscanJourneyRepository
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.bulk.FileUploadedView

import scala.concurrent.Future

class FileUploadedControllerSpec extends SpecBase with MockitoSugar {

  private val mockRepository = mock[UpscanJourneyRepository]
  private val mockStcUpscanProcessingService = mock[StcUpscanProcessingService]

  private val testKey = "test-key"

  private val testFileUpload = FileUpload(
    reference = "ref123",
    status = UpscanJourneyStatus.Ready,
    downloadUrl = Some("http://download"),
    uploadDetails = None,
    failureReason = None,
    message = None
  )

  private val validationResponse = StcFileValidationResponse(
    rows = Seq.empty
  )

  private def application: Application =
    applicationBuilder()
      .overrides(
        inject.bind[UpscanJourneyRepository].toInstance(mockRepository),
        inject.bind[StcUpscanProcessingService].toInstance(mockStcUpscanProcessingService)
      )
      .build()

  private def fileUploadedRoute(key: String): String =
    individualRoutes.FileUploadedController.onPageLoad(key).url

  "FileUploadedController" - {

    "must return OK and the correct view when file upload is found and processing succeeds" in {

      when(mockRepository.find(any()))
        .thenReturn(Future.successful(Some(testFileUpload)))

      when(mockStcUpscanProcessingService.process(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validationResponse)))

      val app = application

      running(app) {
        val request = FakeRequest(GET, fileUploadedRoute(testKey))
        val result  = route(app, request).value

        val view = app.injector.instanceOf[FileUploadedView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(testFileUpload)(request, messages(app)).toString
      }
    }

    "must redirect to the formatting error page when file upload is found and processing returns a parse error" in {

      when(mockRepository.find(any()))
        .thenReturn(Future.successful(Some(testFileUpload)))

      when(mockStcUpscanProcessingService.process(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(FileParseError.UnsupportedMimeType("application/pdf"))))

      val app = application

      running(app) {
        val request = FakeRequest(GET, fileUploadedRoute(testKey))
        val result  = route(app, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe individualRoutes.FormattingErrorController.onPageLoad().url
      }
    }

    "must return OK and the correct view when file upload is found but is not ready" in {

      val initiatedUpload = testFileUpload.copy(status = UpscanJourneyStatus.Initiated)

      when(mockRepository.find(any()))
        .thenReturn(Future.successful(Some(initiatedUpload)))

      val app = application

      running(app) {
        val request = FakeRequest(GET, fileUploadedRoute(testKey))
        val result  = route(app, request).value

        val view = app.injector.instanceOf[FileUploadedView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(initiatedUpload)(request, messages(app)).toString
      }
    }

    "must redirect to JourneyRecovery when file upload is not found" in {

      when(mockRepository.find(any()))
        .thenReturn(Future.successful(None))

      val app = application

      running(app) {
        val request = FakeRequest(GET, fileUploadedRoute(testKey))
        val result  = route(app, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}