package controllers.sh03.agents.single

import base.SpecBase
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.single.routes
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.agents.single.CannotSubmitFormErrorView

class CannotSubmitFormErrorControllerSpec extends SpecBase {

  "CannotSubmitFormError Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CannotSubmitFormErrorController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CannotSubmitFormErrorView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, messages(application)).toString
      }
    }
  }
}
