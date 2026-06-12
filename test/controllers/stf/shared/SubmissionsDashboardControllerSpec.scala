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

package controllers.stf.shared

import base.Fixtures.{testCredentialId, testSubmissionId}
import base.{AuditTestSupport, SpecBase}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.securitiestransferchargefrontend.clients.{SaveAndReturnClient, SubmissionIdClient}
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{SubmissionId, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.audit.JourneyStatus.StartSubmission
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.services.AuditService
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.shared.SubmissionsDashboardView

import scala.concurrent.Future

class SubmissionsDashboardControllerSpec extends SpecBase with MockitoSugar with AuditTestSupport {

  lazy val submissionsDashboardRoute: String =
    routes.SubmissionsDashboardController.onPageLoad().url

  lazy val submissionsDashboardSubmitRoute: String =
    routes.SubmissionsDashboardController.onSubmit().url

  "SubmissionsDashboardController" - {

    "GET onPageLoad" - {

      "must return OK and the correct view when no submissions exist" in {

        val mockSaveAndReturnClient = mock[SaveAndReturnClient]

        when(mockSaveAndReturnClient.listByUser(any[UserId])(any()))
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

          verify(mockSaveAndReturnClient).listByUser(any[UserId])(any())
        }
      }

      "must return OK and render submission IDs when they exist" in {

        val mockSaveAndReturnClient = mock[SaveAndReturnClient]

        val submissionIds = List(
          SubmissionId("STC-123456789"),
          SubmissionId("STC-987654321")
        )

        when(mockSaveAndReturnClient.listByUser(any[UserId])(any()))
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

          verify(mockSaveAndReturnClient).listByUser(any[UserId])(any())
        }
      }
    }

    "POST onSubmit" - {

      Seq(
        AffinityGroup.Individual,
        AffinityGroup.Organisation,
        AffinityGroup.Agent
      ).foreach { affinityGroup =>

        s"must generate submissionId, audit event and redirect for $affinityGroup" in {

          val mockIdClient = mock[SubmissionIdClient]
          val mockAuditService = mock[AuditService]

          val generatedSubmissionId = testSubmissionId

          when(mockIdClient.nextSubmissionId()(any())).thenReturn(Future.successful(generatedSubmissionId))

          val application =
            applicationBuilder(affinityGroup = affinityGroup)
              .overrides(
                bind[SubmissionIdClient].toInstance(mockIdClient),
                bind[AuditService].toInstance(mockAuditService),
                bind[Navigator].qualifiedWith("individuals").toInstance(getNavigator),
                bind[Navigator].qualifiedWith("organisations").toInstance(getNavigator),
                bind[Navigator].qualifiedWith("agents").toInstance(getNavigator)
              )
              .build()

          running(application) {

            val request = FakeRequest(POST, submissionsDashboardSubmitRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual testNextPage.url

            verify(mockIdClient).nextSubmissionId()(any())

            verifyAudit(mockAuditService, StartSubmission, affinityGroup, testCredentialId, testSubmissionId)
          }
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