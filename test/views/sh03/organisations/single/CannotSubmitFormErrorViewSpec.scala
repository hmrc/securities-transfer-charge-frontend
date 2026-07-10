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

package views.sh03.organisations.single

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.single.routes as sh03OrgSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.organisations.single.CannotSubmitFormErrorView
import views.ViewBaseSpec

class CannotSubmitFormErrorViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder(affinityGroup = orgAffinity).build()

  private val viewInstance         = app.injector.instanceOf[CannotSubmitFormErrorView]

  def view(): Document = Jsoup.parse(
    viewInstance()(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title: String = messages("org.sh03.cannotSubmitFormError.title")
    val heading: String = messages("org.sh03.cannotSubmitFormError.heading")

    val para1Value: String = messages("org.sh03.cannotSubmitFormError.p1")
    val para2Value: String = messages("org.sh03.cannotSubmitFormError.p2")

    val item1: String = messages("org.sh03.cannotSubmitFormError.item1")
    val item2: String = messages("org.sh03.cannotSubmitFormError.item2")
    val item3: String = messages("org.sh03.cannotSubmitFormError.item3")
    val item4: String = messages("org.sh03.cannotSubmitFormError.item4")
    val item5: String = messages("org.sh03.cannotSubmitFormError.item5")
    val item6: String = messages("org.sh03.cannotSubmitFormError.item6")
    val item7: String = messages("org.sh03.cannotSubmitFormError.item7")
    val item8: String = messages("org.sh03.cannotSubmitFormError.item8")

    val para3Value: String = messages("org.sh03.cannotSubmitFormError.p3")

    val saveAndReturnLink: String = messages("save-and-return-to-dashboard.link")

    val backLinkUrl: String = sh03OrgSingleRoutes.RoleAtPurchasingCompanyController.onPageLoad(NormalMode).url
  }

  "The CannotSubmitFormErrorView" - {
    "the user is an Organisation" - {
      val cannotSubmitFormErrorView = view()

      "have the correct title" in {
        cannotSubmitFormErrorView.title must include(ExpectedContent.title)
      }

      "have the correct heading" in {
        cannotSubmitFormErrorView.select("h1").text() mustBe ExpectedContent.heading
      }

      "display the correct of first paragraph content" in {
        cannotSubmitFormErrorView.para(1) mustBe Some(ExpectedContent.para1Value)
      }

      "display the correct second paragraph" in {
        cannotSubmitFormErrorView.para(2) mustBe Some(ExpectedContent.para2Value)
      }

      "display a bulleted list with 8 items" in {
        cannotSubmitFormErrorView.select("ul.govuk-list.govuk-list--bullet li").size() mustBe 8
      }

      "display the first item correctly" in {
        cannotSubmitFormErrorView.select("ul.govuk-list.govuk-list--bullet li").get(0).text() mustBe ExpectedContent.item1
      }

      "display the second item correctly" in {
        cannotSubmitFormErrorView.select("ul.govuk-list.govuk-list--bullet li").get(1).text() mustBe ExpectedContent.item2
      }

      "display the third item correctly" in {
        cannotSubmitFormErrorView.select("ul.govuk-list.govuk-list--bullet li").get(2).text() mustBe ExpectedContent.item3
      }

      "display the fourth item correctly" in {
        cannotSubmitFormErrorView.select("ul.govuk-list.govuk-list--bullet li").get(3).text() mustBe ExpectedContent.item4
      }

      "display the fifth item correctly" in {
        cannotSubmitFormErrorView.select("ul.govuk-list.govuk-list--bullet li").get(4).text() mustBe ExpectedContent.item5
      }

      "display the sixth item correctly" in {
        cannotSubmitFormErrorView.select("ul.govuk-list.govuk-list--bullet li").get(5).text() mustBe ExpectedContent.item6
      }

      "display the seventh item correctly" in {
        cannotSubmitFormErrorView.select("ul.govuk-list.govuk-list--bullet li").get(6).text() mustBe ExpectedContent.item7
      }

      "display the eighth item correctly" in {
        cannotSubmitFormErrorView.select("ul.govuk-list.govuk-list--bullet li").get(7).text() mustBe ExpectedContent.item8
      }

      "display the correct third paragraph" in {
        cannotSubmitFormErrorView.para(3) mustBe Some(ExpectedContent.para3Value)
      }

      "back link url to the role purchasing company page" in {
        cannotSubmitFormErrorView.select(".govuk-back-link").attr("href") mustBe ExpectedContent.backLinkUrl
      }

      "have a save and return link back to the submission dashboard page" in {
        val saveAndReturnLink = cannotSubmitFormErrorView.select(".govuk-body a.govuk-link").first()
        saveAndReturnLink.text() mustBe ExpectedContent.saveAndReturnLink
        saveAndReturnLink.attr("href") mustBe routes.SubmissionsDashboardController.onPageLoad().url
      }
    }
  }
}