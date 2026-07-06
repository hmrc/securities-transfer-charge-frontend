package controllers

import base.SpecBase
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.securitiestransferchargefrontend.pages.WhatReliefAreYouApplyingForViewPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.organisations.single.WhatReliefAreYouApplyingForViewFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.SessionRepository
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.WhatReliefAreYouApplyingForViewView

import scala.concurrent.Future

class WhatReliefAreYouApplyingForViewControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new WhatReliefAreYouApplyingForViewFormProvider()
  val form = formProvider()

  lazy val whatReliefAreYouApplyingForViewRoute = routes.WhatReliefAreYouApplyingForViewController.onPageLoad(NormalMode).url

  "WhatReliefAreYouApplyingForView Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, whatReliefAreYouApplyingForViewRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[WhatReliefAreYouApplyingForViewView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(WhatReliefAreYouApplyingForViewPage, "answer").success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, whatReliefAreYouApplyingForViewRoute)

        val view = application.injector.instanceOf[WhatReliefAreYouApplyingForViewView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill("answer"), NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whatReliefAreYouApplyingForViewRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, whatReliefAreYouApplyingForViewRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[WhatReliefAreYouApplyingForViewView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, whatReliefAreYouApplyingForViewRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, whatReliefAreYouApplyingForViewRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
