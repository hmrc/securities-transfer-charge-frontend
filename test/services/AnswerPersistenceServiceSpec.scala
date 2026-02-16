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

package services

import base.{Fixtures, SpecBase}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SaveAndReturnClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.SessionRepository
import uk.gov.hmrc.securitiestransferchargefrontend.services.*

import scala.concurrent.Future

class AnswerPersistenceServiceSpec extends SpecBase with MockitoSugar {

  private val mockSessionRepository = mock[SessionRepository]
  private val mockSaveAndReturnClient = mock[SaveAndReturnClient]
  private val mockHeaderCarrier = mock[HeaderCarrier]
  
  def testSetup(): AnswerPersistenceService = {
    reset(mockSessionRepository)
    when(mockSessionRepository.set(any[UserAnswers])).thenReturn(Future.unit)

    reset(mockSaveAndReturnClient)
    when(mockSaveAndReturnClient.save(any[UserAnswers])(any[HeaderCarrier])).thenReturn(Future.unit)

    new AnswerPersistenceServiceImpl(mockSessionRepository, mockSaveAndReturnClient)
  }
  
  "AnswerPersistenceService" - {

    "store the user answers in the session" in {
      val userAnswers = Fixtures.emptyUserAnswers
      val nextCall = routes.NameOfSellerController.onPageLoad(NormalMode)
      val service = testSetup()
      val result = service.persistUserAnswers(userAnswers, nextCall)(mockHeaderCarrier)
      whenReady(result) { _ =>
        verify(mockSessionRepository, times(1)).set(any[UserAnswers])
      }
    }
    "store the user answers in the save and return repo" in {
      val userAnswers = Fixtures.emptyUserAnswers
      val nextCall = routes.NameOfSellerController.onPageLoad(NormalMode)
      val service = testSetup()
      val result = service.persistUserAnswers(userAnswers, nextCall)(mockHeaderCarrier)
      whenReady(result) { _ =>
        verify(mockSaveAndReturnClient, times(1)).save(any[UserAnswers])(any[HeaderCarrier])
      }
    }

  }
}
