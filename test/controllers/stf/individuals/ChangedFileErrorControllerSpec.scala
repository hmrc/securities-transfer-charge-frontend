package controllers.stf.individuals

import base.SpecBase
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.routes
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.ChangedFileErrorView

class ChangedFileErrorControllerSpec extends SpecBase {

  "ChangedFileError Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.ChangedFileErrorController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ChangedFileErrorView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, messages(application)).toString
      }
    }
  }
}
