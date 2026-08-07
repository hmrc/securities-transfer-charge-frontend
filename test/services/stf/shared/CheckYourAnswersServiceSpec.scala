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

package services.stf.shared

import base.{Fixtures, SpecBase}
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessagesApi
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.{DetailsOfThisTransfer, SecuritiesTarget}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.stf.shared.CheckYourAnswersService

class CheckYourAnswersServiceSpec extends SpecBase {

  val service = new CheckYourAnswersService()
  implicit val messages: Messages = stubMessagesApi().preferred(Seq.empty)

  "CheckYourAnswersService" - {

    "buildYourDetailsRows" - {
      "must use ConfirmAddressPage when available" in {
        val userAnswers = buildStfUserAnswers(
          buyerAddress = Some(Fixtures.confirmableAddress)
        )

        val rows = service.buildYourDetailsRows(userAnswers)

        rows.size mustBe 1
      }

      "must use StfBuyersAddressPage when ConfirmAddressPage not available" in {
        val userAnswers = buildStfUserAnswers(
          stfBuyerAddress = Some(Fixtures.fakeAlfConfirmedAddress)
        )

        val rows = service.buildYourDetailsRows(userAnswers)

        rows.size mustBe 1
      }

      "must fallback to ConfirmAddressSummary when neither page available" in {
        val userAnswers = emptyUserAnswers

        val rows = service.buildYourDetailsRows(userAnswers)

        rows.size mustBe 1
      }
    }

    "buildSellerDetailsRows" - {
      "must return seller name and address rows" in {
        val userAnswers = buildStfUserAnswers(
          sellerName = Some("Test Seller"),
          sellerAddress = Some(Fixtures.fakeAlfConfirmedAddress)
        )

        val rows = service.buildSellerDetailsRows(userAnswers)

        rows.size mustBe 2
      }
    }

    "buildTransferDetailsRows" - {
      "must include relief row when applying for relief" in {
        val userAnswers = buildStfUserAnswers(
          connectedPersons = Some(true),
          applyingForRelief = Some(true),
          reliefName = Some("Test Relief"),
          securitiesTarget = Some(SecuritiesTarget("Test Company", Some("12345678")))
        )

        val rows = service.buildTransferDetailsRows(userAnswers)

        rows.size must be >= 5
      }

      "must exclude relief row when not applying for relief" in {
        val userAnswers = buildStfUserAnswers(
          connectedPersons = Some(true),
          applyingForRelief = Some(false),
          securitiesTarget = Some(SecuritiesTarget("Test Company", Some("12345678")))
        )

        val rows = service.buildTransferDetailsRows(userAnswers)

        rows.size must be >= 4
      }
    }

    "buildSecuritiesDetailsRows" - {
      "must build shares details with market value for connected persons" in {
        val detailsOfTransfer = DetailsOfThisTransfer(100, "Ordinary", BigDecimal("10000"), Some(BigDecimal("12000")))
        val userAnswers = emptyUserAnswers
          .set(PurchasingSharesPage, true).success.value
          .set(ConnectedPersonsPage, true).success.value
          .set(DetailsOfThisTransferPage, detailsOfTransfer).success.value

        val rows = service.buildSecuritiesDetailsRows(userAnswers)

        rows.size mustBe 5
      }

      "must build shares details without market value for non-connected persons" in {
        val detailsOfTransfer = DetailsOfThisTransfer(100, "Ordinary", BigDecimal("10000"), None)
        val userAnswers = emptyUserAnswers
          .set(PurchasingSharesPage, true).success.value
          .set(ConnectedPersonsPage, false).success.value
          .set(DetailsOfThisTransferPage, detailsOfTransfer).success.value

        val rows = service.buildSecuritiesDetailsRows(userAnswers)

        rows.size mustBe 4
      }

      "must build other securities details with market value for connected persons" in {
        val userAnswers = emptyUserAnswers
          .set(PurchasingSharesPage, false).success.value
          .set(ConnectedPersonsPage, true).success.value
          .set(OtherSecuritiesTypePage, "Bonds").success.value
          .set(AmountPaidForSecuritiesPage, BigDecimal("5000")).success.value
          .set(TotalMarketValuePage, BigDecimal("6000")).success.value

        val rows = service.buildSecuritiesDetailsRows(userAnswers)

        rows.size mustBe 4
      }

      "must build other securities details without market value for non-connected persons" in {
        val userAnswers = emptyUserAnswers
          .set(PurchasingSharesPage, false).success.value
          .set(ConnectedPersonsPage, false).success.value
          .set(OtherSecuritiesTypePage, "Bonds").success.value
          .set(AmountPaidForSecuritiesPage, BigDecimal("5000")).success.value

        val rows = service.buildSecuritiesDetailsRows(userAnswers)

        rows.size mustBe 3
      }
    }
  }
}
