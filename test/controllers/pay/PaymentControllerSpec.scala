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

package controllers.pay

import base.SpecBase
import base.Fixtures.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.PayApiConnector
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.pay.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.pay.StartPaymentJourneyResponse
import uk.gov.hmrc.securitiestransferchargefrontend.services.DashboardService

import scala.concurrent.Future

class PaymentControllerSpec extends SpecBase {

  "PaymentController" - {

    "must redirect to the OPS nextUrl when starting a payment journey succeeds" in {

      val mockDashboardService = mock[DashboardService]
      val mockPayApiConnector  = mock[PayApiConnector]

      val nextUrl = "http://localhost:9056/pay/initiate-journey?traceId=123"
      val expectedResponse = StartPaymentJourneyResponse("journey-123", nextUrl)

      when(mockDashboardService.getSubmission(any(), any())(any()))
        .thenReturn(Future.successful(singleSubmissionReadyToPay))

      when(mockPayApiConnector.startPaymentJourney(any())(any()))
        .thenReturn(Future.successful(expectedResponse))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[DashboardService].toInstance(mockDashboardService),
          bind[PayApiConnector].toInstance(mockPayApiConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.PaymentController.startPayment(testSubmissionId.toString).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual nextUrl
      }
    }

    "must fail the future when the DashboardService fails to retrieve the submission" in {

      val mockDashboardService = mock[DashboardService]
      val mockPayApiConnector  = mock[PayApiConnector]

      when(mockDashboardService.getSubmission(any(), any())(any()))
        .thenReturn(Future.failed(new RuntimeException("Database error")))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[DashboardService].toInstance(mockDashboardService),
          bind[PayApiConnector].toInstance(mockPayApiConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.PaymentController.startPayment(testSubmissionId.toString).url)
        val result = route(application, request).value

        whenReady(result.failed) { exception =>
          exception mustBe a[RuntimeException]
          exception.getMessage mustEqual "Database error"
        }
      }
    }

    "must fail the future when the PayApiConnector fails to create the journey" in {

      val mockDashboardService = mock[DashboardService]
      val mockPayApiConnector  = mock[PayApiConnector]

      when(mockDashboardService.getSubmission(any(), any())(any()))
        .thenReturn(Future.successful(singleSubmissionReadyToPay))

      when(mockPayApiConnector.startPaymentJourney(any())(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("Unauthorized", UNAUTHORIZED)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[DashboardService].toInstance(mockDashboardService),
          bind[PayApiConnector].toInstance(mockPayApiConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.PaymentController.startPayment(testSubmissionId.toString).url)
        val result = route(application, request).value

        whenReady(result.failed) { exception =>
          exception mustBe a[UpstreamErrorResponse]
          exception.asInstanceOf[UpstreamErrorResponse].statusCode mustEqual UNAUTHORIZED
        }
      }
    }
  }
}