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

package navigation

import com.google.common.collect.{BiMap, HashBiMap}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsString, Json}
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{GroupIdentifier, SubmissionId, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.{Navigator, UserAnswersValidator}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.{CyaPage, ErrorPage, Page, QuestionPage}
import uk.gov.hmrc.securitiestransferchargefrontend.queries.Gettable

import scala.concurrent.{ExecutionContext, Future}

class UserAnswersValidatorSpec extends AnyFreeSpec with Matchers with ScalaFutures {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val request: Request[_] = FakeRequest()

  // Test pages
  case object TestStartPage extends QuestionPage[String] {
    override def path: play.api.libs.json.JsPath = play.api.libs.json.JsPath \ toString
    override def toString: String = "testStartPage"
  }

  case object TestPage1 extends QuestionPage[String] {
    override def path: play.api.libs.json.JsPath = play.api.libs.json.JsPath \ toString
    override def toString: String = "testPage1"
  }

  case object TestPage2 extends QuestionPage[String] {
    override def path: play.api.libs.json.JsPath = play.api.libs.json.JsPath \ toString
    override def toString: String = "testPage2"
  }

  case object TestPage3 extends QuestionPage[String] {
    override def path: play.api.libs.json.JsPath = play.api.libs.json.JsPath \ toString
    override def toString: String = "testPage3"
  }

  case object TestCyaPage extends CyaPage with Gettable[String] {
    override def path: play.api.libs.json.JsPath = play.api.libs.json.JsPath \ toString
    override def toString: String = "testCyaPage"
  }

  case object TestErrorPage extends ErrorPage with Gettable[String] {
    override def path: play.api.libs.json.JsPath = play.api.libs.json.JsPath \ toString
    override def toString: String = "testErrorPage"
  }

  // Test calls
  val startPageCall: Call = Call("GET", "/start")
  val page1Call: Call = Call("GET", "/page1")
  val page2Call: Call = Call("GET", "/page2")
  val page3Call: Call = Call("GET", "/page3")
  val cyaPageCall: Call = Call("GET", "/cya")
  val errorPageCall: Call = Call("GET", "/error")
  val recoveryPageCall: Call = Call("GET", "/recovery")

  // Test navigator
  class TestNavigator extends Navigator {
    var nextPageMap: Map[Page, Call] = Map.empty

    def setNextPage(page: Page, call: Call): Unit = {
      nextPageMap = nextPageMap + (page -> call)
    }

    override def nextPage(page: Page, mode: uk.gov.hmrc.securitiestransferchargefrontend.models.Mode, userAnswers: UserAnswers, isReturn: Boolean = false)(implicit request: Request[_]): Future[Call] = {
      Future.successful(nextPageMap.getOrElse(page, recoveryPageCall))
    }

    override def previousPage(page: Page, mode: uk.gov.hmrc.securitiestransferchargefrontend.models.Mode, userAnswers: UserAnswers): Call = {
      recoveryPageCall
    }

    override def previousPage(page: Page, mode: uk.gov.hmrc.securitiestransferchargefrontend.models.Mode, userAnswers: Option[UserAnswers]): Call = {
      recoveryPageCall
    }

    override def errorPage(forPage: Page): Call = errorPageCall
  }

  def createUserAnswers(data: Map[String, String]): UserAnswers = {
    val jsonData = data.foldLeft(Json.obj()) { case (acc, (key, value)) =>
      acc + (key -> JsString(value))
    }
    UserAnswers(
      UserId("test-user-id"),
      GroupIdentifier("test-group-id"),
      SubmissionId("test-submission-id"),
      None,
      jsonData
    )
  }

  "UserAnswersValidator" - {

    "validate must" - {

      "return Right(true) when all pages have data and reach CYA page" in {
        val navigator = new TestNavigator
        navigator.setNextPage(TestStartPage, page1Call)
        navigator.setNextPage(TestPage1, page2Call)
        navigator.setNextPage(TestPage2, cyaPageCall)

        val validator = new UserAnswersValidator(navigator) {
          override protected val startPage: GettablePage[?] = TestStartPage
          override protected val pageCallMap: BiMap[GettablePage[?], Call] = {
            val map = HashBiMap.create[GettablePage[?], Call]()
            map.put(TestStartPage, startPageCall)
            map.put(TestPage1, page1Call)
            map.put(TestPage2, page2Call)
            map.put(TestCyaPage, cyaPageCall)
            map
          }
        }

        val userAnswers = createUserAnswers(Map(
          "testStartPage" -> "value0",
          "testPage1" -> "value1",
          "testPage2" -> "value2"
        ))

        whenReady(validator.validate(userAnswers)) { result =>
          result mustBe Right(true)
        }
      }

      "return Left(Call) when a page is missing data" in {
        val navigator = new TestNavigator
        navigator.setNextPage(TestStartPage, page1Call)
        navigator.setNextPage(TestPage1, page2Call)
        navigator.setNextPage(TestPage2, cyaPageCall)

        val validator = new UserAnswersValidator(navigator) {
          override protected val startPage: GettablePage[?] = TestStartPage
          override protected val pageCallMap: BiMap[GettablePage[?], Call] = {
            val map = HashBiMap.create[GettablePage[?], Call]()
            map.put(TestStartPage, startPageCall)
            map.put(TestPage1, page1Call)
            map.put(TestPage2, page2Call)
            map.put(TestCyaPage, cyaPageCall)
            map
          }
        }

        val userAnswers = createUserAnswers(Map(
          "testStartPage" -> "value0"
          // TestPage1 is missing
        ))

        whenReady(validator.validate(userAnswers)) { result =>
          result mustBe Left(page1Call)
        }
      }

      "return Left(Call) for the first missing page when multiple pages are missing" in {
        val navigator = new TestNavigator
        navigator.setNextPage(TestStartPage, page1Call)
        navigator.setNextPage(TestPage1, page2Call)
        navigator.setNextPage(TestPage2, page3Call)
        navigator.setNextPage(TestPage3, cyaPageCall)

        val validator = new UserAnswersValidator(navigator) {
          override protected val startPage: GettablePage[?] = TestStartPage
          override protected val pageCallMap: BiMap[GettablePage[?], Call] = {
            val map = HashBiMap.create[GettablePage[?], Call]()
            map.put(TestStartPage, startPageCall)
            map.put(TestPage1, page1Call)
            map.put(TestPage2, page2Call)
            map.put(TestPage3, page3Call)
            map.put(TestCyaPage, cyaPageCall)
            map
          }
        }

        val userAnswers = createUserAnswers(Map(
          "testStartPage" -> "value0"
          // TestPage1, TestPage2, and TestPage3 are all missing
        ))

        whenReady(validator.validate(userAnswers)) { result =>
          result mustBe Left(page1Call)
        }
      }

      "return Right(false) when an error page is encountered" in {
        val navigator = new TestNavigator
        navigator.setNextPage(TestStartPage, page1Call)
        navigator.setNextPage(TestPage1, errorPageCall)

        val validator = new UserAnswersValidator(navigator) {
          override protected val startPage: GettablePage[?] = TestStartPage
          override protected val pageCallMap: BiMap[GettablePage[?], Call] = {
            val map = HashBiMap.create[GettablePage[?], Call]()
            map.put(TestStartPage, startPageCall)
            map.put(TestPage1, page1Call)
            map.put(TestErrorPage, errorPageCall)
            map
          }
        }

        val userAnswers = createUserAnswers(Map(
          "testStartPage" -> "value0",
          "testPage1" -> "value1"
        ))

        whenReady(validator.validate(userAnswers)) { result =>
          result mustBe Right(false)
        }
      }

      "return Left(Call) when start page is missing data" in {
        val navigator = new TestNavigator
        navigator.setNextPage(TestStartPage, page1Call)

        val validator = new UserAnswersValidator(navigator) {
          override protected val startPage: GettablePage[?] = TestStartPage
          override protected val pageCallMap: BiMap[GettablePage[?], Call] = {
            val map = HashBiMap.create[GettablePage[?], Call]()
            map.put(TestStartPage, startPageCall)
            map.put(TestPage1, page1Call)
            map
          }
        }

        val userAnswers = createUserAnswers(Map.empty)

        whenReady(validator.validate(userAnswers)) { result =>
          result mustBe Left(startPageCall)
        }
      }

      "handle conditional navigation correctly" in {
        val navigator = new TestNavigator
        // Simulate conditional navigation: if TestPage1 has data, go to TestPage2, else go to CYA
        navigator.setNextPage(TestStartPage, page1Call)
        navigator.setNextPage(TestPage1, page2Call)
        navigator.setNextPage(TestPage2, cyaPageCall)

        val validator = new UserAnswersValidator(navigator) {
          override protected val startPage: GettablePage[?] = TestStartPage
          override protected val pageCallMap: BiMap[GettablePage[?], Call] = {
            val map = HashBiMap.create[GettablePage[?], Call]()
            map.put(TestStartPage, startPageCall)
            map.put(TestPage1, page1Call)
            map.put(TestPage2, page2Call)
            map.put(TestCyaPage, cyaPageCall)
            map
          }
        }

        val userAnswers = createUserAnswers(Map(
          "testStartPage" -> "value0",
          "testPage1" -> "value1",
          "testPage2" -> "value2"
        ))

        whenReady(validator.validate(userAnswers)) { result =>
          result mustBe Right(true)
        }
      }

      "use recovery page when call is not in pageCallMap" in {
        val navigator = new TestNavigator
        val unknownCall = Call("GET", "/unknown")
        navigator.setNextPage(TestStartPage, unknownCall)

        val validator = new UserAnswersValidator(navigator) {
          override protected val startPage: GettablePage[?] = TestStartPage
          override protected val pageCallMap: BiMap[GettablePage[?], Call] = {
            val map = HashBiMap.create[GettablePage[?], Call]()
            map.put(TestStartPage, startPageCall)
            // unknownCall is not in the map
            map
          }
        }

        val userAnswers = createUserAnswers(Map(
          "testStartPage" -> "value0"
        ))

        whenReady(validator.validate(userAnswers)) { result =>
          // Should navigate to recovery page (which is an ErrorPage) and return Right(false)
          result mustBe Right(false)
        }
      }
    }

  }
}
