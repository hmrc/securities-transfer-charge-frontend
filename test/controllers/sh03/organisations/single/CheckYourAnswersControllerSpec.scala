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

package controllers.sh03.organisations.single

import base.SpecBase
import com.google.inject.name.Names
import play.api.inject.bind
import play.api.libs.json.JsPath
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.single.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.{Navigator, PageCallBiMap, PageCallBiMapBuilder, UserAnswersValidator}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.{CyaPage, ErrorPage, Page}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.CompanyDetailsPage
import uk.gov.hmrc.securitiestransferchargefrontend.queries.Gettable

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase {

  private case object TestCyaPage extends Page with CyaPage with Gettable[Nothing] { override def path: JsPath = JsPath \ "test-cya" }
  private case object TestErrorPage extends Page with ErrorPage with Gettable[Nothing] { override def path: JsPath = JsPath \ "test-error" }

  def fakeNavigatorWithValidatorOutcome(outcome: Either[Call, Boolean]): Navigator = new Navigator {

    override def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers, isReturn: Boolean = false)(implicit request: Request[_]): Future[Call] =
      Future.successful(testNextPage)

    override def previousPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = testBackLinkRoute

    override def previousPage(page: Page, mode: Mode, userAnswers: Option[UserAnswers]): Call = testBackLinkRoute

    override def errorPage(forPage: Page): Call = testErrorPage

    override val userAnswersValidator: UserAnswersValidator = new UserAnswersValidator(this)(ec) {

      override protected val startPage: GettablePage[?] = outcome match {
        case Right(true)  => TestCyaPage
        case Right(false) => TestErrorPage
        case Left(_)      => CompanyDetailsPage
      }

      override protected lazy val pageCallMap: PageCallBiMap = outcome match {
        case Left(call) =>
          new PageCallBiMapBuilder().addMapping(CompanyDetailsPage, _ => call).build
        case _ =>
          new PageCallBiMapBuilder().build
      }

      override protected def pageHasValidDataAtPath(userAnswers: UserAnswers, page: GettablePage[?]): Boolean =
        outcome match {
          case Left(_) => false
          case _       => super.pageHasValidDataAtPath(userAnswers, page)
        }
    }
  }

  "CheckYourAnswers Controller" - {

    "onPageLoad" - {

      "must return OK and the correct view for a GET when validation succeeds (Right(true))" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].qualifiedWith(Names.named("orgSh03")).toInstance(fakeNavigatorWithValidatorOutcome(Right(true)))
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("Check your answers")
        }
      }

      "must redirect to the missing data page when validation returns Left(Call)" in {
        val fakeMissingDataCall = Call("GET", "/some-fake-missing-data-url")

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].qualifiedWith(Names.named("orgSh03")).toInstance(fakeNavigatorWithValidatorOutcome(Left(fakeMissingDataCall)))
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual fakeMissingDataCall.url
        }
      }

      "must redirect to the error page when validation returns Right(false)" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].qualifiedWith(Names.named("orgSh03")).toInstance(fakeNavigatorWithValidatorOutcome(Right(false)))
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual testErrorPage.url
        }
      }
    }

    "onSubmit" - {

      "must redirect to the next page" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].qualifiedWith(Names.named("orgSh03")).toInstance(fakeNavigatorWithValidatorOutcome(Right(true)))
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual testNextPage.url
        }
      }
    }
  }
}