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

package uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard

import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import uk.gov.hmrc.securitiestransferchargefrontend.config.CurrencyFormatter
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard.routes as dashboardRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.pay.routes as payRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.domain.TransferType
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.EtmpSuccessResponseExtensions.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.{EtmpChargeDetail, SttSubmission, SttTransfer}
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.{BuyerTaxRate, DeclarationRole, ReasonForPurchase, ViewTransferResponse}
import uk.gov.hmrc.securitiestransferchargefrontend.utils.{CommonHelpers, DateTimeFormats}
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.govuk.summarylist.*

/**
 * Everything the Transfer details page needs.
 *
 * @param chargesList unpaid charges only, each with a pay link; empty when everything is paid
 * @param payFullBalanceUrl only present when there is something left to pay
 * @param paidChargesUrl only present when at least one charge has been paid
 * @param sections the headed summary lists under the reference list, which depend on the type of transfer
 */
final case class TransferDetailsViewModel(
  submissionId      : String,
  transferNumber    : Int,
  balanceToPay      : String,
  payFullBalanceUrl : Option[String],
  chargesList       : Option[SummaryList],
  paidChargesUrl    : Option[String],
  referenceList     : SummaryList,
  sections          : Seq[TransferDetailsSection]
)

final case class TransferDetailsSection(heading: String, list: SummaryList)

object TransferDetailsViewModel {

  // Key column is a third of its list; the value takes the rest (or, for charges, all but the Pay link).
  private val keyWidth = "govuk-!-width-one-third"

  private def row(key: String, value: String)(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key   = KeyViewModel(Text(messages(key))).withCssClass(keyWidth),
      value = ValueViewModel(Text(value))
    )

  private def htmlRow(key: String, html: String)(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key   = KeyViewModel(Text(messages(key))).withCssClass(keyWidth),
      value = ValueViewModel(HtmlContent(html))
    )

  private def formatDate(date: java.time.LocalDate)(implicit messages: Messages): String =
    DateTimeFormats.formatDate(date)(messages.lang)

  private def yesNo(answer: Boolean)(implicit messages: Messages): String =
    messages(if answer then "site.yes" else "site.no")

  /** Charge types in the order the design shows them. */
  private enum ChargeKind(val key: String, val matches: EtmpChargeDetail => Boolean):
    case Tax                 extends ChargeKind("tax", _.isTaxCharge)
    case LateFilingPenalty   extends ChargeKind("lateFilingPenalty", _.isLateFilingPenalty)
    case LatePaymentPenalty  extends ChargeKind("latePaymentPenalty", _.isLatePaymentPenalty)
    case LatePaymentInterest extends ChargeKind("latePaymentInterest", _.isLatePaymentInterest)

  // The tax charge shows its due date; penalties and interest only say "due".
  private def chargeAmountText(kind: ChargeKind, charge: EtmpChargeDetail)(implicit messages: Messages): String = {
    val amount = CurrencyFormatter.currencyFormat(charge.chargeAmountPending)
    kind match {
      case ChargeKind.Tax if charge.isOverdue => messages("transferDetails.charges.wasDueBy", amount, formatDate(charge.chargeDueDate))
      case ChargeKind.Tax                     => messages("transferDetails.charges.dueBy", amount, formatDate(charge.chargeDueDate))
      case _                                  => messages("transferDetails.charges.due", amount)
    }
  }

  private def chargeRow(submissionId: String, transferNumber: Int, kind: ChargeKind, charge: EtmpChargeDetail)
                       (implicit messages: Messages): SummaryListRow = {
    val payUrl = payRoutes.PaymentController.startChargePayment(submissionId, transferNumber, charge.chargeReference).url
    SummaryListRowViewModel(
      key     = KeyViewModel(Text(messages(s"transferDetails.charges.${kind.key}"))).withCssClass(keyWidth),
      value   = ValueViewModel(Text(chargeAmountText(kind, charge))),
      actions = Seq(
        ActionItemViewModel(Text(messages("transferDetails.charges.pay")), payUrl)
          .withVisuallyHiddenText(messages(s"transferDetails.charges.pay.hidden.${kind.key}"))
      )
    )
  }

  private def chargesList(submissionId: String, transferNumber: Int, transfer: SttTransfer)
                         (implicit messages: Messages): Option[SummaryList] = {
    val unpaid = for {
      kind   <- ChargeKind.values.toSeq
      charge <- transfer.charges.filter(c => kind.matches(c) && !c.isPaid)
    } yield chargeRow(submissionId, transferNumber, kind, charge)

    Option.when(unpaid.nonEmpty)(SummaryList(rows = unpaid, classes = "stc-charges-list"))
  }

  private def referenceList(submission: SttSubmission, transfer: SttTransfer)(implicit messages: Messages): SummaryList = {
    // The UTRN is shown once there's no tax left to pay: either it's been paid, or none was due (e.g. a relief).
    // Penalties and interest don't affect this.
    val noTaxDue = transfer.taxToPay == 0
    val reference = if noTaxDue then transfer.utrn else messages("transferDetails.noData")
    val submissionUrl = dashboardRoutes.SubmissionDetailsController.onPageLoad(submission.submissionId).url
    val submissionLink =
      s"""<a class="govuk-link" href="$submissionUrl">${HtmlFormat.escape(submission.submissionId)}</a>"""

    SummaryList(rows = Seq(
      row("transferDetails.transferReference", reference),
      htmlRow("transferDetails.partOfSubmission", submissionLink),
      row("transferDetails.submissionDate", formatDate(submission.submissionDate))
    ))
  }

  private def address(lines: Seq[Option[String]]): String =
    lines.flatten.filter(_.trim.nonEmpty).map(HtmlFormat.escape(_).toString).mkString("<br>")

  // Only the main buyer and main seller are shown: the service doesn't support multiple buyers or sellers yet.
  private def partyList(main: Option[(String, String)])(implicit messages: Messages): SummaryList =
    SummaryList(rows = main.toSeq.flatMap { case (name, addressHtml) =>
      Seq(row("transferDetails.name", name), htmlRow("transferDetails.address", addressHtml))
    })

  private def buyerList(transaction: ViewTransferResponse)(implicit messages: Messages): SummaryList = {
    val main = transaction.mainBuyerDetails
    val addressHtml = address(Seq(Some(main.addr1), main.addr2, main.addr3, main.addr4, Some(main.postcode)))
    partyList(Some(main.buyerName -> addressHtml))
  }

  private def sellerList(transaction: ViewTransferResponse)(implicit messages: Messages): SummaryList =
    partyList(transaction.mainSellerDetails.map { s =>
      s.sellerName -> address(Seq(Some(s.addr1), s.addr2, s.addr3, s.addr4, Some(s.postcode)))
    })

  private def transferList(transaction: ViewTransferResponse, affinityKey: String)(implicit messages: Messages): SummaryList = {
    val details = transaction.transactionDetails
    val taxRate = transaction.mainBuyerDetails.taxRate match {
      case BuyerTaxRate.HalfPercent       => messages("taxRate.half")
      case BuyerTaxRate.OneAndHalfPercent => messages("taxRate.oneAndHalf")
    }
    val relief = details.reliefClaimedName.filter(_.trim.nonEmpty)

    val rows = Seq(
      Some(row(s"$affinityKey.connectedPersons.checkYourAnswersLabel", yesNo(details.isConnectedPartiesTransactions))),
      Some(row(s"$affinityKey.applyingForRelief.checkYourAnswersLabel", yesNo(relief.isDefined))),
      relief.map(row("whatReliefAreYouApplyingFor.checkYourAnswersLabel", _)),
      Some(row(s"$affinityKey.transferDetails.businessName", details.companyName)),
      details.companyRegistrationNumber.filter(_.trim.nonEmpty).map(row("transferDetails.crn", _)),
      Some(row(s"$affinityKey.chargingPoint.checkYourAnswersLabel", formatDate(details.originalChargingPoint))),
      Some(row("taxRate.checkYourAnswersLabel", taxRate)),
      // ETMP doesn't hold the purchasing-shares answer, so this shows the type of security as agreed with the team.
      Some(row(s"$affinityKey.purchasingShares.checkYourAnswersLabel", details.typeOfSecurity)),
      Some(row(s"$affinityKey.otherSecuritiesType.checkYourAnswersLabel", details.typeOfSecurity)),
      Some(row("transferDetails.numberOfSecurities", CommonHelpers.formatWithCommas(details.numberOfShares))),
      Some(row(s"$affinityKey.amountPaidForSecurities.checkYourAnswersLabel", CurrencyFormatter.currencyFormat(details.considerationActual))),
      details.marketValue.map(v => row(s"$affinityKey.totalMarketValue.checkYourAnswersLabel", CurrencyFormatter.currencyFormat(v)))
    ).flatten

    SummaryList(rows = rows)
  }

  // Buyer details are only shown to agents; individuals and organisations are the buyer themselves.
  private def stfSections(transaction: ViewTransferResponse, isAgent: Boolean, affinityKey: String)
                         (implicit messages: Messages): Seq[TransferDetailsSection] =
    Option.when(isAgent)(TransferDetailsSection(messages("transferDetails.buyer.heading"), buyerList(transaction))).toSeq ++ Seq(
      TransferDetailsSection(messages("transferDetails.seller.heading"), sellerList(transaction)),
      TransferDetailsSection(messages("transferDetails.transfer.heading"), transferList(transaction, affinityKey))
    )

  private def currency(amount: BigDecimal): String = CurrencyFormatter.currencyFormat(amount)

  // "Both" means purchased for cancellation and the shares also qualify as treasury shares (see UserAnswersTransforms).
  private def sh03ReasonText(reason: ReasonForPurchase)(implicit messages: Messages): String = reason match {
    case ReasonForPurchase.PurchasedToPlaceIntoTreasury => messages("reasonForPurchase.treasury")
    case _                                              => messages("reasonForPurchase.cancellation")
  }

  // The treasury shares question is only asked for shares purchased for cancellation.
  private def sh03TreasuryShares(reason: ReasonForPurchase): Option[Boolean] = reason match {
    case ReasonForPurchase.PurchasedForCancellation     => Some(false)
    case ReasonForPurchase.Both                         => Some(true)
    case ReasonForPurchase.PurchasedToPlaceIntoTreasury => None
  }

  // Message keys use the role name in camel case, e.g. PersonAuthorised -> personAuthorised.
  private def sh03RoleText(role: DeclarationRole)(implicit messages: Messages): String = {
    val name = role.toString
    messages(s"org.sh03.roleAtPurchasingCompany.${name.head.toLower}${name.tail}")
  }

  // Business SH03 sections, grouped as on the business SH03 check your answers page.
  private def sh03Sections(transaction: ViewTransferResponse)(implicit messages: Messages): Seq[TransferDetailsSection] = {
    val details = transaction.transactionDetails
    val relief = details.reliefClaimedName.filter(_.trim.nonEmpty)
    val reason = details.reasonForPurchase

    val yourDetails = Seq(
      Some(row("org.sh03.companyDetails.companyName.label", details.companyName)),
      details.companyRegistrationNumber.filter(_.trim.nonEmpty).map(row("org.sh03.companyDetails.crn.label", _)),
      transaction.mainBuyerDetails.isPLC.map(isPlc => row("org.sh03.companyDetails.isPlc.label", yesNo(isPlc)))
    ).flatten

    val transferDetails = Seq(
      reason.map(r => row("org.sh03.reasonForPurchase.checkYourAnswersLabel", sh03ReasonText(r))),
      reason.flatMap(sh03TreasuryShares).map(t => row("org.sh03.treasuryShares.checkYourAnswersLabel", yesNo(t))),
      Some(row("org.sh03.connectedPersons.checkYourAnswersLabel", yesNo(details.isConnectedPartiesTransactions))),
      Some(row("org.sh03.applyingForRelief.checkYourAnswersLabel", yesNo(relief.isDefined))),
      relief.map(row("org.sh03.whatReliefAreYouApplyingFor.checkYourAnswersLabel", _)),
      Some(row("org.sh03.detailsOfSharePurchase.numberOfShares", CommonHelpers.formatWithCommas(details.numberOfShares))),
      Some(row("org.sh03.detailsOfSharePurchase.typeOfShares", details.typeOfSecurity)),
      Some(row("transferDetails.sh03.amountPaid", currency(details.considerationActual))),
      details.marketValue.map(v => row("org.sh03.detailsOfSharePurchase.checkYourAnswersLabel.marketValue", currency(v))),
      details.maxPricePaid.map(v => row("org.sh03.maximumAmountPaid.checkYourAnswersLabel", currency(v))),
      details.minPricePaid.map(v => row("org.sh03.minimumAmountPaid.checkYourAnswersLabel", currency(v))),
      Some(row("org.sh03.chargingPoint.checkYourAnswersLabel", formatDate(details.originalChargingPoint)))
    ).flatten

    val declaration = transaction.declaration
    val declarationRows = Seq(
      declaration.role1.map(r => row("org.sh03.roleAtPurchasingCompany.checkYourAnswersLabel", sh03RoleText(r))),
      declaration.role2.filter(_.trim.nonEmpty).map(row("org.sh03.roleAtPurchasingCompany.uksOrgan.label", _))
    ).flatten

    Seq(
      TransferDetailsSection(messages("transferDetails.yourDetails.heading"), SummaryList(rows = yourDetails)),
      TransferDetailsSection(messages("transferDetails.transfer.heading"), SummaryList(rows = transferDetails)),
      TransferDetailsSection(messages("transferDetails.declaration.heading"), SummaryList(rows = declarationRows))
    )
  }

  /**
   * @param transferNumber the transfer's number in the submission (1-based, in UTRN order; see SttSubmission.transferNumber).
   * @param affinityKey the message key prefix for the transfer details rows ("individual", "org" or "agent").
   */
  def build(
    submission: SttSubmission,
    transferNumber: Int,
    transfer: SttTransfer,
    transaction: ViewTransferResponse,
    affinityKey: String
  )(implicit messages: Messages): TransferDetailsViewModel = {
    val submissionId = submission.submissionId
    val balance = transfer.totalToPay
    val anyPaid = transfer.charges.exists(_.isPaid)
    val isAgent = affinityKey == "agent"

    TransferDetailsViewModel(
      submissionId      = submissionId,
      transferNumber    = transferNumber,
      balanceToPay      = CurrencyFormatter.currencyFormat(balance),
      payFullBalanceUrl = Option.when(balance > 0)(payRoutes.PaymentController.startTransferPayment(submissionId, transferNumber).url),
      chargesList       = chargesList(submissionId, transferNumber, transfer),
      paidChargesUrl    = Option.when(anyPaid)(dashboardRoutes.PaidChargesController.onPageLoad(submissionId, transferNumber).url),
      referenceList     = referenceList(submission, transfer),
      sections          = submission.submissionType match {
        case TransferType.SH03 => sh03Sections(transaction)
        case _                 => stfSections(transaction, isAgent, affinityKey)
      }
    )
  }
}
