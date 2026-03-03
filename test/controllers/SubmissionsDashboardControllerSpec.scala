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
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.ArgumentMatchers.any
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.clients.{SaveAndReturnClient, SubmissionIdClient}
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.SubmissionsDashboardView

import scala.concurrent.Future

class SubmissionsDashboardControllerSpec extends SpecBase with MockitoSugar {

  lazy val submissionsDashboardRoute: String =
    routes.SubmissionsDashboardController.onPageLoad().url

  lazy val submissionsDashboardSubmitRoute: String =
    routes.SubmissionsDashboardController.onSubmit().url

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

          verify(mockSaveAndReturnClient).list(any[String])(any())
        }
      }

      "must return OK and render submission IDs when they exist" in {

        val mockSaveAndReturnClient = mock[SaveAndReturnClient]

        val submissionIds = List(
          SubmissionId("STC-123456789"),
          SubmissionId("STC-987654321")
        )

        when(mockSaveAndReturnClient.list(any[String])(any()))
          .thenReturn(Future.successful(submissionIds))

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
            view(submissionIds)(
              request,
              messages(application)
            ).toString

          verify(mockSaveAndReturnClient).list(any[String])(any())
        }
      }
    }

    "POST onSubmit" - {

      "must generate a submissionId and redirect using navigator" in {

        val mockIdClient = mock[SubmissionIdClient]

        val generatedSubmissionId = SubmissionId("STC-111111111")

        when(mockIdClient.nextSubmissionId()(any()))
          .thenReturn(Future.successful(generatedSubmissionId))

        val application =
          applicationBuilder()
            .overrides(
              bind[SubmissionIdClient].toInstance(mockIdClient),
              bind[Navigator].qualifiedWith("individuals").toInstance(getNavigator),
              bind[Navigator].qualifiedWith("organisations").toInstance(getNavigator)
            )
            .build()

        running(application) {

          val request = FakeRequest(POST, submissionsDashboardSubmitRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual testNextPage.url

          verify(mockIdClient).nextSubmissionId()(any())
        }
      }


      "must generate a submissionId and redirect when affinity group is Organisation" in {

        val mockIdClient = mock[SubmissionIdClient]

        val generatedSubmissionId = SubmissionId("STC-222222222")

        when(mockIdClient.nextSubmissionId()(any()))
          .thenReturn(Future.successful(generatedSubmissionId))

        val application =
          applicationBuilder()
            .configure("test.affinityGroup" -> "Organisation")
            .overrides(
              bind[SubmissionIdClient].toInstance(mockIdClient),
              bind[Navigator].qualifiedWith("individuals").toInstance(getNavigator),
              bind[Navigator].qualifiedWith("organisations").toInstance(getNavigator)
            )
            .build()

        running(application) {

          val request = FakeRequest(POST, submissionsDashboardSubmitRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual testNextPage.url

          verify(mockIdClient).nextSubmissionId()(any())
        }
      }


      "must fail when submission ID generation fails" in {

        val mockIdClient = mock[SubmissionIdClient]

        when(mockIdClient.nextSubmissionId()(any()))
          .thenReturn(Future.failed(new RuntimeException("exception")))

        val application =
          applicationBuilder()
            .overrides(
              bind[SubmissionIdClient].toInstance(mockIdClient)
            )
            .build()

        running(application) {

          val request = FakeRequest(POST, submissionsDashboardSubmitRoute)

          val thrown = intercept[RuntimeException] {
            await(route(application, request).value)
          }

          thrown.getMessage mustBe "exception"
        }
      }
    }
  }
}