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

package connectors

import base.SpecBase
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SaveAndReturnClient
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.SaveAndReturnConnectorImpl
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers

import scala.concurrent.Future

class SaveAndReturnConnectorImplSpec
  extends SpecBase
    with MockitoSugar
    with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    reset(mockSaveAndReturnClient)
  }

  private val mockSaveAndReturnClient = mock[SaveAndReturnClient]

  private val connector =
    new SaveAndReturnConnectorImpl(mockSaveAndReturnClient)

  private val userId = "user-id"
  private val submissionId = SubmissionId("submission-id")
  private val userAnswers = UserAnswers(userId, submissionId)

  "SaveAndReturnConnectorImpl.save" - {

    "return a unit on successful save" in {

      when(mockSaveAndReturnClient.save(userAnswers))
        .thenReturn(Future.successful(()))

      connector.save(userAnswers).futureValue mustBe()

      verify(mockSaveAndReturnClient).save(userAnswers)
      verifyNoMoreInteractions(mockSaveAndReturnClient)
    }
  }

  "SaveAndReturnConnectorImpl.retrieve" - {

    " return userAnswers on successful retrieval" in {

      when(mockSaveAndReturnClient.retrieve(userId, submissionId))
        .thenReturn(Future.successful(userAnswers))

      val result =
        connector.retrieve(userId, submissionId).futureValue

      result mustBe userAnswers

      verify(mockSaveAndReturnClient).retrieve(userId, submissionId)
      verifyNoMoreInteractions(mockSaveAndReturnClient)
    }
  }

  "SaveAndReturnConnectorImpl.list" - {

    "return submission ids on successfully retrieval" in {

      val submissionIds = List(
        SubmissionId("1"),
        SubmissionId("2")
      )

      when(mockSaveAndReturnClient.list(userId))
        .thenReturn(Future.successful(submissionIds))

      val result =
        connector.list(userId).futureValue

      result mustBe submissionIds

      verify(mockSaveAndReturnClient).list(userId)
      verifyNoMoreInteractions(mockSaveAndReturnClient)
    }

    "return an empty List if no submission Ids are retrieved" in {

      when(mockSaveAndReturnClient.list(userId))
        .thenReturn(Future.successful(List.empty))

      val result =
        connector.list(userId).futureValue

      result mustBe empty

      verify(mockSaveAndReturnClient).list(userId)
      verifyNoMoreInteractions(mockSaveAndReturnClient)
    }
  }
}
