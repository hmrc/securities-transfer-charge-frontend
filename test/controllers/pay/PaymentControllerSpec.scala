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
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.PayApiConnector
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.pay.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes as appRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.pay.{BasketItem, OPSChargeType, StartPaymentJourneyRequest, StartPaymentJourneyResponse}
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.{EtmpChargeDetail, SttSubmission}
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

    "transfer and charge payments" - {

      val nextUrl = "http://localhost:9056/pay/initiate-journey?traceId=456"

      def chargeWith(chargeType: String, ref: String, pending: BigDecimal): EtmpChargeDetail =
        buildTaxCharge(testUtrn, false, false).copy(chargeType = chargeType, chargeReference = ref, chargeAmountPending = pending)

      val otherTransfer = readyToPaySttTransferTaxOnly.copy(
        utrn = "900459000001",
        buyerNames = "Other Buyer",
        charges = Seq(chargeWith("STT", "OTHER-TAX", 999))
      )
      // Transfers are numbered in UTRN order, so this one is transfer-2.
      val transfer = readyToPaySttTransferTaxOnly.copy(
        utrn = "900459000002",
        buyerNames = "Buyer Two",
        sellerNames = None,
        charges = Seq(
          chargeWith("STT", "TAX-2", 0),          // already paid
          chargeWith("LFP", "LFP-2", 50),
          chargeWith("LPI", "LPI-2", 12.34)
        )
      )
      val submission: SttSubmission = singleSubmissionReadyToPay.copy(transfers = Seq(otherTransfer, transfer))

      def run(url: String) = {
        val mockDashboardService = mock[DashboardService]
        val mockPayApiConnector  = mock[PayApiConnector]
        when(mockDashboardService.getSubmission(any(), any())(any())).thenReturn(Future.successful(submission))
        when(mockPayApiConnector.startPaymentJourney(any())(any()))
          .thenReturn(Future.successful(StartPaymentJourneyResponse("journey-456", nextUrl)))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[DashboardService].toInstance(mockDashboardService),
            bind[PayApiConnector].toInstance(mockPayApiConnector)
          )
          .build()

        running(application) {
          val result = route(application, FakeRequest(GET, url)).value
          (status(result), redirectLocation(result), mockPayApiConnector)
        }
      }

      def sentBasket(connector: PayApiConnector): Seq[BasketItem] = {
        val captor = ArgumentCaptor.forClass(classOf[StartPaymentJourneyRequest])
        verify(connector).startPaymentJourney(captor.capture())(any())
        captor.getValue.basketDetails.basketItems
      }

      "pay full balance sends only the outstanding charges on that transfer" in {
        val (status, location, connector) =
          run(routes.PaymentController.startTransferPayment(testSubmissionId.toString, 2).url)

        status mustEqual SEE_OTHER
        location.value mustEqual nextUrl
        sentBasket(connector) mustBe Seq(
          BasketItem("Buyer Two", "Not provided", "LFP-2", OPSChargeType.LatePaymentPenalty, 5000L),
          BasketItem("Buyer Two", "Not provided", "LPI-2", OPSChargeType.LatePaymentInterest, 1234L)
        )
      }

      "a pay link sends only that charge" in {
        val (status, location, connector) =
          run(routes.PaymentController.startChargePayment(testSubmissionId.toString, 2, "LPI-2").url)

        status mustEqual SEE_OTHER
        location.value mustEqual nextUrl
        sentBasket(connector) mustBe Seq(
          BasketItem("Buyer Two", "Not provided", "LPI-2", OPSChargeType.LatePaymentInterest, 1234L)
        )
      }

      "a pay link for a charge that is already paid goes to Journey Recovery without calling OPS" in {
        val (status, location, connector) =
          run(routes.PaymentController.startChargePayment(testSubmissionId.toString, 2, "TAX-2").url)

        status mustEqual SEE_OTHER
        location.value mustEqual appRoutes.JourneyRecoveryController.onPageLoad().url
        verify(connector, never()).startPaymentJourney(any())(any())
      }

      "a pay link for a charge on a different transfer goes to Journey Recovery" in {
        val (status, location, connector) =
          run(routes.PaymentController.startChargePayment(testSubmissionId.toString, 2, "OTHER-TAX").url)

        status mustEqual SEE_OTHER
        location.value mustEqual appRoutes.JourneyRecoveryController.onPageLoad().url
        verify(connector, never()).startPaymentJourney(any())(any())
      }

      "pay full balance for a transfer number that does not exist goes to Journey Recovery" in {
        val (status, location, connector) =
          run(routes.PaymentController.startTransferPayment(testSubmissionId.toString, 3).url)

        status mustEqual SEE_OTHER
        location.value mustEqual appRoutes.JourneyRecoveryController.onPageLoad().url
        verify(connector, never()).startPaymentJourney(any())(any())
      }

      "paying the whole submission leaves out charges that are already paid" in {
        val (_, _, connector) = run(routes.PaymentController.startPayment(testSubmissionId.toString).url)

        sentBasket(connector).map(_.chargeReference) mustBe Seq("OTHER-TAX", "LFP-2", "LPI-2")
      }
    }
  }
}
