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

package controllers.sh03.agents.single

import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.single.routes as agentSh03Routes
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.shared.ChargingPointFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.ChargingPointPage
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.agents.single.ChargingPointView

import java.time.{LocalDate, ZoneOffset}

class ChargingPointControllerSpec extends SpecBase with MockitoSugar {

  private implicit val messages: Messages = stubMessages()

  private val formProvider = new ChargingPointFormProvider()
  private def form = formProvider(affinityGroupKeyAgent)
  
  val validAnswer: LocalDate = LocalDate.now(ZoneOffset.UTC)

  lazy val chargingPointRoute: String = agentSh03Routes.ChargingPointController.onPageLoad(NormalMode).url

  override val emptyUserAnswers: UserAnswers = UserAnswers(testUserId, testGroupIdentifier, submissionId)

  def getRequest(): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, chargingPointRoute)

  def postRequest(): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, chargingPointRoute)
      .withFormUrlEncodedBody(
        "value.day"   -> validAnswer.getDayOfMonth.toString,
        "value.month" -> validAnswer.getMonthValue.toString,
        "value.year"  -> validAnswer.getYear.toString
      )

  "ChargingPoint Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = agentAffinity)
        .overrides(bind[Navigator].qualifiedWith("agentsSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val result = route(application, getRequest()).value

        val view = application.injector.instanceOf[ChargingPointView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, testBackLinkRoute)(getRequest(), messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(testUserId, testGroupIdentifier, submissionId).set(ChargingPointPage, validAnswer).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers), affinityGroup = agentAffinity)
        .overrides(bind[Navigator].qualifiedWith("agentsSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val view = application.injector.instanceOf[ChargingPointView]

        val result = route(application, getRequest()).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validAnswer), NormalMode, testBackLinkRoute)(getRequest(), messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = agentAffinity)
        .overrides(bind[Navigator].qualifiedWith("agentsSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val result = route(application, postRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual testNextPage.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = agentAffinity)
        .overrides(bind[Navigator].qualifiedWith("agentsSh03").toInstance(getNavigator))
        .build()

      val request =
        FakeRequest(POST, chargingPointRoute)
          .withFormUrlEncodedBody(("value", "invalid value"))

      running(application) {
        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[ChargingPointView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, affinityGroup = agentAffinity).build()

      running(application) {
        val result = route(application, getRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, affinityGroup = agentAffinity).build()

      running(application) {
        val result = route(application, postRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
