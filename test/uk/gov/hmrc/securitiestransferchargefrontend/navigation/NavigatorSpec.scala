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

package uk.gov.hmrc.securitiestransferchargefrontend.navigation

import base.SpecBase
import base.stubs.StubSessionRepository
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsPath
import play.api.mvc.{Call, Request}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SaveAndReturnClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.AbstractNavigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*
import uk.gov.hmrc.securitiestransferchargefrontend.queries.*
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.SessionRepository

import scala.concurrent.Future

class NavigatorSpec extends SpecBase with MockitoSugar with ScalaFutures {
  val testPage: Page & Gettable[Boolean] & Settable[Boolean] = new Page with Gettable[Boolean] with Settable[Boolean] {
    override def path: JsPath = JsPath \ "test"
  }
  private val emptyUserAnswers = UserAnswers.empty("test-id")(submissionId)
  private val userAnswers = emptyUserAnswers.set(testPage, true).get
  private val mockSessionRepository = mock[SessionRepository]
  private val testCall = routes.JourneyRecoveryController.onPageLoad()
  private val mockSaveAndReturnClient = mock[SaveAndReturnClient]

  private def testSetup(): TestNavigator = {
    reset(mockSessionRepository)
    reset(mockSaveAndReturnClient)

    when(mockSessionRepository.set(any[UserAnswers]())).thenReturn(Future.successful(()))
    when(mockSaveAndReturnClient.save(any[UserAnswers])(any[HeaderCarrier])).thenReturn(Future.successful(()))

    new TestNavigator(mockSessionRepository)
  }

  class TestNavigator(mockSessionRepository: SessionRepository) extends AbstractNavigator(mockSessionRepository, mockSaveAndReturnClient) {
    override val errorPage: Page => Call = _ => testCall

    override def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers)(implicit request: Request[?]): Future[Call] =
      Future.successful(testCall)
  }

  "All navigators should" - {
    "successfully go to a page" in {
      val result = new TestNavigator(new StubSessionRepository()).goTo(testCall)
      result.futureValue mustBe testCall
    }
    "store user answers if supplied when going to a page" in {
      val navigator = testSetup()
      val result = navigator.goTo(testCall, Some(userAnswers))
      whenReady(result) { _ =>
        verify(mockSessionRepository, times(1)).set(userAnswers)
        verify(mockSaveAndReturnClient, times(1)).save(userAnswers)
        result.futureValue mustBe testCall
      }
    }
    "store user answers when navigating from a page that requires data" in {
      val navigator = testSetup()
      val result = navigator.dataRequired(testPage, userAnswers, testCall)
      whenReady(result) { _ =>
        verify(mockSessionRepository, times(1)).set(userAnswers)
        verify(mockSaveAndReturnClient, times(1)).save(userAnswers)
      }
    }
    "return the success page when data is present for data required navigation" in {
      val navigator = testSetup()
      val result = navigator.dataRequired(testPage, userAnswers, testCall)
      whenReady(result) { res =>
        res mustBe testCall
      }
    }
    "return the error page when data is missing for data required navigation" in {
      val navigator = testSetup()
      val result = navigator.dataRequired(testPage, emptyUserAnswers, testCall)
      for {
        res     <- result
        default <- navigator.defaultPage
      } yield {
        res mustBe default
      }
    }
    "return the success page when data is present for data dependent navigation" in {
      val navigator = testSetup()
      val result = navigator.dataDependent(testPage, userAnswers)(_ => testCall)
      whenReady(result) { res =>
        res mustBe testCall
      }
    }
    "return the error page when data is missing for data dependent navigation" in {
      val navigator = testSetup()
      val result = navigator.dataDependent(testPage, emptyUserAnswers)(_ => testCall)
      for {
        res     <- result
        default <- navigator.defaultPage
      } yield {
        res mustBe default
      }
    }
    "call the provided function when data is present for data dependent navigation" in {
      val navigator = testSetup()
      val mockMethod = mock[Boolean => Call]
      when(mockMethod.apply(true)).thenReturn(testCall)
      val result = navigator.dataDependent(testPage, userAnswers)(mockMethod)
        whenReady(result) { _ =>
        verify(mockMethod, times(1)).apply(true)
      }
    }
  }

}
