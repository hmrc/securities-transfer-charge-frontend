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

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.UpscanInitiateConnector
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.bulk.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.{UploadRequest, UpscanInitiateResponse}
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.shared.bulk.FileUploadView

import scala.concurrent.Future

class FileUploadControllerSpec extends SpecBase with MockitoSugar {

  lazy val fileUploadRoute: String = routes.FileUploadController.onPageLoad().url
  lazy val onUploadErrorRoute: String = routes.FileUploadController.onUploadError().url
  val reference = "file1"
  val uploadRequest: UploadRequest = UploadRequest(href = "http://someUrl.com", fields = Map("key" -> "1234"))

  val upscanInitiateResponse: UpscanInitiateResponse = UpscanInitiateResponse(reference, uploadRequest)

  "FileUploadController Controller" - {

    "onPageLoad" - {

      "must return OK and the correct view for a GET" in {

        val mockUpscanInitiateConnector = mock[UpscanInitiateConnector]


        when(mockUpscanInitiateConnector.initiate()(any[HeaderCarrier]())).thenReturn(Future.successful(upscanInitiateResponse))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(inject.bind[UpscanInitiateConnector].to(mockUpscanInitiateConnector))
          .build()


        running(application) {
          val request = FakeRequest(GET, fileUploadRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[FileUploadView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(uploadRequest)(request, messages(application)).toString
        }
      }
    }


    "onUploadError" - {

      "must return BAD_REQUEST and render the view with error message" in {

        val mockConnector = mock[UpscanInitiateConnector]

        val reference = "file1"
        val uploadRequest =
          UploadRequest("http://someUrl.com", Map("key" -> "1234"))

        val initiateResponse =
          UpscanInitiateResponse(reference, uploadRequest)

        when(mockConnector.initiate()(any[HeaderCarrier])).thenReturn(Future.successful(initiateResponse))


        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[UpscanInitiateConnector].toInstance(mockConnector),
          )
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            s"$onUploadErrorRoute?errorCode=EntityTooLarge&errorMessage=some+message"
          )

          val expectedError = Some("The selected file must be smaller than 1GB")

          val result = route(application, request).value


          val view = application.injector.instanceOf[FileUploadView]

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(uploadRequest, expectedError)(
            request,
            messages(application)
          ).toString
        }
      }
    }
  }
}
