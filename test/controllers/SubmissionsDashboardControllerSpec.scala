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

package controllers

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SaveAndReturnClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.SubmissionsDashboardView

import scala.concurrent.Future
import java.time.Instant

class SubmissionsDashboardControllerSpec extends SpecBase with MockitoSugar {

  lazy val submissionsDashboardRoute: String =
    routes.SubmissionsDashboardController.onPageLoad().url

  "SubmissionsDashboardController" - {

    "GET onPageLoad" - {

      "must return OK and the correct view when no submissions exist" in {

        val mockSaveAndReturnClient = mock[SaveAndReturnClient]

        when(mockSaveAndReturnClient.list(any[String])(any()))
          .thenReturn(Future.successful(List.empty))

        val application =
          applicationBuilder(
            saveAndReturnClient = mockSaveAndReturnClient
          ).build()

        running(application) {
          val request = FakeRequest(GET, submissionsDashboardRoute)

          val result = route(application, request).value
          val view = application.injector.instanceOf[SubmissionsDashboardView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual
            view(List.empty)(
              request,
              messages(application)
            ).toString
        }
      }
      
      "must return OK and render submissions when they exist" in {

        val mockSaveAndReturnClient = mock[SaveAndReturnClient]

        val userAnswers =
          UserAnswers(
            userId = userId,
            submissionId = submissionId,
            data = Json.obj(),
            lastUpdated = Instant.now()
          )

        when(mockSaveAndReturnClient.list(any[String])(any()))
          .thenReturn(Future.successful(List(submissionId)))

        when(mockSaveAndReturnClient.retrieve(any[String], any())(any()))
          .thenReturn(Future.successful(userAnswers))

        val application =
          applicationBuilder(
            saveAndReturnClient = mockSaveAndReturnClient
          ).build()

        running(application) {
          val request = FakeRequest(GET, submissionsDashboardRoute)

          val result = route(application, request).value
          val view = application.injector.instanceOf[SubmissionsDashboardView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual
            view(List(userAnswers))(
              request,
              messages(application)
            ).toString
        }
      }
    }
  }
}
