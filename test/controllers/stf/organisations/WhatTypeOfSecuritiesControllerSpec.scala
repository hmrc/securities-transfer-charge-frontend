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

package controllers.stf.organisations

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SaveAndReturnClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.routes as orgRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.organisations.WhatTypeOfSecuritiesFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers, WhatTypeOfSecurities}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.WhatTypeOfSecuritiesPage
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.organisations.WhatTypeOfSecuritiesView

import scala.concurrent.Future

class WhatTypeOfSecuritiesControllerSpec extends SpecBase with MockitoSugar {


  lazy val whatTypeOfSecuritiesRoute: String = orgRoutes.WhatTypeOfSecuritiesController.onPageLoad(NormalMode).url

  val formProvider = new WhatTypeOfSecuritiesFormProvider()
  val form: Form[WhatTypeOfSecurities] = formProvider()

  "WhatTypeOfSecurities Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        affinityGroup = AffinityGroup.Organisation
      )
        .overrides(bind[Navigator].qualifiedWith("organisations").toInstance(getNavigator))
        .build()

      running(application) {
        val request = FakeRequest(GET, whatTypeOfSecuritiesRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[WhatTypeOfSecuritiesView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, affinityGroupKeyOrg, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId,submissionId).set(WhatTypeOfSecuritiesPage, WhatTypeOfSecurities.values.head).success.value

      val application = applicationBuilder(
        userAnswers = Some(userAnswers),
        affinityGroup = AffinityGroup.Organisation
      )
        .overrides(bind[Navigator].qualifiedWith("organisations").toInstance(getNavigator))
        .build()

      running(application) {
        val request = FakeRequest(GET, whatTypeOfSecuritiesRoute)

        val view = application.injector.instanceOf[WhatTypeOfSecuritiesView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(WhatTypeOfSecurities.values.head), NormalMode, affinityGroupKeyOrg, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val saveAndReturnClient = mock[SaveAndReturnClient]

      when(saveAndReturnClient.save(any[UserAnswers]())(any[HeaderCarrier]()))
        .thenReturn(Future.successful(()))

      val application =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          affinityGroup = AffinityGroup.Organisation
        )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whatTypeOfSecuritiesRoute)
            .withFormUrlEncodedBody(("value", WhatTypeOfSecurities.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual orgRoutes.DetailsOfThisTransferController.onPageLoad(NormalMode).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        affinityGroup = AffinityGroup.Organisation
      )
        .overrides(bind[Navigator].qualifiedWith("organisations").toInstance(getNavigator))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, whatTypeOfSecuritiesRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[WhatTypeOfSecuritiesView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, affinityGroupKeyOrg, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(
        userAnswers = None,
        affinityGroup = AffinityGroup.Organisation
      ).build()

      running(application) {
        val request = FakeRequest(GET, whatTypeOfSecuritiesRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(
        userAnswers = None,
        affinityGroup = AffinityGroup.Organisation
      ).build()

      running(application) {
        val request =
          FakeRequest(POST, whatTypeOfSecuritiesRoute)
            .withFormUrlEncodedBody(("value", WhatTypeOfSecurities.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
