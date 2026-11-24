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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import java.time.LocalDate

class SttSubmissionNumberingSpec extends AnyFreeSpec with Matchers {
  import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SttSubmissionNumberingSpec.*

  "SttSubmission transfer numbering" - {
    "numbers transfers in UTRN order, whatever order they arrive in" in {
      val sub = submissionOf("900459000003", "900459000001", "900459000002")
      sub.transferAt(1).map(_.utrn) mustBe Some("900459000001")
      sub.transferAt(2).map(_.utrn) mustBe Some("900459000002")
      sub.transferAt(3).map(_.utrn) mustBe Some("900459000003")
    }

    "sorts UTRNs of different lengths numerically" in {
      val sub = submissionOf("10", "9", "100")
      sub.transfers.flatMap(t => sub.transferNumber(t).map(t.utrn -> _)).toMap mustBe Map("9" -> 1, "10" -> 2, "100" -> 3)
    }

    "returns no number for a transfer that is not in the submission" in {
      val sub = submissionOf("900459000001")
      sub.transferNumber(transferWithUtrn("900459000099")) mustBe None
    }
  }
}

object SttSubmissionNumberingSpec {
  def transferWithUtrn(utrn: String): SttTransfer =
    SttTransfer(utrn = utrn, buyerNames = "Buyer", sellerNames = None, companyName = "Company", charges = Seq.empty)

  def submissionOf(utrns: String*): SttSubmission =
    SttSubmission("STT-000000001", LocalDate.of(2026, 11, 1), None, None, utrns.map(transferWithUtrn))
}
