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

package models.stf.cya

import base.{Fixtures, SpecBase}
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessagesApi
import uk.gov.hmrc.securitiestransferchargefrontend.models.shared.AgentReference
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.TaxRate.HalfPercent
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.cya.StfAgentRowBuilder
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.{DetailsOfThisTransfer, SecuritiesTarget}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.*

import java.time.LocalDate

class StfAgentRowBuilderSpec extends SpecBase {

  implicit val messages: Messages = stubMessagesApi().preferred(Seq.empty)

  "StfAgentRowBuilderSpec" - {

    "buildYourDetailsRows" - {

      "must include the agent reference if available" in {
        val userAnswers = buildStfUserAnswers(
          agentReference = Some(AgentReference(Some("Reference1")))
        )

        val rows = StfAgentRowBuilder.buildYourDetailsRows(userAnswers)

        rows.size mustBe 1
      }
    }

    "buildBuyerDetailsRows" - {
      "must return buyer name and address rows" in {
        val userAnswers = buildStfUserAnswers(
          buyerName = Some("Test Seller"),
          stfBuyerAddress = Some(Fixtures.fakeAlfConfirmedAddress)
        )

        val rows = StfAgentRowBuilder.buildBuyerDetailsRows(userAnswers)

        rows.size mustBe 2
      }
    }

    "buildSellerDetailsRows" - {
      "must return seller name and address rows" in {
        val userAnswers = buildStfUserAnswers(
          sellerName = Some("Test Seller"),
          sellerAddress = Some(Fixtures.fakeAlfConfirmedAddress)
        )

        val rows = StfAgentRowBuilder.buildSellerDetailsRows(userAnswers)

        rows.size mustBe 2
      }
    }

    "buildTransferDetailsRows" - {
      "must include relief row when applying for relief" in {
        val userAnswers = buildStfUserAnswers(
          connectedPersons = Some(true),
          applyingForRelief = Some(true),
          reliefName = Some("Test Relief"),
          securitiesTarget = Some(SecuritiesTarget("Test Company", Some("12345678"))),
          chargingPoint = Some(LocalDate.now()),
          taxRate = Some(HalfPercent),
          purchasingShares = Some(false),
          otherSecuritiesType = Some("loan notes"),
          amountPaidForSecurities = Some(BigDecimal(245)),
          totalMarketValue = Some(BigDecimal(50))
        )

        val rows = StfAgentRowBuilder.buildTransferDetailsRows(userAnswers)
        rows.size mustBe 11
      }

      "must exclude relief row when not applying for relief" in {
        val userAnswers = buildStfUserAnswers(
          connectedPersons = Some(true),
          applyingForRelief = Some(false),
          securitiesTarget = Some(SecuritiesTarget("Test Company", Some("12345678"))),
          chargingPoint = Some(LocalDate.now()),
          taxRate = Some(HalfPercent),
          purchasingShares = Some(false),
          otherSecuritiesType = Some("loan notes"),
          amountPaidForSecurities = Some(BigDecimal(245)),
          totalMarketValue = Some(BigDecimal(50))
        )

        val rows = StfAgentRowBuilder.buildTransferDetailsRows(userAnswers)
        rows.size mustBe 10
      }
    }

    "buildSecuritiesDetailsRows" - {
      "must build shares details with market value for connected persons" in {
        val detailsOfTransfer = DetailsOfThisTransfer(100, "Ordinary", BigDecimal("10000"), Some(BigDecimal("12000")))
        val userAnswers = emptyUserAnswers
          .set(PurchasingSharesPage, true).success.value
          .set(ConnectedPersonsPage, true).success.value
          .set(DetailsOfThisTransferPage, detailsOfTransfer).success.value

        val rows = StfAgentRowBuilder.buildSecuritiesDetailsRows(userAnswers)

        rows.size mustBe 4
      }

      "must build shares details without market value for non-connected persons" in {
        val detailsOfTransfer = DetailsOfThisTransfer(100, "Ordinary", BigDecimal("10000"), None)
        val userAnswers = emptyUserAnswers
          .set(PurchasingSharesPage, true).success.value
          .set(ConnectedPersonsPage, false).success.value
          .set(DetailsOfThisTransferPage, detailsOfTransfer).success.value

        val rows = StfAgentRowBuilder.buildSecuritiesDetailsRows(userAnswers)

        rows.size mustBe 3
      }

      "must build other securities details with market value for connected persons" in {
        val userAnswers = emptyUserAnswers
          .set(PurchasingSharesPage, false).success.value
          .set(ConnectedPersonsPage, true).success.value
          .set(OtherSecuritiesTypePage, "Bonds").success.value
          .set(AmountPaidForSecuritiesPage, BigDecimal("5000")).success.value
          .set(TotalMarketValuePage, BigDecimal("6000")).success.value

        val rows = StfAgentRowBuilder.buildSecuritiesDetailsRows(userAnswers)

        rows.size mustBe 3
      }

      "must build other securities details without market value for non-connected persons" in {
        val userAnswers = emptyUserAnswers
          .set(PurchasingSharesPage, false).success.value
          .set(ConnectedPersonsPage, false).success.value
          .set(OtherSecuritiesTypePage, "loan notes").success.value
          .set(AmountPaidForSecuritiesPage, BigDecimal("5000")).success.value

        val rows = StfAgentRowBuilder.buildSecuritiesDetailsRows(userAnswers)

        rows.size mustBe 2
      }
    }
  }
}
