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

package viewmodels.dashboard

import base.{Fixtures, SpecBase}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import uk.gov.hmrc.securitiestransferchargefrontend.config.CurrencyFormatter
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard.routes as dashboardRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.pay.routes as payRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.{EtmpChargeDetail, SttTransfer}
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.{BuyerTaxRate, ViewTransferResponse}
import uk.gov.hmrc.securitiestransferchargefrontend.utils.DateTimeFormats
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard.TransferDetailsViewModel

import java.time.LocalDate

class TransferDetailsViewModelSpec extends SpecBase with GuiceOneAppPerSuite {

  implicit private val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(fakeRequest)

  private val utrn = Fixtures.testUtrn
  private val subId = Fixtures.testSubmissionId.toString

  private val futureDate = LocalDate.now().plusDays(10)
  private val pastDate = LocalDate.now().minusDays(10)

  private def charge(chargeType: String, ref: String, amount: BigDecimal, pending: BigDecimal, due: LocalDate): EtmpChargeDetail =
    EtmpChargeDetail(
      utrn = utrn,
      chargeTypeDescription = chargeType,
      chargeReference = ref,
      chargeType = chargeType,
      chargeAmountTotal = amount,
      chargeDueDate = due,
      chargeAmountPending = pending
    )

  private def tax(pending: BigDecimal = 5000, due: LocalDate = futureDate) = charge("STT", "TAX1", 5000, pending, due)
  private def lateFiling(pending: BigDecimal = 50) = charge("LFP", "LFP1", 50, pending, futureDate)
  private def latePayment(pending: BigDecimal = 100.01) = charge("LPP", "LPP1", 100.01, pending, pastDate)
  private def lateInterest(pending: BigDecimal = 12.5) = charge("LPI", "LPI1", 12.5, pending, pastDate)

  private def transferWith(charges: EtmpChargeDetail*): SttTransfer =
    Fixtures.readyToPaySttTransferTaxOnly.copy(utrn = utrn, charges = charges)

  private def build(
    transfer: SttTransfer,
    transferNumber: Int = 1,
    transaction: ViewTransferResponse = Fixtures.viewTransferResponse,
    affinityKey: String = "agent"
  ): TransferDetailsViewModel =
    TransferDetailsViewModel.build(
      Fixtures.singleSubmissionReadyToPay.copy(transfers = Seq(transfer)),
      transferNumber,
      transfer,
      transaction,
      affinityKey
    )

  private def keys(list: SummaryList): Seq[String] = list.rows.map(_.key.content.asHtml.toString)
  private def value(row: SummaryListRow): String = row.value.content.asHtml.toString
  private def rowFor(list: SummaryList, key: String): Option[SummaryListRow] =
    list.rows.find(_.key.content.asHtml.toString == messages(key))
  private def valueOf(list: SummaryList, key: String): String = value(rowFor(list, key).get)

  private def date(d: LocalDate): String = DateTimeFormats.formatDate(d)(messages.lang)

  "TransferDetailsViewModel.build" - {

    "transfer number (AC15/AC16)" - {
      "uses the transfer number it is given for the heading" in {
        build(transferWith(tax()), transferNumber = 3).transferNumber mustBe 3
      }
    }

    "balance to pay" - {
      "only includes unpaid charges (AC2)" in {
        val model = build(transferWith(tax(pending = 0), lateFiling()))
        model.balanceToPay mustBe "£50"
      }

      "is £0 when everything is paid (AC3)" in {
        val model = build(transferWith(tax(pending = 0), lateFiling(pending = 0)))
        model.balanceToPay mustBe "£0"
      }

      "uses the shared currency formatting" in {
        val model = build(transferWith(tax(pending = 9000000), latePayment()))
        model.balanceToPay mustBe CurrencyFormatter.currencyFormat(BigDecimal(9000100.01))
        model.balanceToPay mustBe "£9,000,100.01"
      }
    }

    "pay full balance button" - {
      "links to OPS for the whole transfer when there are unpaid charges (AC4)" in {
        build(transferWith(tax(), lateFiling()), transferNumber = 2).payFullBalanceUrl mustBe
          Some(payRoutes.PaymentController.startTransferPayment(subId, 2).url)
      }

      "is not shown when all charges are paid (AC5)" in {
        build(transferWith(tax(pending = 0), lateFiling(pending = 0))).payFullBalanceUrl mustBe None
      }
    }

    "charges list" - {
      "is not shown when all charges are paid (AC3/AC5)" in {
        build(transferWith(tax(pending = 0), lateFiling(pending = 0))).chargesList mustBe None
      }

      "does not show a paid charge (AC6)" in {
        val charges = build(transferWith(tax(pending = 0), lateFiling())).chargesList.value
        keys(charges) mustBe Seq(messages("transferDetails.charges.lateFilingPenalty"))
      }

      "lists unpaid charges in the design order regardless of the order ETMP sends them" in {
        val charges = build(transferWith(lateInterest(), latePayment(), lateFiling(), tax())).chargesList.value
        keys(charges) mustBe Seq(
          messages("transferDetails.charges.tax"),
          messages("transferDetails.charges.lateFilingPenalty"),
          messages("transferDetails.charges.latePaymentPenalty"),
          messages("transferDetails.charges.latePaymentInterest")
        )
      }

      "shows '<amount> due by <date>' for a tax charge that is due (AC7)" in {
        val charges = build(transferWith(tax(pending = 45.01))).chargesList.value
        value(charges.rows.head) mustBe messages("transferDetails.charges.dueBy", "£45.01", date(futureDate))
      }

      "shows '<amount> was due by <date>' for a tax charge that is overdue (AC8)" in {
        val charges = build(transferWith(tax(pending = 45.01, due = pastDate))).chargesList.value
        value(charges.rows.head) mustBe messages("transferDetails.charges.wasDueBy", "£45.01", date(pastDate))
      }

      "shows only '<amount> due' for penalties and interest, even when overdue (AC9)" in {
        val charges = build(transferWith(lateFiling(), latePayment(), lateInterest())).chargesList.value
        charges.rows.map(value) mustBe Seq(
          messages("transferDetails.charges.due", "£50"),
          messages("transferDetails.charges.due", "£100.01"),
          messages("transferDetails.charges.due", "£12.50")
        )
      }

      "gives each unpaid charge a pay link to OPS for that charge only, with hidden text (AC7-AC9)" in {
        val charges = build(transferWith(tax(), latePayment()), transferNumber = 2).chargesList.value

        val expected = Seq(
          ("TAX1", "transferDetails.charges.pay.hidden.tax"),
          ("LPP1", "transferDetails.charges.pay.hidden.latePaymentPenalty")
        )
        charges.rows.zip(expected).foreach { case (row, (ref, hiddenKey)) =>
          val items = row.actions.value.items
          items.size mustBe 1
          val action = items.head
          action.href mustBe payRoutes.PaymentController.startChargePayment(subId, 2, ref).url
          action.content.asHtml.toString mustBe messages("transferDetails.charges.pay")
          action.visuallyHiddenText mustBe Some(messages(hiddenKey))
        }
      }
    }

    "view paid charges link" - {
      "is shown when at least one charge has been paid (AC17)" in {
        build(transferWith(tax(), lateFiling(pending = 0)), transferNumber = 2).paidChargesUrl mustBe
          Some(dashboardRoutes.PaidChargesController.onPageLoad(subId, 2).url)
      }

      "is not shown when no charges have been paid (AC18)" in {
        build(transferWith(tax(), lateFiling())).paidChargesUrl mustBe None
      }
    }

    "transfer reference" - {
      "shows the UTRN when the tax charge has been paid" in {
        val model = build(transferWith(tax(pending = 0), lateFiling()))
        valueOf(model.referenceList, "transferDetails.transferReference") mustBe utrn
      }

      "shows the UTRN when no tax was due (e.g. a relief), even if penalties are unpaid" in {
        val model = build(transferWith(tax(pending = 0).copy(chargeAmountTotal = 0), lateFiling()))
        valueOf(model.referenceList, "transferDetails.transferReference") mustBe utrn
      }

      "shows the UTRN when the transfer has no tax charge" in {
        val model = build(transferWith(lateFiling()))
        valueOf(model.referenceList, "transferDetails.transferReference") mustBe utrn
      }

      "shows 'No data' when the tax charge is unpaid but other charges are paid (AC12)" in {
        val model = build(transferWith(tax(), lateFiling(pending = 0)))
        valueOf(model.referenceList, "transferDetails.transferReference") mustBe messages("transferDetails.noData")
      }

      "shows 'No data' when no charges are paid (AC14)" in {
        val model = build(transferWith(tax(), lateFiling()))
        valueOf(model.referenceList, "transferDetails.transferReference") mustBe messages("transferDetails.noData")
      }

      "links 'Part of submission' to the submission details page" in {
        val model = build(transferWith(tax()))
        val link = valueOf(model.referenceList, "transferDetails.partOfSubmission")
        link must include(s"""href="${dashboardRoutes.SubmissionDetailsController.onPageLoad(subId).url}"""")
        link must include(subId)
      }

      "shows the submission date in written format" in {
        val model = build(transferWith(tax()))
        valueOf(model.referenceList, "transferDetails.submissionDate") mustBe
          date(Fixtures.singleSubmissionReadyToPay.submissionDate)
      }
    }

    "buyer and seller details" - {
      "show the main buyer's name and address" in {
        val model = build(transferWith(tax()))
        valueOf(model.buyerList.value, "transferDetails.name") mustBe "Northbridge Healthcare"
        valueOf(model.buyerList.value, "transferDetails.address") mustBe "50 Medical Park<br>London<br>W0 4DD"
      }

      "show the main seller's name and address" in {
        val model = build(transferWith(tax()))
        valueOf(model.sellerList, "transferDetails.name") mustBe "CardioNova Therapeutics"
        valueOf(model.sellerList, "transferDetails.address") mustBe "20 Discovery campus<br>Cambridge<br>CB0 5EE"
      }

      "use a narrow key column like the design" in {
        val model = build(transferWith(tax()))
        Seq(model.referenceList, model.buyerList.value, model.sellerList, model.transferList).flatMap(_.rows).foreach { r =>
          r.key.classes must include("govuk-!-width-one-third")
        }
      }

      "escape names and addresses" in {
        val transaction = Fixtures.viewTransferResponse.copy(
          mainBuyerDetails = Fixtures.viewTransferResponse.mainBuyerDetails.copy(buyerName = "<b>Buyer</b>", addr1 = "1 & 2 Road")
        )
        val model = build(transferWith(tax()), transaction = transaction)
        valueOf(model.buyerList.value, "transferDetails.name") mustBe "&lt;b&gt;Buyer&lt;/b&gt;"
        valueOf(model.buyerList.value, "transferDetails.address") must startWith("1 &amp; 2 Road")
      }

      "show buyer details for an agent" in {
        build(transferWith(tax()), affinityKey = "agent").buyerList mustBe defined
      }

      "not show buyer details for an organisation or an individual" in {
        build(transferWith(tax()), affinityKey = "org").buyerList mustBe None
        build(transferWith(tax()), affinityKey = "individual").buyerList mustBe None
      }

      "omit the seller address when ETMP has no main seller" in {
        val model = build(transferWith(tax()), transaction = Fixtures.viewTransferResponse.copy(mainSellerDetails = None))
        rowFor(model.sellerList, "transferDetails.address") mustBe None
      }
    }

    "transfer details (AC15)" - {
      "show the answers ETMP holds for the transfer" in {
        val list = build(transferWith(tax())).transferList

        keys(list) mustBe Seq(
          "agent.connectedPersons.checkYourAnswersLabel",
          "agent.applyingForRelief.checkYourAnswersLabel",
          "whatReliefAreYouApplyingFor.checkYourAnswersLabel",
          "agent.transferDetails.businessName",
          "transferDetails.crn",
          "agent.chargingPoint.checkYourAnswersLabel",
          "taxRate.checkYourAnswersLabel",
          "agent.purchasingShares.checkYourAnswersLabel",
          "agent.otherSecuritiesType.checkYourAnswersLabel",
          "transferDetails.numberOfSecurities",
          "agent.amountPaidForSecurities.checkYourAnswersLabel",
          "agent.totalMarketValue.checkYourAnswersLabel"
        ).map(messages(_))

        list.rows.map(value) mustBe Seq(
          messages("site.no"),
          messages("site.yes"),
          "Intra-group relief",
          "CardioNova Therapeutics",
          "12345678",
          "1 July 2026",
          messages("taxRate.half"),
          "Ordinary",
          "Ordinary",
          "100",
          "£12,000,000",
          "£12,500,000"
        )
      }

      "show 'No' for relief and leave out the relief and optional rows when they are not present" in {
        val details = Fixtures.viewTransferResponse.transactionDetails.copy(
          reliefClaimedName = None,
          companyRegistrationNumber = None,
          marketValue = None,
          isConnectedPartiesTransactions = true
        )
        val list = build(transferWith(tax()), transaction = Fixtures.viewTransferResponse.copy(transactionDetails = details)).transferList

        valueOf(list, "agent.applyingForRelief.checkYourAnswersLabel") mustBe messages("site.no")
        valueOf(list, "agent.connectedPersons.checkYourAnswersLabel") mustBe messages("site.yes")
        rowFor(list, "whatReliefAreYouApplyingFor.checkYourAnswersLabel") mustBe None
        rowFor(list, "transferDetails.crn") mustBe None
        rowFor(list, "agent.totalMarketValue.checkYourAnswersLabel") mustBe None
      }

      "show the type of securities from ETMP, straight after the purchasing shares row" in {
        val details = Fixtures.viewTransferResponse.transactionDetails.copy(typeOfSecurity = "Preference")
        Seq("agent", "org").foreach { key =>
          val list = build(transferWith(tax()), transaction = Fixtures.viewTransferResponse.copy(transactionDetails = details), affinityKey = key).transferList
          val labels = keys(list)
          valueOf(list, s"$key.otherSecuritiesType.checkYourAnswersLabel") mustBe "Preference"
          labels.indexOf(messages(s"$key.otherSecuritiesType.checkYourAnswersLabel")) mustBe
            labels.indexOf(messages(s"$key.purchasingShares.checkYourAnswersLabel")) + 1
        }
      }

      Seq("an organisation" -> "org", "an individual" -> "individual").foreach { case (userType, key) =>
        s"use the $key labels for $userType" in {
          val list = build(transferWith(tax()), affinityKey = key).transferList

          val expectedKeys = Seq(
            s"$key.connectedPersons.checkYourAnswersLabel",
            s"$key.applyingForRelief.checkYourAnswersLabel",
            "whatReliefAreYouApplyingFor.checkYourAnswersLabel",
            s"$key.transferDetails.businessName",
            "transferDetails.crn",
            s"$key.chargingPoint.checkYourAnswersLabel",
            "taxRate.checkYourAnswersLabel",
            s"$key.purchasingShares.checkYourAnswersLabel",
            s"$key.otherSecuritiesType.checkYourAnswersLabel",
            "transferDetails.numberOfSecurities",
            s"$key.amountPaidForSecurities.checkYourAnswersLabel",
            s"$key.totalMarketValue.checkYourAnswersLabel"
          )

          // messages() falls back to the key itself, so check every label really exists
          expectedKeys.filterNot(messages.isDefinedAt) mustBe empty
          keys(list) mustBe expectedKeys.map(messages(_))
        }
      }

      "show the business name with the label for each user type" in {
        Seq("agent", "org", "individual").foreach { key =>
          val list = build(transferWith(tax()), affinityKey = key).transferList
          valueOf(list, s"$key.transferDetails.businessName") mustBe "CardioNova Therapeutics"
        }
      }

      "show the number of securities with thousands separators" in {
        val details = Fixtures.viewTransferResponse.transactionDetails.copy(numberOfShares = 1234567)
        val list = build(transferWith(tax()), transaction = Fixtures.viewTransferResponse.copy(transactionDetails = details)).transferList
        valueOf(list, "transferDetails.numberOfSecurities") mustBe "1,234,567"
      }

      "show the 1.5% tax rate" in {
        val transaction = Fixtures.viewTransferResponse.copy(
          mainBuyerDetails = Fixtures.viewTransferResponse.mainBuyerDetails.copy(taxRate = BuyerTaxRate.OneAndHalfPercent)
        )
        valueOf(build(transferWith(tax()), transaction = transaction).transferList, "taxRate.checkYourAnswersLabel") mustBe
          messages("taxRate.oneAndHalf")
      }
    }
  }
}
