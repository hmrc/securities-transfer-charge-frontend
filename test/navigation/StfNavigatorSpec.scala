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
import base.stubs.StubAnswerPersistenceService
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.individuals.routes as individualRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.*
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.StfNavigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*
import base.Fixtures
import uk.gov.hmrc.securitiestransferchargefrontend.pages.individuals.{AmountPaidForSecuritiesPage, ApplyingForReliefPage, ChargingPointPage, ConfirmAddressPage, ConnectedPersonsPage, DetailsOfThisTransferPage, HowToNotifyAboutSecuritiesTransferPage, NameOfSellerPage, OtherSecuritiesTypePage, SecuritiesTargetPage, SellerAddressPage, StfBuyersAddressPage, TaxRatePage, WhatTypeOfSecuritiesPage}

import java.time.LocalDate

class StfNavigatorSpec extends SpecBase with ScalaFutures {

  val navigator = new StfNavigator(StubAnswerPersistenceService())

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
          res mustBe individualRoutes.ConfirmAddressController.onPageLoad()
        }
      }

      "must go from the ConfirmAddressPage to NameOfSellerController" in {
        val answers = emptyUserAnswers.set(ConfirmAddressPage, Fixtures.confirmableAddress).get
        val result = navigator.nextPage(ConfirmAddressPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe individualRoutes.NameOfSellerController.onPageLoad(NormalMode)
        }
      }

      "must go from the NameOfSellerPage to sellers AddressLookup" in {
        val answers = emptyUserAnswers.set(NameOfSellerPage, "John").get
        val result = navigator.nextPage(NameOfSellerPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe individualRoutes.StfSellerAddressController.onPageLoad()
        }
      }

      "must go from the buyers AddressLookup to NameOfSellerController" in {
        val answers = emptyUserAnswers.set(StfBuyersAddressPage, Fixtures.fakeAlfConfirmedAddress).get
        val result = navigator.nextPage(StfBuyersAddressPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe individualRoutes.NameOfSellerController.onPageLoad(NormalMode)
        }
      }

      "must go from the sellers AddressLookup to ConnectedPersonsController" in {
        val answers = emptyUserAnswers.set(SellerAddressPage, Fixtures.fakeAlfConfirmedAddress).get
        val result = navigator.nextPage(SellerAddressPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe individualRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
        }
      }

      "must go from the ConnectedPersonsPage to ApplyingForReliefController" in {
        val answers = emptyUserAnswers.set(ConnectedPersonsPage, true).get
        val result = navigator.nextPage(ConnectedPersonsPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe individualRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
        }
      }

      "must go from the ApplyingForReliefPage to WhatReliefAreYouApplyingForController when yes is selected" in {
        val answers = emptyUserAnswers.set(ApplyingForReliefPage, true).get
        val result = navigator.nextPage(ApplyingForReliefPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe individualRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
        }
      }

      "must go from the ApplyingForReliefPage to SecuritiesTargetController when no is selected" in {
        val answers = emptyUserAnswers.set(ApplyingForReliefPage, false).get
        val result = navigator.nextPage(ApplyingForReliefPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe individualRoutes.SecuritiesTargetController.onPageLoad(NormalMode)
        }
      }

      "must go from the SecuritiesTargetPage to ChargingPointController" in {
        val answers = emptyUserAnswers.set(SecuritiesTargetPage, SecuritiesTarget("Business 1",Some("12345678"))).get
        val result = navigator.nextPage(SecuritiesTargetPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe individualRoutes.ChargingPointController.onPageLoad(NormalMode)
        }
      }

      "must go from the ChargingPointPage to TaxRateController" in {
        val answers = emptyUserAnswers.set(ChargingPointPage, LocalDate.now()).get
        val result = navigator.nextPage(ChargingPointPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe individualRoutes.TaxRateController.onPageLoad(NormalMode)
        }
      }

      "must go from the TaxRatePage to WhatTypeOfSecuritiesController" in {
        val answers = emptyUserAnswers.set(TaxRatePage, TaxRate.Half).get
        val result = navigator.nextPage(TaxRatePage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe individualRoutes.WhatTypeOfSecuritiesController.onPageLoad(NormalMode)
        }
      }

      "must go from the WhatTypeOfSecuritiesPage to DetailsOfThisTransferController when shares is selected" in {
        val answers = emptyUserAnswers.set(WhatTypeOfSecuritiesPage, WhatTypeOfSecurities.Shares).get
        val result = navigator.nextPage(WhatTypeOfSecuritiesPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe individualRoutes.DetailsOfThisTransferController.onPageLoad(NormalMode)
        }
      }

      "must go from the WhatTypeOfSecuritiesPage to OtherSecuritiesTypeController when other is selected" in {
        val answers = emptyUserAnswers.set(WhatTypeOfSecuritiesPage, WhatTypeOfSecurities.Other).get
        val result = navigator.nextPage(WhatTypeOfSecuritiesPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe individualRoutes.OtherSecuritiesTypeController.onPageLoad(NormalMode)
        }
      }

      "must go from the OtherSecuritiesTypePage to AmountPaidForSecuritiesController" in {
        val answers = emptyUserAnswers.set(OtherSecuritiesTypePage, "bonds").get
        val result = navigator.nextPage(OtherSecuritiesTypePage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe individualRoutes.AmountPaidForSecuritiesController.onPageLoad(NormalMode)
        }
      }

      "must go from the AmountPaidForSecuritiesPage to TotalMarketValueController when connected persons is true" in {
        val answers = emptyUserAnswers.set(AmountPaidForSecuritiesPage, BigDecimal(500)).get
        val updated = answers.set(ConnectedPersonsPage,true).get
        val result = navigator.nextPage(AmountPaidForSecuritiesPage, NormalMode, updated)(fakeRequest)
        whenReady(result) { res =>
          res mustBe individualRoutes.TotalMarketValueController.onPageLoad(NormalMode)
        }
      }

      "must go from the AmountPaidForSecuritiesPage to CheckYourAnswersController when connected persons is false" in {
        val answers = emptyUserAnswers.set(AmountPaidForSecuritiesPage, BigDecimal(500)).get
        val updated = answers.set(ConnectedPersonsPage, false).get
        val result = navigator.nextPage(AmountPaidForSecuritiesPage, NormalMode, updated)(fakeRequest)
        whenReady(result) { res =>
          res mustBe routes.CheckYourAnswersController.onPageLoad()
        }
      }

      "must go from the DetailsOfThisTransferPage to CheckYourAnswersController" in {
        val answers = emptyUserAnswers.set(DetailsOfThisTransferPage, DetailsOfThisTransfer(numberOfShares = "200",
          typeOfShares = "ordinary share", amountPaid = BigDecimal(500), marketValue = BigDecimal(1500))).get
        val result = navigator.nextPage(DetailsOfThisTransferPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe routes.CheckYourAnswersController.onPageLoad()
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
