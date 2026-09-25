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

package controllers.stf.shared

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.ConfirmationViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.TransactionResponseRepository
import uk.gov.hmrc.securitiestransferchargefrontend.services.SubmissionCreateResponseSuccess
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.shared.ConfirmationView

import java.time.LocalDate
import scala.concurrent.Future

class ConfirmationControllerSpec extends SpecBase with MockitoSugar {

  private val mockTransactionResponseRepository = mock[TransactionResponseRepository]

  "ConfirmationController" - {

    "must return OK and the correct view when transaction response exists" in {
      val paymentDueBy = LocalDate.of(2024, 12, 31)
      val taxDue = BigDecimal("1000.50")
      val responseSuccess = SubmissionCreateResponseSuccess(
        submissionId = submissionId,
        utrn = testUtrn, 
        chargeReferences = Seq("CHARGE-REF-001"),
        taxDue = taxDue,
        paymentDueBy = paymentDueBy,
        agentReference = None
      )

      when(mockTransactionResponseRepository.retrieve(any()))
        .thenReturn(Future.successful(responseSuccess))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[TransactionResponseRepository].toInstance(mockTransactionResponseRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.ConfirmationController.onPageLoad().url)
        val view = application.injector.instanceOf[ConfirmationView]
        val result = route(application, request).value

        val viewModel = ConfirmationViewModel(
          submissionId = submissionId,
          paymentDueBy = paymentDueBy,
          reference = None,
          taxDue = taxDue,
          isAgent = false
        )(messages(application))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(viewModel)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery when transaction response does not exist" in {
      when(mockTransactionResponseRepository.retrieve(any()))
        .thenReturn(Future.failed(new NoSuchElementException("Not found")))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[TransactionResponseRepository].toInstance(mockTransactionResponseRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.ConfirmationController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must include("there-is-a-problem")
      }
    }
  }
}
