/*
 * Copyright 2025 HM Revenue & Customs
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

package views

import base.SpecBase
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.SubmissionsDashboardView
import views.ViewBaseSpec

class SubmissionsDashboardViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()
  
  private val viewInstance         = app.injector.instanceOf[SubmissionsDashboardView]

  def view(): Document = Jsoup.parse(
    viewInstance()(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title = "Submissions"
    val heading = "Submissions"
    val createNew = "Create new submission"
    
  }

  "The SubmissionsDashboardView" - {
    "the user is an Individual" - {
      val submissionsDashboardView = view()

      "have the correct title" in {
        submissionsDashboardView.title must include(ExpectedContent.title)
      }

      "have the correct heading" in {
        submissionsDashboardView.select("h1").text() mustBe ExpectedContent.heading
      }

      "have a create button with the correct text" in {
        val button = submissionsDashboardView.select(".govuk-button")
        button.text() mustBe ExpectedContent.createNew
      }
    }
  }

}
