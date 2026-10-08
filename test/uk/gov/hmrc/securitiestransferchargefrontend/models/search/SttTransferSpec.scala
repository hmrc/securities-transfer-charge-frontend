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
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.EtmpSuccessResponseExtensions.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SubmissionStatus.{Overdue, Paid, ReadyToPay}

import java.time.LocalDate

class SttTransferSpec extends AnyPropSpec with ScalaCheckPropertyChecks with Matchers {

  val genUtrn: Gen[String] = Gen.listOfN(8, Gen.numChar).map(_.mkString)

  val sttCharge: (String, String) = ("STT", "Securities transfer tax")
  val lateFilingPenalty: (String, String) = ("LFP", "Late filing penalty")
  val latePaymentPenalty: (String, String) = ("LPP", "Late payment penalty")
  val latePaymentInterest: (String, String) = ("LPI", "Late payment interest")

  def genNonEmptyStringWithMaxSize(max: Int): Gen[String] =
    Gen.alphaStr.suchThat(_.nonEmpty).map(_.take(max))

  val localDatePlusMinus30Days: Gen[LocalDate] = Gen.delay {
    val today = LocalDate.now()
    val minEpoch = today.minusDays(30).toEpochDay
    val maxEpoch = today.plusDays(30).toEpochDay

    Gen.chooseNum[Long](minEpoch, maxEpoch).map(LocalDate.ofEpochDay)
  }

  val genEtmpChargeDetail: Tuple2[String, String] => Gen[EtmpChargeDetail] = typeAndDesc => for {
    utrn <- genUtrn
    (typ, desc) = typeAndDesc
    ref <- genNonEmptyStringWithMaxSize(24)
    total <- Gen.chooseNum(0, 1_000_000_000)
    due <- localDatePlusMinus30Days
    pending <- Gen.posNum[Int].suchThat(_ < total)
  } yield EtmpChargeDetail(utrn, desc, ref, typ, total, due, pending)

  val genTaxCharge: Gen[EtmpChargeDetail] = genEtmpChargeDetail(sttCharge)
  val genLfpCharge: Gen[EtmpChargeDetail] = genEtmpChargeDetail(lateFilingPenalty)
  val genLppCharge: Gen[EtmpChargeDetail] = genEtmpChargeDetail(latePaymentPenalty)
  val genLpiCharge: Gen[EtmpChargeDetail] = genEtmpChargeDetail(latePaymentInterest)

  val withZeroPending: EtmpChargeDetail => EtmpChargeDetail = _.copy(chargeAmountPending = 0)
  val dueInThePast: EtmpChargeDetail => EtmpChargeDetail = _.copy(chargeDueDate = LocalDate.now().minusDays(2))
  val dueInTheFuture: EtmpChargeDetail => EtmpChargeDetail = _.copy(chargeDueDate = LocalDate.now().plusDays(2))

  val genTransfer: Seq[EtmpChargeDetail] => Gen[SttTransfer] = chgs => for {
    utrn <- genUtrn
    buyers <- Gen.alphaStr
    sellers <- Gen.alphaStr
    company <- Gen.alphaStr
  } yield SttTransfer(
    utrn = utrn,
    buyerNames = buyers,
    sellerNames = Some(sellers),
    companyName = company,
    charges = chgs
  )

  val genListOfCharges: Gen[Seq[EtmpChargeDetail]] = {
    for {
      tax <- genTaxCharge
      lfp <- Gen.option(genLfpCharge)
      lpp <- Gen.option(genLppCharge)
      lpiCount <- lpp match {
        case Some(_) => Gen.choose(0, 10)
        case None => Gen.const(0)
      }
      lpis <- Gen.listOfN(lpiCount, genLpiCharge)
    } yield {
      List(tax) ++ lfp.toList ++ lpp.toList ++ lpis
    }
  }

  val genTransferWithTaxes: Gen[SttTransfer] = for {
    cs <- genListOfCharges
    tf <- genTransfer(cs)
  } yield tf

  implicit val config: PropertyCheckConfiguration =
    PropertyCheckConfiguration(
      minSuccessful = 50,
      maxDiscardedFactor = 4.0
    )

  property("A charge should be considered paid if the pending amount is zero") {
    forAll(genTaxCharge) { chg =>
      val zeroPending = withZeroPending(chg)
      zeroPending.isPaid mustBe true
      zeroPending.isOverdue mustBe false
      zeroPending.isUnpaid mustBe false
    }
  }

  property("A charge should be considered unpaid if the pending amount is not zero but the due date is in the future") {
    forAll(genTaxCharge.suchThat(_.chargeAmountPending > 0)) { chg =>
      val toTest = dueInTheFuture(chg)
      toTest.isPaid mustBe false
      toTest.isUnpaid mustBe true
      toTest.isOverdue mustBe false
    }
  }

  property("A charge should be considered overdue if the pending amount is not zero and the due date is in the past") {
    forAll(genTaxCharge.suchThat(_.chargeAmountPending > 0)) { chg =>
      val toTest = dueInThePast(chg)
      toTest.isPaid mustBe false
      toTest.isUnpaid mustBe false
      toTest.isOverdue mustBe true
    }
  }

  property("A transfer with tax to pay should report taxes correctly") {
    forAll(genTransferWithTaxes) { tf =>
      val expectedTax = tf.charges.filter(_.isTaxCharge).map(_.chargeAmountPending).sum
      tf.taxToPay mustBe expectedTax
    }
  }

  property("A transfer with late filing penalties should report taxes correctly") {
    forAll(genTransferWithTaxes) { tf =>
      val expectedTax = tf.charges.filter(_.isLateFilingPenalty).map(_.chargeAmountPending).sum
      tf.lateFilingPenaltiesToPay mustBe expectedTax
    }
  }

  property("A transfer with late payment penalties should report taxes correctly") {
    forAll(genTransferWithTaxes) { tf =>
      val expectedTax = tf.charges.filter(_.isLatePaymentPenalty).map(_.chargeAmountPending).sum
      tf.latePaymentPenaltiesToPay mustBe expectedTax
    }
  }

  property("A transfer with late payment interest should report taxes correctly") {
    forAll(genTransferWithTaxes) { tf =>
      val expectedTax = tf.charges.filter(_.isLatePaymentInterest).map(_.chargeAmountPending).sum
      tf.latePaymentInterestToPay mustBe expectedTax
    }
  }

  property("A transfer should report its original charge amounts correctly") {
    forAll(genTransferWithTaxes) { tf =>
      val expected = tf.charges.map(_.chargeAmountTotal).sum
      tf.originalTotalAmount mustBe expected
    }
  }

  property("A transfer should report its due by date correctly") {
    forAll(genTransferWithTaxes) { tf =>
      val expected = tf.charges.map(_.chargeDueDate).min
      tf.paymentDueByDate mustBe expected
    }
  }

  property("A transfer should report its status as overdue if one of its charges is overdue") {
    forAll(genTransferWithTaxes) { tf =>
      if tf.charges.exists(_.isOverdue)
        then tf.status mustBe Overdue
      else if tf.charges.exists(_.isUnpaid)
        then tf.status mustBe ReadyToPay
      else tf.status mustBe Paid
    }
  }
}
