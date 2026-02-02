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
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.clients.registration.Subscription
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.SubscriptionConnector
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.ConfirmAddressView
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{ConfirmableAddress, Country}

import java.time.LocalDateTime
import scala.concurrent.Future

class ConfirmAddressControllerSpec extends SpecBase {

  "ConfirmAddress Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockSubscriptionConnector = mock[SubscriptionConnector]
      val subscription = Subscription(
        subsValidTo = LocalDateTime.now(),
        contactName = "John Doe",
        addressLine1 = "1 High Street",
        addressLine2 = None,
        addressLine3 = Some("Town"),
        postcode = "ZZ1 1ZZ",
        countryCode = "GB",
        telephoneNumber = "07777777777",
        emailAddress = "test@test.com"
      )

      when(mockSubscriptionConnector.getSubscriptionDetails(any())(any()))
        .thenReturn(Future.successful(subscription))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.ConfirmAddressController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ConfirmAddressView]


        val expectedAddress = ConfirmableAddress(
          lines = List(
            "1 High Street",
            "Town"
          ),
          postcode = "ZZ1 1ZZ",
          country = Some(Country("United Kingdom", "GB"))
        )

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(expectedAddress)(request, messages(application)).toString
      }
    }
  }
}
