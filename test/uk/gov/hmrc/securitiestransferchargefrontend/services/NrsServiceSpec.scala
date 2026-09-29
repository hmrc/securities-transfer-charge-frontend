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

package uk.gov.hmrc.securitiestransferchargefrontend.services

import base.{Fixtures, SpecBase}
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.securitiestransferchargefrontend.clients.registration.NrsClient
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.{StcAuthorisedRequest, StcDataRequest}
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.models.nrs.IdentityData
import uk.gov.hmrc.securitiestransferchargefrontend.services.NrsServiceImpl

class NrsServiceSpec extends SpecBase with BeforeAndAfterEach with MockitoSugar {

  private def mockNrsClient = mock[NrsClient]
  private def mockConfig = mock[FrontendAppConfig]

  def getService(): NrsServiceImpl = {
    new NrsServiceImpl(mockNrsClient, mockConfig)
  }

  "The service" - {
    "should successfully pull out non-tax search keys and continue without a tax ID" in {
      val service = getService()
      val request = mock[StcDataRequest[?]]

      val authReq = mock[StcAuthorisedRequest[?]]
      when(request.request).thenReturn(authReq)
      when(authReq.subscriptionId).thenReturn(subscriptionId)

      val answers = mock[UserAnswers]
      when(request.userAnswers).thenReturn(answers)
      when(answers.submissionId).thenReturn(submissionId)

      val mockIdentityData = mock[IdentityData]
      when(mockIdentityData.nino).thenReturn(None)
      when(mockIdentityData.saUtr).thenReturn(None)
      when(authReq.maybeArn).thenReturn(None)
      when(authReq.identityData).thenReturn(mockIdentityData)

      val keys = service.searchKeys(request, mockIdentityData, testUtrn)
      keys(NrsSearchKeys.SubmissionId) mustBe submissionId.value
      keys(NrsSearchKeys.SubscriptionId) mustBe subscriptionId.value
      keys(NrsSearchKeys.Utrn) mustBe testUtrn
    }

    "should successfully pull out individual tax search keys" in {
      val service = getService()
      val request = mock[StcDataRequest[?]]

      val authReq = mock[StcAuthorisedRequest[?]]
      when(request.request).thenReturn(authReq)
      when(authReq.subscriptionId).thenReturn(subscriptionId)

      val answers = mock[UserAnswers]
      when(request.userAnswers).thenReturn(answers)
      when(answers.submissionId).thenReturn(submissionId)

      val mockIdentityData = mock[IdentityData]
      when(mockIdentityData.nino).thenReturn(Some(Fixtures.testNino))
      when(mockIdentityData.saUtr).thenReturn(None)
      when(authReq.maybeArn).thenReturn(None)
      when(authReq.identityData).thenReturn(mockIdentityData)

      val keys = service.searchKeys(request, mockIdentityData, testUtrn)
      keys(NrsSearchKeys.SubmissionId) mustBe submissionId.value
      keys(NrsSearchKeys.SubscriptionId) mustBe subscriptionId.value
      keys(NrsSearchKeys.Utrn) mustBe testUtrn
      keys(NrsSearchKeys.Nino) mustBe Fixtures.testNino
    }
    "should successfully pull out org tax search keys" in {
      val service = getService()
      val request = mock[StcDataRequest[?]]

      val authReq = mock[StcAuthorisedRequest[?]]
      when(request.request).thenReturn(authReq)
      when(authReq.subscriptionId).thenReturn(subscriptionId)

      val answers = mock[UserAnswers]
      when(request.userAnswers).thenReturn(answers)
      when(answers.submissionId).thenReturn(submissionId)

      val mockIdentityData = mock[IdentityData]
      when(mockIdentityData.nino).thenReturn(None)
      when(mockIdentityData.saUtr).thenReturn(Some(Fixtures.testUtr))
      when(authReq.maybeArn).thenReturn(None)
      when(authReq.identityData).thenReturn(mockIdentityData)

      val keys = service.searchKeys(request, mockIdentityData, testUtrn)
      keys(NrsSearchKeys.SubmissionId) mustBe submissionId.value
      keys(NrsSearchKeys.SubscriptionId) mustBe subscriptionId.value
      keys(NrsSearchKeys.Utrn) mustBe testUtrn
      keys(NrsSearchKeys.Utr) mustBe Fixtures.testUtr
    }
    "should successfully pull out agent tax search keys" in {
      val service = getService()
      val request = mock[StcDataRequest[?]]

      val authReq = mock[StcAuthorisedRequest[?]]
      when(request.request).thenReturn(authReq)
      when(authReq.subscriptionId).thenReturn(subscriptionId)

      val answers = mock[UserAnswers]
      when(request.userAnswers).thenReturn(answers)
      when(answers.submissionId).thenReturn(submissionId)

      val mockIdentityData = mock[IdentityData]
      when(mockIdentityData.nino).thenReturn(None)
      when(mockIdentityData.saUtr).thenReturn(None)
      when(authReq.maybeArn).thenReturn(Some(Fixtures.testArn))
      when(authReq.identityData).thenReturn(mockIdentityData)

      val keys = service.searchKeys(request, mockIdentityData, testUtrn)
      keys(NrsSearchKeys.SubmissionId) mustBe submissionId.value
      keys(NrsSearchKeys.SubscriptionId) mustBe subscriptionId.value
      keys(NrsSearchKeys.Utrn) mustBe testUtrn
      keys(NrsSearchKeys.Arn) mustBe Fixtures.testArn
    }

    "should successfully build metadata from inputs" in {
      
    }

  }


}
