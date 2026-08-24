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

package viewmodels.sh03.agents

import base.SpecBase
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.{CompanyDetails, DetailsOfThisSharePurchase, ReasonForPurchase, RoleAtPurchasingCompany}
import uk.gov.hmrc.securitiestransferchargefrontend.models.shared.AgentReference
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.*
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.sh03.agents.CheckYourAnswersRowBuilder

import java.time.LocalDate

class CheckYourAnswersRowBuilderSpec extends SpecBase {

  implicit val msgs: Messages = stubMessages()

  "CheckYourAnswersRowBuilder" - {

    "buildYourDetailsRows" - {
      "must return an empty sequence when no Agent Reference is present" in {
        CheckYourAnswersRowBuilder.buildYourDetailsRows(emptyUserAnswers) mustBe Seq.empty
      }

      "must return rows when Agent Reference is present" in {
        val answers = emptyUserAnswers.set(AgentReferencePage, AgentReference(Some("REF123"))).success.value
        val rows = CheckYourAnswersRowBuilder.buildYourDetailsRows(answers)

        rows.size mustBe 1
      }
    }

    "buildBuyerDetailsRows" - {
      "must return an empty sequence when Company Details are missing" in {
        CheckYourAnswersRowBuilder.buildBuyerDetailsRows(emptyUserAnswers) mustBe Seq.empty
      }

      "must return 3 rows (Name, CRN, isPlc) when Company Details are present" in {
        val answers = emptyUserAnswers.set(CompanyDetailsPage, CompanyDetails("Test Ltd", "12345678", isPlc = false)).success.value
        val rows = CheckYourAnswersRowBuilder.buildBuyerDetailsRows(answers)

        rows.size mustBe 3
      }
    }

    "buildTransferDetailsRows" - {
      "must include the Treasury Shares row when purchase reason is ForCancellation" in {
        val answers = emptyUserAnswers
          .set(ReasonForPurchasePage, ReasonForPurchase.ForCancellation).success.value
          .set(TreasurySharesPage, true).success.value

        val rows = CheckYourAnswersRowBuilder.buildTransferDetailsRows(answers)

        rows.size mustBe 2
      }

      "must NOT include the Treasury Shares row when purchase reason is ToPlaceIntoTreasury" in {
        val answers = emptyUserAnswers
          .set(ReasonForPurchasePage, ReasonForPurchase.ToPlaceIntoTreasury).success.value

        val rows = CheckYourAnswersRowBuilder.buildTransferDetailsRows(answers)

        rows.size mustBe 1
      }

      "must include Relief Type row when applyingForRelief is true" in {
        val answers = emptyUserAnswers
          .set(ApplyingForReliefPage, true).success.value
          .set(WhatReliefAreYouApplyingForPage, "Some Relief").success.value

        val rows = CheckYourAnswersRowBuilder.buildTransferDetailsRows(answers)

        rows.size mustBe 2
      }

      "must NOT include Relief Type row when applyingForRelief is false" in {
        val answers = emptyUserAnswers.set(ApplyingForReliefPage, false).success.value
        val rows = CheckYourAnswersRowBuilder.buildTransferDetailsRows(answers)

        rows.size mustBe 1
      }

      "must include Max and Min amount rows when Company is a PLC" in {
        val answers = emptyUserAnswers
          .set(CompanyDetailsPage, CompanyDetails("PLC", "12345678", isPlc = true)).success.value
          .set(MaximumAmountPaidPage, BigDecimal("100.00")).success.value
          .set(MinimumAmountPaidPage, BigDecimal("10.00")).success.value

        val rows = CheckYourAnswersRowBuilder.buildTransferDetailsRows(answers)

        rows.size mustBe 2
      }

      "must NOT include Max and Min amount rows when Company is NOT a PLC" in {
        val answers = emptyUserAnswers
          .set(CompanyDetailsPage, CompanyDetails("LTD", "12345678", isPlc = false)).success.value

        val rows = CheckYourAnswersRowBuilder.buildTransferDetailsRows(answers)

        rows.size mustBe 0
      }

      "must conditionally show Market Value row based on ConnectedPersons being true" in {
        val details = DetailsOfThisSharePurchase(100, "Ordinary", BigDecimal("1000.00"), Some(BigDecimal("1200.00")))

        val answersConnected = emptyUserAnswers
          .set(ConnectedPersonsPage, true).success.value
          .set(DetailsOfThisSharePurchasePage, details).success.value

        val rowsConnected = CheckYourAnswersRowBuilder.buildTransferDetailsRows(answersConnected)
        rowsConnected.size mustBe 5

        val answersNotConnected = emptyUserAnswers
          .set(ConnectedPersonsPage, false).success.value
          .set(DetailsOfThisSharePurchasePage, details).success.value

        val rowsNotConnected = CheckYourAnswersRowBuilder.buildTransferDetailsRows(answersNotConnected)
        rowsNotConnected.size mustBe 4
      }

      "must include Charging Point row when the date is provided" in {
        val answers = emptyUserAnswers
          .set(ChargingPointPage, LocalDate.of(2026, 1, 1)).success.value

        val rows = CheckYourAnswersRowBuilder.buildTransferDetailsRows(answers)

        rows.size mustBe 1
      }
    }

    "buildDeclarationRows" - {

      "must return a single role row when not a UKS organ member" in {
        val answers = emptyUserAnswers.set(RoleAtPurchasingCompanyPage, RoleAtPurchasingCompany("director", None)).success.value
        val rows = CheckYourAnswersRowBuilder.buildDeclarationRows(answers)

        rows.size mustBe 1
      }

      "must return two rows when role requires a UKS organ" in {
        val answers = emptyUserAnswers.set(RoleAtPurchasingCompanyPage, RoleAtPurchasingCompany("ukSocietas", Some("Board"))).success.value
        val rows = CheckYourAnswersRowBuilder.buildDeclarationRows(answers)

        rows.size mustBe 2
      }
    }
  }
}