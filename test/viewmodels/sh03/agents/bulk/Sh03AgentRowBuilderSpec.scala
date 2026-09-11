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

package viewmodels.sh03.agents.bulk

import base.SpecBase
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.RoleAtPurchasingCompany
import uk.gov.hmrc.securitiestransferchargefrontend.models.shared.AgentReference
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.bulk.{BulkAgentReferencePage, BulkCompanyDetailsPage, BulkRoleAtPurchasingCompanyPage}
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.agents.bulk.Sh03AgentRowBuilder
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.bulk.CompanyDetails

class Sh03AgentRowBuilderSpec extends SpecBase {

  implicit val msgs: Messages = stubMessages()

  "Sh03AgentRowBuilder" - {

    "buildYourDetailsRows" - {

      "must render Not provided when no Agent Reference is present" in {
        val answers = emptyUserAnswers.set(BulkAgentReferencePage, AgentReference(None)).success.value

        val rows = Sh03AgentRowBuilder.buildYourDetailsRows(answers)

        rows.map(_.value.content) must contain(Text("site.notProvided"))
      }

      "must return rows when Agent Reference is present" in {
        val answers = emptyUserAnswers.set(AgentReferencePage, AgentReference(Some("REF123"))).success.value
        val rows = Sh03AgentRowBuilder.buildYourDetailsRows(answers)

        rows.size mustBe 1
      }
    }

    "buildBuyerDetailsRows" - {
      "must return an empty sequence when Company Details are missing" in {
        Sh03AgentRowBuilder.buildBuyerDetailsRows(emptyUserAnswers) mustBe Seq.empty
      }

      "must return 3 rows (Name, CRN) when Company Details are present" in {
        val answers = emptyUserAnswers.set(BulkCompanyDetailsPage, CompanyDetails("Test Ltd", "12345678")).success.value
        val rows = Sh03AgentRowBuilder.buildBuyerDetailsRows(answers)

        rows.size mustBe 2
      }
    }

    "buildFileDetailsCard" - {
      "must build the file details card with the correct file name and number of rows" in {
        val fileName = "testFile.csv"
        val numOfRows = 5
        val rows = Sh03AgentRowBuilder.buildFileDetailsCard(fileName = fileName, rows = numOfRows)

        rows.rows.size mustBe 2
      }


      "buildDeclarationRows" - {

        "must return a single role row when not a UKS organ member" in {
          val answers = emptyUserAnswers.set(BulkRoleAtPurchasingCompanyPage, RoleAtPurchasingCompany("director", None)).success.value.setFileUploadReference("ref")
          val rows = Sh03AgentRowBuilder.buildDeclarationRows(answers)

          rows.size mustBe 1
        }

        "must return two rows when role requires a UKS organ" in {
          val answers = emptyUserAnswers.set(BulkRoleAtPurchasingCompanyPage, RoleAtPurchasingCompany("ukSocietas", Some("Board"))).success.value.setFileUploadReference("ref")
          val rows = Sh03AgentRowBuilder.buildDeclarationRows(answers)

          rows.size mustBe 2
        }
      }
    }
  }
}