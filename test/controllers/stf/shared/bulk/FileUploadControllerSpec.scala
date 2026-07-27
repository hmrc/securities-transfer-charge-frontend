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
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.UpscanInitiateConnector
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.fileUpload.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes.JourneyRecoveryController
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.UpscanJourneyStatus.Failed
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.{FileUpload, UploadRequest, UpscanInitiateResponse}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.UpscanJourneyRepository
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.processing.{FileProcessingRefreshCounter, FileProcessingRefreshCounterFactory}
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.fileUpload.FileUploadView

import scala.concurrent.Future

class FileUploadControllerSpec extends SpecBase with MockitoSugar {

  val reference = "file1"
  val uploadRequest: UploadRequest = UploadRequest(href = "http://someUrl.com", fields = Map("key" -> "1234"))

  val upscanInitiateResponse: UpscanInitiateResponse = UpscanInitiateResponse(reference, uploadRequest)

  "FileUploadController Controller" - {

    Seq(JourneyType.STF, JourneyType.SH03).foreach { journeyType =>

      s"For JourneyType $journeyType" - {

        lazy val fileUploadRoute: String = routes.FileUploadController.onPageLoad(journeyType).url
        lazy val onUploadErrorRoute: String = routes.FileUploadController.onUploadError().url

        "onPageLoad" - {

          "must return OK and the correct view for a GET" in {

            val mockUpscanInitiateConnector = mock[UpscanInitiateConnector]
            val mockCounter = mock[FileProcessingRefreshCounter]
            val mockCounterFactory = mock[FileProcessingRefreshCounterFactory]


            when(mockUpscanInitiateConnector.initiate()(any[HeaderCarrier]())).thenReturn(Future.successful(upscanInitiateResponse))
            when(mockCounterFactory.apply(any())).thenReturn(mockCounter)
            when(mockCounter.reset(any[Result])).thenAnswer(inv => inv.getArgument[Result](0))

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(
                inject.bind[UpscanInitiateConnector].to(mockUpscanInitiateConnector),
                inject.bind[FileProcessingRefreshCounterFactory].to(mockCounterFactory))
              .build()


            running(application) {
              val request = FakeRequest(GET, fileUploadRoute)

              val result = route(application, request).value

              val view = application.injector.instanceOf[FileUploadView]

              status(result) mustEqual OK
              contentAsString(result) mustEqual view(uploadRequest,journeyType = journeyType)(request, messages(application)).toString
              verify(mockCounter).reset(any[Result])
            }
          }
        }


        "onUploadError" - {

          "must delete the mongo document and redirect to the BulkUploadErrorController" in {

            val mockRepository = mock[UpscanJourneyRepository]

            when(mockRepository.find("1234"))
              .thenReturn(Future.successful(Some(FileUpload(reference = "1234",status = Failed,journeyType = journeyType))))
            when(mockRepository.delete("1234"))
              .thenReturn(Future.successful(()))

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(
                inject.bind[UpscanJourneyRepository].toInstance(mockRepository)
              )
              .build()

            running(application) {

              val request =
                FakeRequest(GET, s"$onUploadErrorRoute?key=1234")

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER

              redirectLocation(result).value mustEqual
                routes.BulkUploadErrorController.onPageLoad(journeyType).url

              verify(mockRepository).delete("1234")
            }
          }

          "must redirect to the JourneyRecovery when no key is supplied" in {

            val mockRepository = mock[UpscanJourneyRepository]

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(
                inject.bind[UpscanJourneyRepository].toInstance(mockRepository)
              )
              .build()

            running(application) {

              val request =
                FakeRequest(GET, onUploadErrorRoute)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER

              redirectLocation(result).value mustEqual JourneyRecoveryController.onPageLoad().url

              verify(mockRepository, never()).delete(any[String])
            }
          }
        }
      }
    }
  }
}