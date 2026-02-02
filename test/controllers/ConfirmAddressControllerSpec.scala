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
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.SubscriptionConnector
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{ConfirmableAddress, Country}
import uk.gov.hmrc.securitiestransferchargefrontend.services.AddressService
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.ConfirmAddressView

import scala.concurrent.Future

class ConfirmAddressControllerSpec extends SpecBase {

  "ConfirmAddress Controller" - {

    "must return OK and the correct view for a GET" in {

      val confirmableAddress = ConfirmableAddress(
        lines = List(
          "1 High Street",
          "Town"
        ),
        postcode = "ZZ1 1ZZ",
        country = Some(Country("United Kingdom", "GB"))
      )

      val mockSubscriptionConnector = mock[SubscriptionConnector]
      val mockAddressService = mock[AddressService]

      when(mockSubscriptionConnector.getValidSubscription(any())(any()))
        .thenReturn(Future.successful(subscription))
      when(mockAddressService.extractConfirmableAddress(subscription)).thenReturn(confirmableAddress)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
            inject.bind[AddressService].toInstance(mockAddressService)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.ConfirmAddressController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ConfirmAddressView]


        status(result) mustEqual OK
        contentAsString(result) mustEqual view(confirmableAddress)(request, messages(application)).toString
      }
    }
  }
}
