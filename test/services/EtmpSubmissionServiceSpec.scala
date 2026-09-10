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

package services

import base.SpecBase
import clients.FakeEtmpSubmissionClient
import org.mockito.Mockito.*
import org.scalacheck.Gen
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.HowToNotifyAboutSecuritiesTransfer.OneAtATime
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.RoleAtPurchasingCompanyPage
import uk.gov.hmrc.securitiestransferchargefrontend.services.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.AgentReferencePage
import java.time.LocalDate

class EtmpSubmissionServiceSpec extends AnyFreeSpec with Matchers with SpecBase with ScalaFutures with MockitoSugar {

  private val mockUserAnswers = mock[UserAnswers]

  private val tx = StfTransaction(
    howToNotifyAboutSecuritiesTransfer = OneAtATime,
    agentReference = None,
    confirmedAddress = Some(ConfirmableAddress(List("123 Main Street", "London"), "SW1A 1AA", Some(Country("United Kingdom", "GB")))),
    nameofBuyer = Some("John Smith"),
    buyerAddress = Some(AlfConfirmedAddress("audit-ref-123", Some("id-123"), AlfAddress(List("123 Main Street", "London"), "SW1A 1AA", Country("United Kingdom", "GB")))),
    nameOfSeller = "Helen Jones",
    sellerAddress = AlfConfirmedAddress("audit-ref-456", Some("id-456"), AlfAddress(List("456 High Street", "Manchester"), "M1 1AA", Country("United Kingdom", "GB"))),
    connectedPersons = true,
    applyingForRelief = false,
    whatReliefAreYouApplyingFor = None,
    securitiesTarget = SecuritiesTarget("Company ABC Ltd", Some("12345678")),
    chargingPoint = LocalDate.of(2026, 6, 15),
    taxRate = TaxRate.HalfPercent,
    purchasingShares = true,
    detailsOfThisTransfer = Some(DetailsOfThisTransfer(
      numberOfShares = 100,
      typeOfShares = "ordinary shares",
      amountPaid = BigDecimal("8000.00"),
      marketValue = Some(BigDecimal("10000.50"))
    )),
    otherSecuritiesType = None,
    amountPaidForSecurities = None,
    totalMarketValue = None
  )

  when(mockUserAnswers.get(StfTransaction)).thenReturn(Some(tx))
  when(mockUserAnswers.get(RoleAtPurchasingCompanyPage)).thenReturn(None)
  when(mockUserAnswers.get(AgentReferencePage)).thenReturn(None)
  when(mockUserAnswers.submissionId).thenReturn(submissionId)

  val affinityData: AffinityData = Individual(
    name = "John Smith",
    address = Address(
      addressLine1 = "10 High Street",
      addressLine2 = Some("Bolton"),
      addressLine3 = None,
      postcode = "BL1 2TG",
      countryCode = "UK"
    ),
    phone = "01234 567890",
    email = "jsmith@foo.com",
    nino = "NX787356B" // Can come from subscription
  )

  val successfulChargeGen: Gen[StcCharge] = for {
    recordId              <- Gen.chooseNum(1, 1000)
    utrn                  <- Gen.chooseNum(10000000, 99999999).map(_.toString)
    (chargeTypeDescription, chargeType) <- Gen.oneOf(
      ("Securities transfer tax", "STT"),
      ("Late submission", "LSP"),
      ("Late Payment", "LPP"),
      ("Late Payment Interest", "LPI")
    )
    chargeReference       <- Gen.chooseNum(1, 1000).map(n => s"STT$n")
    chargeAmount          <- Gen.chooseNum(100d, 1000d)
    chargeDueDate         = "2026-09-09"
  } yield StcChargeSuccess(recordId, utrn, chargeTypeDescription, chargeReference, chargeType, chargeAmount, chargeDueDate)

  val failedChargeGen: Gen[StcCharge] = for {
    recordId  <- Gen.chooseNum(1, 1000)
    errorCode <- Gen.oneOf("404", "401", "400", "500")
    errorText = "Big error"
  } yield StcChargeFailure(recordId, errorCode, errorText)

  def successfulChargesGen: Gen[List[StcCharge]] = Gen.nonEmptyListOf(successfulChargeGen)
  def failureChargesGen: Gen[List[StcCharge]] = Gen.nonEmptyListOf(failedChargeGen)
  def mixedChargesGen: Gen[List[StcCharge]] = for {
    succ   <- successfulChargeGen
    fail   <- failedChargeGen
    count  <- Gen.chooseNum(0, 10)
    others <- Gen.listOfN(count, Gen.oneOf(successfulChargeGen, failedChargeGen))
  } yield succ :: fail :: others

  def successfulChargesResponseGen: Gen[StcTransactionCreateResponse] = for {
    charges <- successfulChargesGen
  } yield StcTransactionCreateProcessed(
    success = StcTransactionCreateProcessedBody(
      processingDate = "2026-09-01",
      charges = charges
    ))

  def mixedChargesResponseGen: Gen[StcTransactionCreateResponse] = for {
    charges <- mixedChargesGen
  } yield StcTransactionCreateProcessed(
    success = StcTransactionCreateProcessedBody(
      processingDate = "2026-09-01",
      charges = charges
    ))

  def failedChargesResponseGen: Gen[StcTransactionCreateResponse] = for {
    charges <- failureChargesGen
  } yield StcTransactionCreateProcessed(
    success = StcTransactionCreateProcessedBody(
      processingDate = "2026-09-01",
      charges = charges
    ))

  "The service" - {
    "return success if ETMP returns only successful responses" in {
      val resp = successfulChargesResponseGen.sample.get
      val fakeClient = FakeEtmpSubmissionClient(resp)
      val service = new EtmpSubmissionServiceImpl(fakeClient)
      val result = service.submitSingleStf(subscriptionId, mockUserAnswers, affinityData)
        whenReady(result) { r =>
          r mustBe a[SubmissionCreateResponseSuccess]
        }
      }

    "return partial success if ETMP returns a mix of successful and unsuccessful responses" in {
      val resp = mixedChargesResponseGen.sample.get
      val fakeClient = FakeEtmpSubmissionClient(resp)
      val service = new EtmpSubmissionServiceImpl(fakeClient)
      val result = service.submitSingleStf(subscriptionId, mockUserAnswers, affinityData)
      whenReady(result) { r =>
        r mustBe a[SubmissionCreateResponsePartialFailure]
      }
    }
    "return failure if ETMP returns only unsuccessful responses" in {
      val resp = failedChargesResponseGen.sample.get
      val fakeClient = FakeEtmpSubmissionClient(resp)
      val service = new EtmpSubmissionServiceImpl(fakeClient)
      val result = service.submitSingleStf(subscriptionId, mockUserAnswers, affinityData)
      whenReady(result) { r =>
        r mustBe SubmissionCreateResponseFailure
      }
    }
  }
}
