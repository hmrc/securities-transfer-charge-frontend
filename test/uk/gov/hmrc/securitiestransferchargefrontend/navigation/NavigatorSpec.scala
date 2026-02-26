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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsPath
import play.api.mvc.{Call, Request}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.routes as individualRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.AbstractNavigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*
import uk.gov.hmrc.securitiestransferchargefrontend.queries.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.AnswerPersistenceService

import scala.concurrent.Future

class NavigatorSpec extends SpecBase with MockitoSugar with ScalaFutures {
  val testPage: Page & Gettable[Boolean] & Settable[Boolean] = new Page with Gettable[Boolean] with Settable[Boolean] {
    override def path: JsPath = JsPath \ "test"
  }
  private val emptyUserAnswers = UserAnswers.empty("test-id")(submissionId)
  private val userAnswers = emptyUserAnswers.set(testPage, true).get
  //private val errorCall = routes.JourneyRecoveryController.onPageLoad()
  private val testCall = individualRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(NormalMode)

  private val mockAnswerPersistenceService = mock[AnswerPersistenceService]
  when(mockAnswerPersistenceService.save(any[UserAnswers])(any[HeaderCarrier]))
    .thenReturn(Future.unit)

  private implicit val mockHeaderCarrier: HeaderCarrier = mock[HeaderCarrier]

  private def testSetup(): TestNavigator = {
    reset(mockAnswerPersistenceService)
    when(mockAnswerPersistenceService.save(any[UserAnswers])(any[HeaderCarrier]))
      .thenReturn(Future.unit)

    new TestNavigator()
  }

  class TestNavigator extends AbstractNavigator(mockAnswerPersistenceService) {
    override val errorPage: Page => Call = _ => testCall

    override def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers)(implicit request: Request[?]): Future[Call] =
      Future.successful(testCall)
  }

  "All navigators should" - {
    "successfully go to a page" in {
      val navigator = testSetup()
      val result = navigator.goTo(testCall)
      result.futureValue mustBe testCall
    }
    "update and store user answers if supplied when going to a page" in {
      val navigator = testSetup()
      val mockUserAnswers = mock[UserAnswers]
      when(mockUserAnswers.setNextPage(testCall)).thenReturn(mockUserAnswers)
      val updatedUserAnswers = mockUserAnswers.setNextPage(testCall)
      val result = navigator.goTo(testCall, Some(mockUserAnswers))
      whenReady(result) { _ =>
        verify(mockAnswerPersistenceService, times(1)).save(updatedUserAnswers)(mockHeaderCarrier)
        // Look for 2 calls here because there's one above in the test setup.
        verify(mockUserAnswers, times(2)).setNextPage(testCall)
        result.futureValue mustBe testCall
      }
    }
    "store user answers when navigating from a page that requires data" in {
      val navigator = testSetup()
      val result = navigator.dataRequired(testPage, userAnswers, testCall)
      val updatedUserAnswers = userAnswers.setNextPage(testCall)
      whenReady(result) { _ =>
        verify(mockAnswerPersistenceService, times(1)).save(updatedUserAnswers)(mockHeaderCarrier)
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
      } yield {
        res mustBe Navigator.defaultPage
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
      } yield {
        res mustBe Navigator.defaultPage
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
    "return the success page when data is present for user answers dependent navigation" in {
      val navigator = testSetup()
      val result = navigator.userAnswersDependent(userAnswers)(_ => testCall)
      whenReady(result) { res =>
        res mustBe testCall
      }
    }
    "call the provided function and save user answers for user answers dependent navigation" in {
      val navigator = testSetup()
      val mockMethod = mock[UserAnswers => Call]
      when(mockMethod.apply(userAnswers)).thenReturn(testCall)
      val updatedUserAnswers = userAnswers.setNextPage(testCall)
      val result = navigator.userAnswersDependent(userAnswers)(mockMethod)
      whenReady(result) { _ =>
        verify(mockMethod, times(1)).apply(userAnswers)
        verify(mockAnswerPersistenceService, times(1)).save(updatedUserAnswers)(mockHeaderCarrier)
      }
    }
  }


}
