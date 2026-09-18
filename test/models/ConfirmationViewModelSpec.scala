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
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.ConfirmationViewModel

import java.time.LocalDate

class ConfirmationViewModelSpec extends SpecBase {

  implicit val messages: Messages = messages(applicationBuilder().build())

  "ConfirmationViewModel" - {

    "must create view model with all required fields for individual" in {
      val submissionId = SubmissionId("STC-123456789")
      val paymentDueBy = LocalDate.of(2024, 12, 31)
      val taxDue = BigDecimal("1000.50")

      val viewModel = ConfirmationViewModel(
        submissionId = submissionId,
        paymentDueBy = paymentDueBy,
        reference = None,
        taxDue = taxDue,
        isAgent = false
      )

      viewModel.submissionId mustEqual submissionId.value
      viewModel.taxDue must include("1,000.5")
      viewModel.isAgent mustEqual false
      viewModel.reference mustBe None
    }

    "must create view model with agent reference for agent" in {
      val submissionId = SubmissionId("STC-987654321")
      val paymentDueBy = LocalDate.of(2025, 6, 15)
      val agentReference = Some("AGENT-REF-123")
      val taxDue = BigDecimal("2500.75")

      val viewModel = ConfirmationViewModel(
        submissionId = submissionId,
        paymentDueBy = paymentDueBy,
        reference = agentReference,
        taxDue = taxDue,
        isAgent = true
      )

      viewModel.submissionId mustEqual submissionId.value
      viewModel.isAgent mustEqual true
      viewModel.reference mustBe agentReference
    }

    "must format tax amounts correctly" in {
      val viewModel = ConfirmationViewModel(
        submissionId = SubmissionId("STC-123456789"),
        paymentDueBy = LocalDate.now(),
        reference = None,
        taxDue = BigDecimal("1000.50"),
        isAgent = false
      )

      viewModel.taxDue must include("£")
      viewModel.taxDue must include("1,000.5")
    }

    "must format tax due correctly" - {

      "must truncate to 2 decimal places without rounding up" in {
        val result = ConfirmationViewModel(
          submissionId = SubmissionId("ABC123"),
          paymentDueBy = LocalDate.now(),
          reference = None,
          taxDue = BigDecimal("9999.999"),
          isAgent = false
        )
        result.taxDue mustBe "£9,999.99"
      }

      "must truncate values with more than 2 decimal places" in {
        val result = ConfirmationViewModel(
          submissionId = SubmissionId("ABC123"),
          paymentDueBy = LocalDate.now(),
          reference = None,
          taxDue = BigDecimal("1234.567"),
          isAgent = false
        )
        result.taxDue mustBe "£1,234.56"
      }

      "must display pence when present" in {
        val result = ConfirmationViewModel(
          submissionId = SubmissionId("ABC123"),
          paymentDueBy = LocalDate.now(),
          reference = None,
          taxDue = BigDecimal("9999.19"),
          isAgent = false
        )
        result.taxDue mustBe "£9,999.19"
      }

      "must not display .00 for whole numbers" in {
        val result = ConfirmationViewModel(
          submissionId = SubmissionId("ABC123"),
          paymentDueBy = LocalDate.now(),
          reference = None,
          taxDue = BigDecimal("9000"),
          isAgent = false
        )
        result.taxDue mustBe "£9,000"
      }

      "must not display .00 for zero" in {
        val result = ConfirmationViewModel(
          submissionId = SubmissionId("ABC123"),
          paymentDueBy = LocalDate.now(),
          reference = None,
          taxDue = BigDecimal("0.0000"),
          isAgent = false
        )
        result.taxDue mustBe "£0"
      }

      "must format large amounts with commas and no .00" in {
        val result = ConfirmationViewModel(
          submissionId = SubmissionId("ABC123"),
          paymentDueBy = LocalDate.now(),
          reference = None,
          taxDue = BigDecimal("9999999"),
          isAgent = false
        )
        result.taxDue mustBe "£9,999,999"
      }

      "must format amounts with single penny" in {
        val result = ConfirmationViewModel(
          submissionId = SubmissionId("ABC123"),
          paymentDueBy = LocalDate.now(),
          reference = None,
          taxDue = BigDecimal("1234.5"),
          isAgent = false
        )
        result.taxDue mustBe "£1,234.5"
      }

      "must handle small amounts with pence" in {
        val result = ConfirmationViewModel(
          submissionId = SubmissionId("ABC123"),
          paymentDueBy = LocalDate.now(),
          reference = None,
          taxDue = BigDecimal("0.99"),
          isAgent = false
        )
        result.taxDue mustBe "£0.99"
      }

      "must handle small amounts with fraction less than pence" in {
        val result = ConfirmationViewModel(
          submissionId = SubmissionId("ABC123"),
          paymentDueBy = LocalDate.now(),
          reference = None,
          taxDue = BigDecimal("0.0099"),
          isAgent = false
        )
        result.taxDue mustBe "£0"
      }

      "must truncate even when third decimal would round up" in {
        val result = ConfirmationViewModel(
          submissionId = SubmissionId("ABC123"),
          paymentDueBy = LocalDate.now(),
          reference = None,
          taxDue = BigDecimal("100.999"),
          isAgent = false
        )
        result.taxDue mustBe "£100.99"
      }
    }
  }
}
