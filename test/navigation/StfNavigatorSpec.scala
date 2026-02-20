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

import base.SpecBase
import base.stubs.StubSessionRepository
import clients.FakeSaveAndReturnClient
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.seller.routes.StfSellerAddressController
import uk.gov.hmrc.securitiestransferchargefrontend.models.*
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.StfNavigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages._
import base.Fixtures

class StfNavigatorSpec extends SpecBase with ScalaFutures {

  val navigator = new StfNavigator(StubSessionRepository(), FakeSaveAndReturnClient())

  "Navigator" - {

    "in Normal mode" - {

      "must go from a page that doesn't exist in the route map to Journey Recovery" in {

        case object UnknownPage extends Page
        val result = navigator.nextPage(UnknownPage, NormalMode, UserAnswers("id", submissionId))(fakeRequest)
        whenReady(result) { res =>
          res mustBe routes.JourneyRecoveryController.onPageLoad()
        }

      }

      "must go from the HowToNotifyAboutSecuritiesTransfer to ConfirmAddressController when one at a time is selected" in {
        val answers = emptyUserAnswers.set(HowToNotifyAboutSecuritiesTransferPage, HowToNotifyAboutSecuritiesTransfer.OneAtATime).get
        val result = navigator.nextPage(HowToNotifyAboutSecuritiesTransferPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe routes.ConfirmAddressController.onPageLoad()
        }
      }

      "must go from the ConfirmAddressPage to NameOfSellerController" in {
        val answers = emptyUserAnswers.set(ConfirmAddressPage, Fixtures.confirmableAddress).get
        val result = navigator.nextPage(ConfirmAddressPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe routes.NameOfSellerController.onPageLoad(NormalMode)
        }
      }

      "must go from the NameOfSellerPage to sellers AddressLookup" in {
        val answers = emptyUserAnswers.set(NameOfSellerPage, "John").get
        val result = navigator.nextPage(NameOfSellerPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe StfSellerAddressController.onPageLoad()
        }
      }

      "must go from the buyers AddressLookup to NameOfSellerController" in {
        val answers = emptyUserAnswers.set(StfBuyersAddressPage, Fixtures.fakeAlfConfirmedAddress).get
        val result = navigator.nextPage(StfBuyersAddressPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe routes.NameOfSellerController.onPageLoad(NormalMode)
        }
      }

      "must go from the sellers AddressLookup to ConnectedPersonsController" in {
        val answers = emptyUserAnswers.set(SellerAddressPage, Fixtures.fakeAlfConfirmedAddress).get
        val result = navigator.nextPage(SellerAddressPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe routes.ConnectedPersonsController.onPageLoad(NormalMode)
        }
      }

      "must go from the ConnectedPersonsPage to ApplyingForReliefController" in {
        val answers = emptyUserAnswers.set(ConnectedPersonsPage, true).get
        val result = navigator.nextPage(ConnectedPersonsPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe routes.ApplyingForReliefController.onPageLoad(NormalMode)
        }
      }

      "must go from the ApplyingForReliefPage to WhatReliefAreYouApplyingForController when yes is selected" in {
        val answers = emptyUserAnswers.set(ApplyingForReliefPage, true).get
        val result = navigator.nextPage(ApplyingForReliefPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe routes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
        }
      }

      "must go from the OtherSecuritiesTypePage to AmountPaidForSecuritiesController" in {
        val answers = emptyUserAnswers.set(OtherSecuritiesTypePage, "OtherSecurities").get
        val result = navigator.nextPage(OtherSecuritiesTypePage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe routes.AmountPaidForSecuritiesController.onPageLoad(NormalMode)
        }
      }
    }

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {

        case object UnknownPage extends Page
        val result = navigator.nextPage(UnknownPage, CheckMode, UserAnswers("id", submissionId))(fakeRequest)
        whenReady(result) { res =>
          res mustBe routes.CheckYourAnswersController.onPageLoad()
        }
      }
    }
  }
}
