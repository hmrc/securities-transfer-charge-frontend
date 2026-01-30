package controllers

import base.SpecBase
import uk.gov.hmrc.securitiestransferchargefrontend.forms.HowToNotifyAboutSecuritiesTransferFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, HowToNotifyAboutSecuritiesTransfer, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.securitiestransferchargefrontend.pages.HowToNotifyAboutSecuritiesTransferPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.SessionRepository
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.HowToNotifyAboutSecuritiesTransferView

import scala.concurrent.Future

class HowToNotifyAboutSecuritiesTransferControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  lazy val howToNotifyAboutSecuritiesTransferRoute = routes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(NormalMode).url

  val formProvider = new HowToNotifyAboutSecuritiesTransferFormProvider()
  val form = formProvider()

  "HowToNotifyAboutSecuritiesTransfer Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, howToNotifyAboutSecuritiesTransferRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[HowToNotifyAboutSecuritiesTransferView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(HowToNotifyAboutSecuritiesTransferPage, HowToNotifyAboutSecuritiesTransfer.values.head).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, howToNotifyAboutSecuritiesTransferRoute)

        val view = application.injector.instanceOf[HowToNotifyAboutSecuritiesTransferView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(HowToNotifyAboutSecuritiesTransfer.values.head), NormalMode)(request, messages(application)).toString
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
          FakeRequest(POST, howToNotifyAboutSecuritiesTransferRoute)
            .withFormUrlEncodedBody(("value", HowToNotifyAboutSecuritiesTransfer.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, howToNotifyAboutSecuritiesTransferRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[HowToNotifyAboutSecuritiesTransferView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, howToNotifyAboutSecuritiesTransferRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, howToNotifyAboutSecuritiesTransferRoute)
            .withFormUrlEncodedBody(("value", HowToNotifyAboutSecuritiesTransfer.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
