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

package views.dashboard

import base.Fixtures
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.pay.routes as paymentRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SttTransfer
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard.TransferDetailsViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.dashboard.TransferDetailsView
import views.ViewBaseSpec

import scala.jdk.CollectionConverters.*

class TransferDetailsViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()

  private val viewInstance = app.injector.instanceOf[TransferDetailsView]
  private val utrn = Fixtures.testUtrn
  private val submission = Fixtures.singleSubmissionReadyToPay
  private val subId = submission.submissionId

  private val unpaidTransfer: SttTransfer = Fixtures.readyToPaySttTransferTaxOnly.copy(
    charges = Seq(Fixtures.buildTaxCharge(utrn, false, false), Fixtures.buildLateFilingCharge(utrn, false, false))
  )
  private val partPaidTransfer: SttTransfer = Fixtures.readyToPaySttTransferTaxOnly.copy(
    charges = Seq(Fixtures.buildTaxCharge(utrn, false, true), Fixtures.buildLateFilingCharge(utrn, false, false))
  )
  private val paidTransfer: SttTransfer = Fixtures.readyToPaySttTransferTaxOnly.copy(
    charges = Seq(Fixtures.buildTaxCharge(utrn, false, true))
  )

  private def view(
    transfer: SttTransfer,
    transferNumber: Int = 1,
    affinityKey: String = "agent",
    declareeName: Option[String] = None
  ): Document = Jsoup.parse(
    viewInstance(
      TransferDetailsViewModel.build(
        submission.copy(transfers = Seq(transfer), declareeName = declareeName), transferNumber, transfer, Fixtures.viewTransferResponse, affinityKey
      )
    )(fakeRequest, messages).body
  )

  "TransferDetailsView" - {

    "have the correct page title and heading" in {
      val doc = view(unpaidTransfer, transferNumber = 3)
      doc.title mustBe s"${messages("transferDetails.title", 3)} - ${messages("reportAndPay.title")} - ${messages("site.govuk")}"
      doc.select("h1").text mustBe messages("transferDetails.heading", 3)
    }

    "have breadcrumbs back to the dashboard, submissions and the submission" in {
      val links = view(unpaidTransfer).select(".govuk-breadcrumbs__link").asScala.toSeq
      links.map(_.text) mustBe Seq(
        messages("breadcrumbs.dashboard"),
        messages("breadcrumbs.submissions"),
        messages("transferDetails.breadcrumb.submission", subId)
      )
      links.map(_.attr("href")) mustBe Seq(
        routes.DashboardController.onPageLoad().url,
        routes.SubmissionsController.onPageLoad().url,
        routes.SubmissionDetailsController.onPageLoad(subId).url
      )
    }

    "show the balance to pay in the grey card" in {
      view(unpaidTransfer).select(".tax-card h2").text mustBe messages("transferDetails.balance.heading", "£246.90")
    }

    "when there are unpaid charges" - {
      "show the pay full balance button linking to OPS for the transfer" in {
        val button = view(unpaidTransfer, transferNumber = 2).select(".tax-card a.govuk-button")
        button.text mustBe messages("transferDetails.payFullBalance")
        button.attr("href") mustBe paymentRoutes.PaymentController.startTransferPayment(subId, 2).url
      }

      "show each charge with a pay link and visually hidden text" in {
        val doc = view(unpaidTransfer)
        doc.select(".tax-card h3").text mustBe messages("transferDetails.charges.heading")
        val payLinks = doc.select(".tax-card .govuk-summary-list__actions a").asScala.toSeq
        payLinks.map(_.select(".govuk-visually-hidden").text) mustBe Seq(
          messages("transferDetails.charges.pay.hidden.tax"),
          messages("transferDetails.charges.pay.hidden.lateFilingPenalty")
        )
      }

      "not show the view paid charges link when nothing has been paid" in {
        view(unpaidTransfer).select("#view-paid-charges").size mustBe 0
      }
    }

    "when some charges are paid" - {
      "show the view paid charges link" in {
        val link = view(partPaidTransfer, transferNumber = 2).select("#view-paid-charges")
        link.text mustBe messages("transferDetails.viewPaidCharges")
        link.attr("href") mustBe routes.PaidChargesController.onPageLoad(subId, 2).url
      }

      "only list the unpaid charges" in {
        view(partPaidTransfer).select(".tax-card .govuk-summary-list__key").asScala.map(_.text) mustBe
          Seq(messages("transferDetails.charges.lateFilingPenalty"))
      }
    }

    "when every charge is paid" - {
      "show a £0 balance with no button or charges list" in {
        val doc = view(paidTransfer)
        doc.select(".tax-card h2").text mustBe messages("transferDetails.balance.heading", "£0")
        doc.select(".tax-card a.govuk-button").size mustBe 0
        doc.select(".tax-card h3").size mustBe 0
        doc.select(".tax-card .govuk-summary-list").size mustBe 0
      }
    }

    "show the buyer, seller and transfer details sections for an agent" in {
      view(unpaidTransfer, affinityKey = "agent").select("h2.govuk-heading-m").asScala.map(_.text).drop(1) mustBe Seq(
        messages("transferDetails.buyer.heading"),
        messages("transferDetails.seller.heading"),
        messages("transferDetails.transfer.heading")
      )
    }

    Seq("an organisation" -> "org", "an individual" -> "individual").foreach { case (userType, affinityKey) =>
      s"not show the buyer section for $userType" in {
        view(unpaidTransfer, affinityKey = affinityKey).select("h2.govuk-heading-m").asScala.map(_.text).drop(1) mustBe Seq(
          messages("transferDetails.seller.heading"),
          messages("transferDetails.transfer.heading")
        )
      }
    }

    "show the your details, transfer details and declaration sections for a business SH03" in {
      val doc = view(unpaidTransfer, affinityKey = "org", declareeName = Some("James White"))
      doc.select("h2.govuk-heading-m").asScala.map(_.text).drop(1) mustBe Seq(
        messages("transferDetails.yourDetails.heading"),
        messages("transferDetails.transfer.heading"),
        messages("transferDetails.declaration.heading")
      )
      doc.select(".govuk-grid-column-three-quarters .govuk-summary-list").size mustBe 4
    }
  }
}
