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

import org.scalacheck.Gen
import org.scalatest.matchers.must.Matchers
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.gov.hmrc.securitiestransferchargefrontend.domain.TransferType.{SH03, STF}
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SttSubmissionSpec.genSttSubmission
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SttTransferSpec.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.EtmpSuccessResponseExtensions.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SubmissionStatus.*

class SttSubmissionSpec extends AnyPropSpec with ScalaCheckPropertyChecks with Matchers {

  implicit val config: PropertyCheckConfiguration =
    PropertyCheckConfiguration(
      minSuccessful = 50,
      maxDiscardedFactor = 4.0
    )

  val allCharges: SttSubmission => Seq[EtmpChargeDetail] = _.transfers.flatMap(_.charges)

  property("A submission should correctly report the tax due") {
    forAll(genSttSubmission) { sub =>
      val expected = allCharges(sub).filter(_.isTaxCharge).map(_.chargeAmountPending).sum
      sub.taxToPay mustBe expected
    }
  }

  property("A submission should correctly report the late filing penalties due") {
    forAll(genSttSubmission) { sub =>
      val expected = allCharges(sub).filter(_.isLateFilingPenalty).map(_.chargeAmountPending).sum
      sub.lateFilingPenaltiesToPay mustBe expected
    }
  }

  property("A submission should correctly report the late payment penalties due") {
    forAll(genSttSubmission) { sub =>
      val expected = allCharges(sub).filter(_.isLatePaymentPenalty).map(_.chargeAmountPending).sum
      sub.latePaymentPenaltiesToPay mustBe expected
    }
  }

  property("A submission should correctly report the late payment interest due") {
    forAll(genSttSubmission) { sub =>
      val expected = allCharges(sub).filter(_.isLatePaymentInterest).map(_.chargeAmountPending).sum
      sub.latePaymentInterestToPay mustBe expected
    }
  }

  property("A submission should correctly report the original total amount due") {
    forAll(genSttSubmission) { sub =>
      val expected = allCharges(sub).map(_.chargeAmountTotal).sum
      sub.originalTotalAmount mustBe expected
    }
  }


  property("A submission should correctly report the type of the submission") {
    forAll(genSttSubmission) { sub =>
      val expected = if sub.declareeName.isEmpty then STF else SH03
        sub.submissionType mustBe expected
    }
  }

  property("A submission should correctly report the due by date of the submission") {
    forAll(genSttSubmission) { sub =>
      val reportedDate = sub.paymentDueByDate
      val allDates = allCharges(sub).map(_.chargeDueDate)
      allDates.exists(_.isBefore(reportedDate)) mustBe false
    }
  }

  property("A submission should correctly report the number of transfers in the submission") {
    forAll(genSttSubmission) { sub =>
      val expected = sub.transfers.size
      sub.numberOfTransfers mustEqual expected
    }
  }

  property("A submission should correctly report its status") {
    forAll(genSttSubmission) { sub =>
      val expected = {
        if allCharges(sub).exists(_.isOverdue) then Overdue
        else if allCharges(sub).exists(_.isUnpaid) then ReadyToPay
        else Paid
      }
      sub.submissionStatus mustEqual expected
    }
  }
}


object SttSubmissionSpec {
  def genSubmissionId: Gen[String] = for {
    nums <- Gen.listOfN(9, Gen.numChar)
  } yield s"STT-${nums.mkString}"

  def genSttSubmission: Gen[SttSubmission] = for {
    submissionId       <- genSubmissionId
    submissionDate     <- localDatePlusMinus30Days
    clientReference    <- Gen.option(Gen.alphaNumStr)
    declareeName       <- Gen.option(Gen.alphaNumStr)
    numberOfTransfers  <- Gen.choose(1, 10)
    transfers          <- Gen.listOfN(numberOfTransfers, genTransferWithTaxes)
  } yield SttSubmission(submissionId, submissionDate, clientReference, declareeName, transfers)
}
