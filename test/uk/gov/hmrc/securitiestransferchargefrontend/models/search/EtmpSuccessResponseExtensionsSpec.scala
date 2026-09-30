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

package uk.gov.hmrc.securitiestransferchargefrontend.models.search

import base.{Fixtures, SpecBase}
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.EtmpSuccessResponseExtensions.*

class EtmpSuccessResponseExtensionsSpec extends SpecBase with Matchers {

  import Fixtures.{buildEtmpSuccessResponse, buildSubmissionDetails, buildTaxCharge}
  
  "Transaction details" - {
    "with one transfer and no charges should parse" in {
      val singleTransferWithRelief = buildEtmpSuccessResponse(
        transactionDetails = List(
          buildSubmissionDetails(Fixtures.testSubmissionId, false, Fixtures.testUtrn)
        ),
        chargeDetails = List.empty
      )
      singleTransferWithRelief.allSubmissionIds mustBe List(Fixtures.testSubmissionId)
      val transformed = singleTransferWithRelief.toSttSubmissions
      transformed.length mustBe 1
      val submission = transformed.head
      submission.submissionId mustBe Fixtures.testSubmissionId.value
      val transfers = submission.transfers
      transfers.length mustBe 1
      val transfer = transfers.head
      transfer.utrn mustBe Fixtures.testUtrn
      transfer.charges.isEmpty mustBe true
    }
    "with two transfers and one charge should parse" in {
      val otherUtrn = "11223344"
      val allTransfers = buildEtmpSuccessResponse(
        transactionDetails = List(
          buildSubmissionDetails(Fixtures.testSubmissionId, false, Fixtures.testUtrn),
          buildSubmissionDetails(Fixtures.testSubmissionId, false, otherUtrn)
        ),
        chargeDetails = List(
          buildTaxCharge(otherUtrn, false, false)
        )
      )
      allTransfers.allSubmissionIds mustBe List(Fixtures.testSubmissionId)
      val transformed = allTransfers.toSttSubmissions
      transformed.length mustBe 1
      val submission = transformed.head
      submission.submissionId mustBe Fixtures.testSubmissionId.value
      val transfers = submission.transfers
      transfers.length mustBe 2
      val transfer = transfers.head
      transfer.utrn mustBe Fixtures.testUtrn
      transfer.charges.isEmpty mustBe true
      val otherTransfer = transfers(1)
      otherTransfer.utrn mustBe otherUtrn
      val taxCharge = otherTransfer.charges.head
      taxCharge.utrn mustBe otherUtrn
    }
  }
}
