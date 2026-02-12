/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.AlfAddressConnector
import uk.gov.hmrc.securitiestransferchargefrontend.models.AlfConfirmedAddress
import uk.gov.hmrc.securitiestransferchargefrontend.models.AlfAddress
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import scala.concurrent.Future
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.securitiestransferchargefrontend.models.Country

class AddressControllerSpec extends SpecBase with MockitoSugar {

  "AddressController" - {

    "onPageLoad should redirect to ALF" in {

      val mockAlf = mock[AlfAddressConnector]

      when(mockAlf.initAlfJourneyRequest(any(), any())(any()))
        .thenReturn(Future.successful(Redirect("/alf")))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[AlfAddressConnector].toInstance(mockAlf))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.AddressController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
      }
    }

    "onReturn should retrieve address and redirect" in {

      val mockAlf = mock[AlfAddressConnector]

      val confirmedAddress = AlfConfirmedAddress(
        auditRef = "audit-ref-123",
        id = Some("id"),
        address = AlfAddress(
          lines = List("1 Test Street"),
          postcode = "ZZ1 1ZZ",
          country = Country("United Kingdom", "GB")
        )
      )

      when(mockAlf.alfRetrieveAddress(any())(any()))
        .thenReturn(Future.successful(confirmedAddress))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[AlfAddressConnector].toInstance(mockAlf))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.AddressController.onReturn("addressId").url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
      }
    }

  }
}
