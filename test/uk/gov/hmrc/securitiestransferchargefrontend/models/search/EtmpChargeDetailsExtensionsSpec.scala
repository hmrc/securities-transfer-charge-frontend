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

import java.time.LocalDate

class EtmpChargeDetailsExtensionsSpec extends AnyPropSpec with ScalaCheckPropertyChecks with Matchers {

  val genUtrn: Gen[String] = Gen.listOfN(8, Gen.numChar).map(_.mkString)
  val genChargeTypeAndDescription: Gen[(String, String)] = Gen.oneOf(
    ("LPP", "Late payment penalty"),
    ("LPI", "Late payment interest"),
    ("LFP", "Late filing penalty"),
    ("STT", "Securities transfer tax")
  )

  def genNonEmptyStringWithMaxSize(max: Int): Gen[String] =
    Gen.alphaStr.suchThat(_.nonEmpty).map(_.take(max))

  val localDatePlusMinus30Days: Gen[LocalDate] = Gen.delay {
    val today = LocalDate.now()
    val minEpoch = today.minusDays(30).toEpochDay
    val maxEpoch = today.plusDays(30).toEpochDay

    Gen.chooseNum[Long](minEpoch, maxEpoch).map(LocalDate.ofEpochDay)
  }

  val genEtmpChargeDetail: Gen[EtmpChargeDetail] = for {
    utrn <- genUtrn
    (typ, desc) <- genChargeTypeAndDescription
    ref <- genNonEmptyStringWithMaxSize(24)
    total <- Gen.chooseNum(0, 1_000_000_000)
    due <- localDatePlusMinus30Days
    pending <- Gen.posNum[Int].suchThat(_ < total)
  } yield EtmpChargeDetail(utrn, desc, ref, typ, total, due, pending)

  val withZeroPending: EtmpChargeDetail => EtmpChargeDetail = _.copy(chargeAmountPending = 0)
  val dueInThePast: EtmpChargeDetail => EtmpChargeDetail = _.copy(chargeDueDate = LocalDate.now().minusDays(2))
  val dueInTheFuture: EtmpChargeDetail => EtmpChargeDetail = _.copy(chargeDueDate = LocalDate.now().plusDays(2))

  property("A charge should be considered paid if the pending amount is zero") {
    forAll(genEtmpChargeDetail) { chg =>
      val zeroPending = withZeroPending(chg)
      zeroPending.isPaid mustBe true
      zeroPending.isOverdue mustBe false
      zeroPending.isUnpaid mustBe false
    }
  }

  property("A charge should be considered unpaid if the pending amount is not zero but the due date is in the future") {
    forAll(genEtmpChargeDetail.suchThat(_.chargeAmountPending > 0)) { chg =>
      val toTest = dueInTheFuture(chg)
      toTest.isPaid mustBe false
      toTest.isUnpaid mustBe true
      toTest.isOverdue mustBe false
    }
  }

  property("A charge should be considered overdue if the pending amount is not zero and the due date is in the past") {
    forAll(genEtmpChargeDetail.suchThat(_.chargeAmountPending > 0)) { chg =>
      val toTest = dueInThePast(chg)
      toTest.isPaid mustBe false
      toTest.isUnpaid mustBe false
      toTest.isOverdue mustBe true
    }
  }

}
