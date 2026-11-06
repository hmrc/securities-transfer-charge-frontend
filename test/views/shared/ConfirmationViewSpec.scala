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

package views.shared

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard.routes as dashboardRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes as sharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.{SH03, STF}
import uk.gov.hmrc.securitiestransferchargefrontend.models.{ConfirmationViewModel, JourneyType}
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.shared.ConfirmationView
import views.ViewBaseSpec

import scala.jdk.CollectionConverters.*

class ConfirmationViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder().build()

  private val viewInstance = app.injector.instanceOf[ConfirmationView]

  private val testReference = "AgentRef-1"


  private def viewModel(
                         journeyType: JourneyType = STF,
                         isBulk: Boolean = false,
                         isAgent: Boolean = false,
                         reference: Option[String] = None
                       ): ConfirmationViewModel =
    ConfirmationViewModel(
      submissionId = submissionId.value,
      paymentDueBy = "25 January 2026",
      reference    = reference,
      taxDue       = "£500",
      isAgent      = isAgent,
      journeyType  = journeyType,
      isBulk       = isBulk
    )

  def view(vm: ConfirmationViewModel): Document = Jsoup.parse(viewInstance(vm)(fakeRequest, messages).body)

  object ExpectedContent {

    val title: String = messages("confirmation.title")
    val bannerTitle: String = messages("confirmation.banner.title")
    val bannerContentP1: String = messages("confirmation.banner.p1", viewModel().paymentDueBy)
    val bannerContentP2: String = messages("confirmation.banner.p2")

    val nextStepsHeading: String = messages("confirmation.heading.nextSteps")
    val transferReferenceHeading: String = messages("confirmation.heading.transferReference")
    val deadlineHeading: String = messages("confirmation.heading.deadline")
    val deadlineP1: String = messages("confirmation.heading.deadline.p1")

    val submissionLinkText: String = messages("confirmation.submission.link")

    val tableSubmissionIdHeading: String = messages("confirmation.table.heading.submissionId")
    val tableYourReferenceHeading: String = messages("confirmation.table.heading.reference")
    val tableTaxDueHeading: String = messages("confirmation.table.heading.taxDue")
    val notProvided: String = messages("site.notProvided")

    val payButton: String = messages("confirmation.button.pay")
    val viewSubmissionButton: String = messages("confirmation.button.viewSubmission")
    val returnToDashboard: String = messages("return-to-dashboard.link")

    def referencesText(journeyType: JourneyType, isBulk: Boolean): String = {
      val ending = journeyType match {
        case STF => messages("confirmation.heading.transferReference.p2")
        case SH03 => messages("confirmation.heading.shareBuyBack.p2")
      }

      if (isBulk) {
        s"${messages("bulkConfirmation.transferReference.p1")} ${messages("bulkConfirmation.transferReference.p2")} $ending"
      } else {
        val start = journeyType match {
          case STF => messages("confirmation.heading.transferReference.p1", submissionLinkText)
          case SH03 => messages("confirmation.heading.shareBuyBack.p1", submissionLinkText)
        }
        s"$start $ending"
      }
    }
  }

  private def referencesParagraph(doc: Document): Element = doc.select("p.govuk-body").get(0)

  private def referenceRow(doc: Document): Option[Element] =
    doc.select(".govuk-table__row").asScala.find(_.select(".govuk-table__header").text() == ExpectedContent.tableYourReferenceHeading)

  private case class Scenario(name: String, journeyType: JourneyType, isBulk: Boolean, isAgent: Boolean)

  private val scenarios = Seq(
    Scenario("STF individual single", STF, isBulk = false, isAgent = false),
    Scenario("STF organisation single", STF, isBulk = false, isAgent = false),
    Scenario("STF agent single", STF, isBulk = false, isAgent = true),
    Scenario("STF individual bulk", STF, isBulk = true, isAgent = false),
    Scenario("STF organisation bulk", STF, isBulk = true, isAgent = false),
    Scenario("STF agent bulk", STF, isBulk = true, isAgent = true),
    Scenario("SH03 organisation single", SH03, isBulk = false, isAgent = false),
    Scenario("SH03 agent single", SH03, isBulk = false, isAgent = true),
    Scenario("SH03 organisation bulk", SH03, isBulk = true, isAgent = false),
    Scenario("SH03 agent bulk", SH03, isBulk = true, isAgent = true)
  )

  "The ConfirmationView" - {

    "for all submissions" - {

      "should have the correct title" in {
        view(viewModel()).title must include(ExpectedContent.title)
      }

      "should have the correct heading in the notification banner" in {
        view(viewModel()).select(".govuk-notification-banner__title").text() mustBe ExpectedContent.bannerTitle
      }

      "should have the correct notification banner content" in {
        val bannerContent = view(viewModel()).select(".govuk-notification-banner__content").html()
        bannerContent must include(ExpectedContent.bannerContentP1)
        bannerContent must include(ExpectedContent.bannerContentP2)
      }

      "should have the next steps heading" in {
        view(viewModel()).select("h1").eachText().asScala must contain(ExpectedContent.nextStepsHeading)
      }

      "should have the transfer reference and deadline headings" in {
        val headings = view(viewModel()).select("h2").eachText().asScala
        headings must contain(ExpectedContent.transferReferenceHeading)
        headings must contain(ExpectedContent.deadlineHeading)
      }

      "should have the deadline content" in {
        view(viewModel()).select("p.govuk-body").eachText().asScala must contain(ExpectedContent.deadlineP1)
      }

      "should have a table with submission ID and tax due rows" in {
        val rowHeadings = view(viewModel()).select(".govuk-table__header").asScala.map(_.text())
        rowHeadings must contain(ExpectedContent.tableSubmissionIdHeading)
        rowHeadings must contain(ExpectedContent.tableTaxDueHeading)
      }

      "should have a return to dashboard link" in {
        val link = view(viewModel()).select(".govuk-button-group a.govuk-link")
        link.text() mustBe ExpectedContent.returnToDashboard
        link.attr("href") mustBe dashboardRoutes.DashboardController.onPageLoad().url
      }
    }

    scenarios.foreach { scenario =>

      scenario.name - {

        val vm = viewModel(journeyType = scenario.journeyType, isBulk = scenario.isBulk, isAgent = scenario.isAgent)

        "should show the correct transfer reference text" in {
          referencesParagraph(view(vm)).text() mustBe ExpectedContent.referencesText(scenario.journeyType, scenario.isBulk)
        }

        if (scenario.isBulk) {

          "should not show a link in the transfer reference text" in {
            referencesParagraph(view(vm)).select("a").size() mustBe 0
          }

          "should show a 'View your submission' button that goes to the submission details page" in {
            val doc = view(vm)
            doc.select(".govuk-button").text() mustBe ExpectedContent.viewSubmissionButton
            doc.select("form").attr("action") mustBe
              dashboardRoutes.SubmissionDetailsController.onPageLoad(submissionId.value).url
          }

        } else {

          "should show a link to the dashboard that opens in a new tab" in {
            val link = referencesParagraph(view(vm)).select("a.govuk-link")
            link.text() mustBe ExpectedContent.submissionLinkText
            link.attr("href") mustBe dashboardRoutes.DashboardController.onPageLoad().url
            link.attr("target") mustBe "_blank"
            link.attr("rel") mustBe "noreferrer noopener"
          }

          "should show a 'Pay for this submission' button that goes to the pay page" in {
            val doc = view(vm)
            doc.select(".govuk-button").text() mustBe ExpectedContent.payButton
            doc.select("form").attr("action") mustBe sharedRoutes.PaySubmissionController.onPageLoad().url
          }
        }

        if (scenario.isAgent) {

          "should show the 'Your reference' row with the reference when one was given" in {
            val withRefVm = viewModel(journeyType = scenario.journeyType, isBulk = scenario.isBulk, isAgent = true, reference = Some(testReference))
            referenceRow(view(withRefVm)).map(_.select(".govuk-table__cell").text()) mustBe Some(testReference)
          }

          "should show the 'Your reference' row with 'Not provided' when no reference was given" in {
            referenceRow(view(vm)).map(_.select(".govuk-table__cell").text()) mustBe Some(ExpectedContent.notProvided)
          }

          "should show the 'Your reference' row with 'Not provided' when the reference is empty" in {
            val emptyRefVm = viewModel(journeyType = scenario.journeyType, isBulk = scenario.isBulk, isAgent = true, reference = Some(""))
            referenceRow(view(emptyRefVm)).map(_.select(".govuk-table__cell").text()) mustBe Some(ExpectedContent.notProvided)
          }

        } else {

          "should not show the 'Your reference' row, even if a reference is present" in {
            val withRefVm = viewModel(journeyType = scenario.journeyType, isBulk = scenario.isBulk, isAgent = false, reference = Some(testReference))
            referenceRow(view(withRefVm)) mustBe None
          }
        }
      }
    }
  }
}