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

import base.Fixtures.{buildEtmpSuccessResponse, buildSubmissionDetails}
import base.{Fixtures, SpecBase}
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.EtmpTransactionSummaryResponse
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SubmissionStatus.{Paid, ReadyToPay}

import java.time.LocalDate
import base.Fixtures.{buildLateFilingCharge, buildTaxCharge, withDueDate}
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SubmissionStatus.Overdue

class DashboardServiceObjectSpec extends SpecBase {

  "getSubmissionStatus should" - {
    "return overdue if a single charge is overdue and unpaid" in {
      val oneOverdue =
        List(
          buildTaxCharge(Fixtures.testUtrn, false, true),
          buildLateFilingCharge(Fixtures.testUtrn, true, false),
        )

      DashboardService.getSubmissionStatus(oneOverdue) mustBe Overdue
    }
    "return paid even if a single charge is overdue so long as it is unpaid" in {
      val allPaid =
        List(
          buildTaxCharge(Fixtures.testUtrn, false, true),
          buildLateFilingCharge(Fixtures.testUtrn, true, true),
        )

      DashboardService.getSubmissionStatus(allPaid) mustBe Paid
    }

    "return ready to pay if a single charge is unpaid but not overdue" in {
      val oneUnpaid =
        List(
          buildTaxCharge(Fixtures.testUtrn, false, false),
          buildLateFilingCharge(Fixtures.testUtrn, true, true),
        )

      DashboardService.getSubmissionStatus(oneUnpaid) mustBe ReadyToPay
    }

    "return paid if all charges are paid" in {
      val allPaid =
        List(
          buildTaxCharge(Fixtures.testUtrn, false, true),
          buildLateFilingCharge(Fixtures.testUtrn, true, true),
        )

      DashboardService.getSubmissionStatus(allPaid) mustBe Paid
    }
  }

  "submissionDueBy should" - {
    "return the earliest date" in {
      val earliest = LocalDate.parse("2027-02-01")
      val allUnpaid = List(
          buildTaxCharge(Fixtures.testUtrn, false, false).withDueDate(earliest),
          buildLateFilingCharge(Fixtures.testUtrn, true, false).withDueDate(earliest.plusDays(10)),
        )

        DashboardService.submissionDueBy(allUnpaid) mustBe Some(earliest)
      }
    }

  "toSubmissionList should" - {
    "Include all submission IDs if there are more than one" in {
      val otherUtrn = "00998877"
      val otherSubmissionId = SubmissionId("STC-00998877")
      val expectedSubmissionIds = List(Fixtures.testSubmissionId.value, otherSubmissionId.value)
      val resp = buildEtmpSuccessResponse(
        transactionDetails = List(
          buildSubmissionDetails(
            submissionId = Fixtures.testSubmissionId,
            isPast = false,
            utrn = testUtrn
          ),
          buildSubmissionDetails(
            submissionId = otherSubmissionId,
            isPast = false,
            utrn = otherUtrn
          ),
        ),
        chargeDetails = List.empty
      )
      DashboardService
        .toSummaryList(EtmpTransactionSummaryResponse(resp))
        .map(_.submissionId) mustBe expectedSubmissionIds
    }
    "Include all submission IDs if there are only one" in {
      val expectedSubmissionIds = List(Fixtures.testSubmissionId.value)
      val resp = buildEtmpSuccessResponse(
        transactionDetails = List(
          buildSubmissionDetails(
            submissionId = Fixtures.testSubmissionId,
            isPast = false,
            utrn = testUtrn
          )
        ),
        chargeDetails = List.empty
      )
      DashboardService
        .toSummaryList(EtmpTransactionSummaryResponse(resp))
        .map(_.submissionId) mustBe expectedSubmissionIds
    }
    "Make the due date for a submission the earliest due date of a charge" in {
      val otherUtrn = "00998877"
      val unconnectedUtrn = "1357911"
      val expectedDueDate = LocalDate.parse("2037-02-01")
      val resp = buildEtmpSuccessResponse(
        transactionDetails = List(
          buildSubmissionDetails(
            submissionId = Fixtures.testSubmissionId,
            isPast = false,
            utrn = testUtrn
          ),
          buildSubmissionDetails(
            submissionId = Fixtures.testSubmissionId,
            isPast = false,
            utrn = otherUtrn
          ),
        ),
        chargeDetails = List(
          buildTaxCharge(otherUtrn, false, false).withDueDate(expectedDueDate),
          buildTaxCharge(otherUtrn, false, false).withDueDate(expectedDueDate.plusDays(1)),
          buildTaxCharge(testUtrn, false, false).withDueDate(expectedDueDate.plusDays(2)),
          buildTaxCharge(unconnectedUtrn, false, false).withDueDate(expectedDueDate.minusDays(2)),
        )
      )
      DashboardService
        .toSummaryList(EtmpTransactionSummaryResponse(resp))
        .filter(_.submissionId == Fixtures.testSubmissionId.value)
        .head
        .submissionId == Fixtures.testSubmissionId.value
    }
    "Make the status if a submission overdue if any charge is overdue" in {
      val otherUtrn = "00998877"
      val unconnectedUtrn = "1357911"
      val expectedDueDate = LocalDate.parse("2037-02-01")
      val resp = buildEtmpSuccessResponse(
        transactionDetails = List(
          buildSubmissionDetails(
            submissionId = Fixtures.testSubmissionId,
            isPast = false,
            utrn = testUtrn
          ),
          buildSubmissionDetails(
            submissionId = Fixtures.testSubmissionId,
            isPast = false,
            utrn = otherUtrn
          ),
        ),
        chargeDetails = List(
          buildTaxCharge(otherUtrn, false, false).withDueDate(expectedDueDate),
          buildTaxCharge(otherUtrn, false, false).withDueDate(expectedDueDate.plusDays(1)),
          buildTaxCharge(testUtrn, true, false),
          buildTaxCharge(unconnectedUtrn, false, false).withDueDate(expectedDueDate.minusDays(2)),
        )
      )
      DashboardService
        .toSummaryList(EtmpTransactionSummaryResponse(resp))
        .filter(_.submissionId == Fixtures.testSubmissionId.value)
        .head
        .status mustBe Overdue
    }
    "Make the status if a submission ready to pay if any charge is unpaid but not late" in {
      val otherUtrn = "00998877"
      val unconnectedUtrn = "1357911"
      val resp = buildEtmpSuccessResponse(
        transactionDetails = List(
          buildSubmissionDetails(
            submissionId = Fixtures.testSubmissionId,
            isPast = false,
            utrn = testUtrn
          ),
          buildSubmissionDetails(
            submissionId = Fixtures.testSubmissionId,
            isPast = false,
            utrn = otherUtrn
          ),
        ),
        chargeDetails = List(
          buildTaxCharge(otherUtrn, false, false),
          buildTaxCharge(otherUtrn, false, false),
          buildTaxCharge(testUtrn, false, false),
          buildTaxCharge(unconnectedUtrn, false, false),
        )
      )
      DashboardService
        .toSummaryList(EtmpTransactionSummaryResponse(resp))
        .filter(_.submissionId == Fixtures.testSubmissionId.value)
        .head
        .status mustBe ReadyToPay
    }
  }
}
