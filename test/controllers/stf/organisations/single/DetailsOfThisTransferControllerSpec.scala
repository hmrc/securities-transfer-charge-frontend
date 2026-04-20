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
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.single.routes as orgSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.shared.DetailsOfThisTransferFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.DetailsOfThisTransfer
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.individuals.single.ConnectedPersonsPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.DetailsOfThisTransferPage
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.organisations.single.DetailsOfThisTransferView

class DetailsOfThisTransferControllerSpec extends SpecBase with MockitoSugar {

  val formProvider = new DetailsOfThisTransferFormProvider()
  val form: Form[DetailsOfThisTransfer] = formProvider()

  lazy val detailsOfThisTransferRoute: String = orgSingleRoutes.DetailsOfThisTransferController.onPageLoad(NormalMode).url

  val amount: BigDecimal = BigDecimal(500)

  val detailsOfThisTransfer: DetailsOfThisTransfer = DetailsOfThisTransfer(numberOfShares = "25",
    typeOfShares = "stocks",
    amountPaid = BigDecimal(100),
    marketValue = Some(BigDecimal(10000)))

  val userAnswers: UserAnswers = UserAnswers(userAnswersId, submissionId)

  "DetailsOfThisTransfer Controller" - {

    "must return OK and the correct view for a GET" in {

      val updatedAnswers = emptyUserAnswers.set(ConnectedPersonsPage,true).success.value

      val application = applicationBuilder(userAnswers = Some(updatedAnswers),affinityGroup = orgAffinity)
        .overrides(bind[Navigator].qualifiedWith("organisations").toInstance(getNavigator))
        .build()


      running(application) {
        val request = FakeRequest(GET, detailsOfThisTransferRoute)

        val view = application.injector.instanceOf[DetailsOfThisTransferView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val answers = userAnswers.set(DetailsOfThisTransferPage, detailsOfThisTransfer).success.value
      val updatedAnswers = answers.set(ConnectedPersonsPage,true).success.value

      val application = applicationBuilder(userAnswers = Some(updatedAnswers),affinityGroup = orgAffinity)
        .overrides(bind[Navigator].qualifiedWith("organisations").toInstance(getNavigator))
        .build()

      running(application) {
        val request = FakeRequest(GET, detailsOfThisTransferRoute)

        val view = application.injector.instanceOf[DetailsOfThisTransferView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(detailsOfThisTransfer), NormalMode, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val updatedAnswers = emptyUserAnswers.set(ConnectedPersonsPage, true).success.value


      val application =
        applicationBuilder(userAnswers = Some(updatedAnswers),affinityGroup = orgAffinity)
          .build()

      running(application) {
        val request = {
          FakeRequest(POST, detailsOfThisTransferRoute)
            .withFormUrlEncodedBody(
              "numberOfShares" -> "15",
              "typeOfShares" -> "stocks",
              "amountPaid" -> amount.toString,
              "marketValue" -> amount.toString
            )
        }

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad().url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val updatedAnswers = emptyUserAnswers.set(ConnectedPersonsPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(updatedAnswers),affinityGroup = orgAffinity)
        .overrides(bind[Navigator].qualifiedWith("organisations").toInstance(getNavigator))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, detailsOfThisTransferRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[DetailsOfThisTransferView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None,affinityGroup = orgAffinity).build()

      running(application) {
        val request = FakeRequest(GET, detailsOfThisTransferRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None,affinityGroup = orgAffinity).build()

      running(application) {
        val request =
          FakeRequest(POST, detailsOfThisTransferRoute)
            .withFormUrlEncodedBody(("numberOfShares", "1"), ("typeOfShares", "stocks"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
