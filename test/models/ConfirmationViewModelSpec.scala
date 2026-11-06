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

package models

import base.SpecBase
import play.api.i18n.Messages
import uk.gov.hmrc.securitiestransferchargefrontend.models.ConfirmationViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.{STF, SH03}
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType


class ConfirmationViewModelSpec extends SpecBase {

  implicit val messages: Messages = messages(applicationBuilder().build())

  private def viewModel(journeyType: JourneyType): ConfirmationViewModel =
    ConfirmationViewModel(
      submissionId = "STC-123456789",
      paymentDueBy = "25 January 2026",
      reference = None,
      taxDue = "£500",
      isAgent = false,
      journeyType = journeyType,
      isBulk = false
    )

  "ConfirmationViewModel" - {

    "singleReferencesKey" - {

      "must use the transfer reference key for STF" in {
        viewModel(STF).singleReferencesKey mustBe "confirmation.heading.transferReference.p1"
      }

      "must use the share buyback key for SH03" in {
        viewModel(SH03).singleReferencesKey mustBe "confirmation.heading.shareBuyBack.p1"
      }
    }

    "useReferencesKey" - {

      "must use the transfer reference key for STF" in {
        viewModel(STF).useReferencesKey mustBe "confirmation.heading.transferReference.p2"
      }

      "must use the share buyback key for SH03" in {
        viewModel(SH03).useReferencesKey mustBe "confirmation.heading.shareBuyBack.p2"
      }
    }
  }
}