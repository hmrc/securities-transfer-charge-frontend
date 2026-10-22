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
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SubmissionStatus.{Overdue, ReadyToPay}
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.SubmissionDetailsSummary
import uk.gov.hmrc.securitiestransferchargefrontend.services.stf.shared.FormattingService
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard.*

class SubmissionDetailsViewModelSpec extends SpecBase with GuiceOneAppPerSuite {

  implicit private val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(fakeRequest)

  private val formatter = app.injector.instanceOf[FormattingService]

  private val readyToPayTransfer = Fixtures.readyToPaySttTransferTaxOnly

  private val overdueTransfer =
    Fixtures.readyToPaySttTransferTaxOnly.copy(
      utrn = "123456789",
      charges = Seq(
        Fixtures.buildTaxCharge("123456789", true, false)
      )
    )

  private def build(transfers: Seq[SttTransfer] = Seq(readyToPayTransfer), isAgent: Boolean = false, isSh03: Boolean = false): SubmissionDetailsViewModel =
    SubmissionDetailsViewModel.build(
      Fixtures.singleSubmissionReadyToPay.copy(
        transfers = transfers,
        declareeName = if (isSh03) Some("James White") else None
      ),
      formatter,
      isAgent
    )

  private def tab(model: SubmissionDetailsViewModel, id: String): TransferTab =
    model.tabs.find(_.id == messages(s"submissionDetails.$id.tabId")).get

  "SubmissionDetailsViewModel.build" - {

    "return the submission ID" in {
      val model = build()

      model.submissionId mustBe Fixtures.testSubmissionId.toString
    }

    "build the submission summary" in {
      val submission = Fixtures.singleSubmissionReadyToPay
      val model = build()

      model.summary mustBe SubmissionDetailsSummary.getSummary(submission, formatter, isAgent = false)
    }

    "build all three tabs in the correct order" in {
      val model = build()

      model.tabs.map(_.id) mustBe Seq(
        messages("submissionDetails.allTransfers.tabId"),
        messages("submissionDetails.readyToPay.tabId"),
        messages("submissionDetails.overdue.tabId")
      )
    }

    "populate the tabs with the correct headings and empty text" in {
      val model = build()

      Seq("allTransfers", "readyToPay", "overdue").foreach { id =>
        val result = tab(model, id)

        result.heading mustBe messages(s"submissionDetails.$id.panel.heading")

        result.emptyText mustBe messages(s"submissionDetails.$id.empty")
      }
    }

    "when transfers have different statuses" - {

      "include all transfers in the All Transfers tab" in {
        val model = build(Seq(readyToPayTransfer, overdueTransfer))

        val result = tab(model, "allTransfers")

        result.table.get.rows.size mustBe 2

        result.table.get.rows.map(_.head.content) mustBe Seq(Text(readyToPayTransfer.utrn), Text(overdueTransfer.utrn))
      }

      "include only ready to pay transfers in the Ready to Pay tab" in {
        val model = build(Seq(readyToPayTransfer, overdueTransfer))

        val result = tab(model, "readyToPay")

        result.table.get.rows.size mustBe 1
        result.table.get.rows.head.head.content mustBe Text(readyToPayTransfer.utrn)
      }

      "include only overdue transfers in the Overdue tab" in {
        val model = build(Seq(readyToPayTransfer, overdueTransfer))

        val result = tab(model, "overdue")

        result.table.get.rows.size mustBe 1
        result.table.get.rows.head.head.content mustBe Text(overdueTransfer.utrn)
      }

      "display the correct Ready to Pay tab label" in {
        val model = build(Seq(readyToPayTransfer, overdueTransfer)
        )

        tab(model, "readyToPay").label mustBe
          StatusTag.labelWithTag(
            messages("submissionDetails.readyToPay.panel.label"),
            1,
            ReadyToPay
          )
      }

      "display the correct Overdue tab label" in {
        val model = build(
          Seq(readyToPayTransfer, overdueTransfer)
        )

        tab(model, "overdue").label mustBe
          StatusTag.labelWithTag(
            messages("submissionDetails.overdue.panel.label"),
            1,
            Overdue
          )
      }
    }

    "when there are no overdue transfers" - {

      "display the All Transfers table" in {
        val model = build()

        tab(model, "allTransfers").table.get.rows.size mustBe 1
      }

      "display the Ready to Pay table" in {
        val model = build()

        tab(model, "readyToPay").table.get.rows.size mustBe 1
      }

      "not display an Overdue table" in {
        val model = build()

        tab(model, "overdue").table mustBe None
      }

      "display zero in the Overdue tab label" in {
        val model = build()

        tab(model, "overdue").label mustBe
          StatusTag.labelWithTag(
            messages("submissionDetails.overdue.panel.label"),
            0,
            Overdue
          )
      }
    }

    "when there are no ready to pay transfers" - {

      "display the All Transfers table" in {
        val model = build(Seq(overdueTransfer))

        tab(model, "allTransfers").table.get.rows.size mustBe 1
      }

      "not display a Ready to Pay table" in {
        val model = build(Seq(overdueTransfer))

        tab(model, "readyToPay").table mustBe None
      }

      "display the Overdue table" in {
        val model = build(Seq(overdueTransfer))

        tab(model, "overdue").table.get.rows.size mustBe 1
      }

      "display zero in the Ready to Pay tab label" in {
        val model = build(Seq(overdueTransfer))

        tab(model, "readyToPay").label mustBe
          StatusTag.labelWithTag(
            messages("submissionDetails.readyToPay.panel.label"),
            0,
            ReadyToPay
          )
      }
    }

    "limit the number of displayed transfers to 10" in {
      val transfers = (1 to 15).map { index =>
        readyToPayTransfer.copy(
          utrn = s"UTRN$index"
        )
      }

      val model = build(transfers)

      tab(model, "allTransfers").table.get.rows.size mustBe 10
      tab(model, "readyToPay").table.get.rows.size mustBe 10
      tab(model, "overdue").table mustBe None

      tab(model, "readyToPay").label mustBe
        StatusTag.labelWithTag(
          messages("submissionDetails.readyToPay.panel.label"),
          15,
          ReadyToPay
        )
    }

    "when the submission is an SH03" - {

      val sh03Headings = Seq(
        Text(messages("submissionDetails.transfers.table.reference.heading")),
        Text(messages("submissionDetails.transfers.table.shares.heading")),
        Text(messages("submissionDetails.transfers.table.for.heading")),
        Text(messages("submissionDetails.transfers.table.paymentDueBy.heading")),
        Text(messages("submissionDetails.transfers.table.status.heading")),
        Text(messages("submissionDetails.transfers.table.action.heading"))
      )

      "display the correct table headings" in {
        val table = tab(build(isSh03 = true), "allTransfers").table.get

        table.head.get.map(_.content) mustBe sh03Headings
      }

      "display the number of shares and the reason for purchase" in {
        val row = tab(build(isSh03 = true), "allTransfers").table.get.rows.head

        row(1).content mustBe Text(readyToPayTransfer.numberOfShares.toString)
        row(2).content mustBe Text(messages("submissionDetails.reasonForPurchase.cancellation")) // TODO: update once etmp api spec is updated
      }

      "display the same headings when the user is an agent" in {
        val table = tab(build(isAgent = true, isSh03 = true), "allTransfers").table.get

        table.head.get.map(_.content) mustBe sh03Headings
      }

      "display the same columns when the user is an agent" in {
        val row = tab(build(isAgent = true, isSh03 = true), "allTransfers").table.get.rows.head

        row(1).content mustBe Text(readyToPayTransfer.numberOfShares.toString)
        row(2).content mustBe Text(messages("submissionDetails.reasonForPurchase.cancellation"))  // TODO: update once etmp api spec is updated
      }
    }

    "when the user is an agent" - {

      "display the correct table headings" in {
        val table = tab(build(isAgent = true), "allTransfers").table.get

        table.head.get.map(_.content) mustBe Seq(
          Text(messages("submissionDetails.transfers.table.reference.heading")),
          Text(messages("submissionDetails.transfers.table.buyer.heading")),
          Text(messages("submissionDetails.transfers.table.seller.heading")),
          Text(messages("submissionDetails.transfers.table.paymentDueBy.heading")),
          Text(messages("submissionDetails.transfers.table.status.heading")),
          Text(messages("submissionDetails.transfers.table.action.heading"))
        )
      }

      "display the buyer and seller names" in {
        val table = tab(build(isAgent = true), "allTransfers").table.get

        val row = table.rows.head

        row(1).content mustBe Text(readyToPayTransfer.buyerNames)
        row(2).content mustBe Text(readyToPayTransfer.sellerNames.get)
      }

      "build the agent submission summary" in {
        val model = build(isAgent = true)

        model.summary mustBe SubmissionDetailsSummary.getSummary(
          Fixtures.singleSubmissionReadyToPay,
          formatter,
          isAgent = true
        )
      }

      "build the agent submission summary with the Your Reference field and the provided client reference" in {
        val model = SubmissionDetailsViewModel.build(
          Fixtures.singleSubmissionReadyToPay.copy(
            clientReference = Some("Reference/1234")
          ),
          formatter,
          isAgent = true
        )

        val rows = model.summary.summaryList.rows

        rows.size mustBe 5
        rows.head.key.content mustBe Text(messages("submissionDetails.submission.agent.reference"))
        rows.head.value.content mustBe Text("Reference/1234")
      }

      "build the agent submission summary with the Your Reference field when nonclient reference is provided" in {
        val model = SubmissionDetailsViewModel.build(
          Fixtures.singleSubmissionReadyToPay.copy(
            clientReference = None
          ),
          formatter,
          isAgent = true
        )

        val rows = model.summary.summaryList.rows

        rows.size mustBe 5
        rows.head.key.content mustBe Text(messages("submissionDetails.submission.agent.reference"))
        rows.head.value.content mustBe Text(messages("submissionDetails.submission.agent.reference.notProvided"))
      }
    }

    "when the user is not an agent" - {

      "display the correct table headings" in {
        val table = tab(build(), "allTransfers").table.get

        table.head.get.map(_.content) mustBe Seq(
          Text(messages("submissionDetails.transfers.table.reference.heading")),
          Text(messages("submissionDetails.transfers.table.seller.heading")),
          Text(messages("submissionDetails.transfers.table.securitiesBoughtIn.heading")),
          Text(messages("submissionDetails.transfers.table.paymentDueBy.heading")),
          Text(messages("submissionDetails.transfers.table.status.heading")),
          Text(messages("submissionDetails.transfers.table.action.heading"))
        )
      }

      "display the seller and company name" in {
        val table = tab(build(), "allTransfers").table.get

        val row = table.rows.head

        row(1).content mustBe Text(readyToPayTransfer.sellerNames.get)

        row(2).content mustBe Text(readyToPayTransfer.companyName)
      }

    }

    "format the payment due by date" in {
      val model = build()
      val table = tab(model, "allTransfers").table.get

      table.rows.head(3).content mustBe Text(
        formatter.formatDate(readyToPayTransfer.paymentDueByDate)
      )
    }

    "display the correct status tag" in {
      val model = build()
      val table = tab(model, "allTransfers").table.get

      table.rows.head(4).content mustBe
        StatusTag.tagHtml(ReadyToPay)
    }

    "display the view link with visually hidden text" in {
      val model = build()
      val table = tab(model, "allTransfers").table.get

      val content = table.rows.head(5).content.asHtml.toString

      content must include("govuk-link")
      content must include("govuk-visually-hidden")
      content must include(
        messages("submissionDetails.transfers.action.view")
      )
      content must include(
        messages(
          "submissionDetails.transfers.action.view.hidden",
          readyToPayTransfer.utrn
        )
      )
    }
  }
}
