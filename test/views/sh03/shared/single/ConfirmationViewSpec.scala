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

package views.sh03.shared.single

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.models.ConfirmationViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.shared.single.ConfirmationView
import views.ViewBaseSpec

import java.time.LocalDate
import scala.jdk.CollectionConverters.*

class ConfirmationViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder(affinityGroup = orgAffinity).build()

  private val viewInstance = app.injector.instanceOf[ConfirmationView]


  private val viewModel =
    ConfirmationViewModel(
      submissionId = submissionId,
      paymentDueBy = LocalDate.of(2026,1,25),
      reference = Some("ref"),
      taxDue = BigDecimal(500),
      isAgent = false
    )

  private val agentViewModel =
    ConfirmationViewModel(
      submissionId = submissionId,
      paymentDueBy = LocalDate.of(2026,1,25),
      reference = Some("ref"),
      taxDue = BigDecimal(500),
      isAgent = true
    )

  def view(vm: ConfirmationViewModel): Document = Jsoup.parse(viewInstance(vm)(fakeRequest, messages).body)

  object ExpectedContent {

    val title: String = messages("confirmation.title")
    val bannerTitle: String = messages("confirmation.banner.title")
    val bannerContentP1: String = messages("confirmation.banner.p1", viewModel.paymentDueBy)
    val bannerContentP2: String = messages("confirmation.banner.p2")

    val nextStepsHeading: String = messages("confirmation.heading.nextSteps")

    val transferReferenceHeading: String = messages("confirmation.heading.transferReference")
    val transferReferenceP1: String = messages("confirmation.heading.transferReference.p1")
    val transferReferenceP2: String = messages("confirmation.heading.shareBuyBack.p2")
    val transferReferenceLink: String = messages("confirmation.heading.transferReference.link")

    val deadlineHeading: String = messages("confirmation.heading.deadline")
    val deadlineP1: String = messages("confirmation.heading.deadline.p1")

    val tableSubmissionIdHeading: String = messages("confirmation.table.heading.submissionId")
    val tableYourReferenceHeading: String = messages("confirmation.table.heading.reference")
    val tableTaxDueHeading: String = messages("confirmation.table.heading.taxDue")

    val buttonText: String = messages("confirmation.button.pay")

    val returnToDashboard: String = messages("return-to-dashboard.link")
  }

  "The ConfirmationView" - {

    "should have the correct title" in {
      view(viewModel).title must include(ExpectedContent.title)
    }

    "should have the correct heading in the notification banner" in {
      view(viewModel).select(".govuk-notification-banner__title").text() mustBe ExpectedContent.bannerTitle
    }

    "should have the correct notification banner content" in {
      val bannerContent = view(viewModel).select(".govuk-notification-banner__content").html()

      bannerContent must include(ExpectedContent.bannerContentP1)
      bannerContent must include(ExpectedContent.bannerContentP2)
    }

    "should have the next steps heading" in {
      view(viewModel).select("h1").eachText().asScala must contain(ExpectedContent.nextStepsHeading)
    }

    "should have the next steps and deadline headings" in {
      val headings = view(viewModel).select("h2").eachText().asScala
      headings must contain(ExpectedContent.transferReferenceHeading)
      headings must contain(ExpectedContent.deadlineHeading)
    }

    "should display the transfer references content" in {
      val transferRefsContent = view(viewModel).select(".govuk-body").get(0).text()
      transferRefsContent must include(ExpectedContent.transferReferenceP1)
      transferRefsContent must include(ExpectedContent.transferReferenceP2)
    }

    "should have a table with submissionId and tax due headings" in {
      val rowHeadings = view(viewModel).select(".govuk-table__header").asScala.map(_.text())
      rowHeadings must contain(ExpectedContent.tableSubmissionIdHeading)
      rowHeadings must contain(ExpectedContent.tableTaxDueHeading)
    }

    "should have a table with your reference heading for agent affinity" in {
      val rowHeadings = view(agentViewModel).select(".govuk-table__header").asScala.map(_.text())
      rowHeadings must contain(ExpectedContent.tableYourReferenceHeading)
    }

    "should have a pay this submission button" in {
      view(viewModel).select(".govuk-button").text() mustBe ExpectedContent.buttonText
    }

    "should have a return to dashboard link" in {
      view(viewModel).select(".govuk-button-group a.govuk-link").text() mustBe ExpectedContent.returnToDashboard
    }
  }
}