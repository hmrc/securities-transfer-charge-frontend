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

package controllers.stf.organisations.single

import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.single.routes as orgSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode

class StfSellerAddressControllerSpec extends SpecBase with MockitoSugar {

  "AddressController" - {

    "onPageLoad should redirect to ALF" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .build()

      running(application) {
        val request = FakeRequest(GET, orgSingleRoutes.StfSellerAddressController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
      }
    }

    "onReturn should retrieve address and redirect" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .build()

      running(application) {
        val request = FakeRequest(GET, orgSingleRoutes.StfSellerAddressController.onReturn("addressId").url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual orgSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode).url
      }
    }
  }
}
