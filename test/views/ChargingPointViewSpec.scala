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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.individuals.ChargingPointFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.ChargingPointView
import views.ViewBaseSpec

class ChargingPointViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()

  private val viewInstance = app.injector.instanceOf[ChargingPointView]
  private val formProvider = new ChargingPointFormProvider()
  private val form = formProvider()

  def view(): Document = Jsoup.parse(
    viewInstance(form, NormalMode)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title = "When did you buy the securities?"
    val caption = "Transfer details"
    val heading = "When did you buy the securities?"
    val hint = "This is also called the charging point. For example, 12 11 2007"
    val saveAndContinue = "Save and continue"
    val saveAndReturn = "Save and return to dashboard"
  }

  "The ChargingPointView" - {
    "should" - {
      val chargingPointView = view()

      "have the correct title" in {
        chargingPointView.title must include(ExpectedContent.title)
      }

      "display the correct caption text" in {
        chargingPointView.getElementsByClass("govuk-caption-l").text() mustBe ExpectedContent.caption
      }

      "have the correct heading" in {
        chargingPointView.select("h1").text() mustBe ExpectedContent.heading
      }

      "have the correct hint" in {
        chargingPointView.select("#value-hint").text() mustBe ExpectedContent.hint
      }

      "have a button with the text save and continue " in {
        val buttons = chargingPointView.select(".govuk-button")
        buttons.get(0).text() mustBe ExpectedContent.saveAndContinue
      }

      "have a button with the text save and return to dashboard" in {
        val buttons = chargingPointView.select(".govuk-button")
        buttons.get(1).text() mustBe ExpectedContent.saveAndReturn
      }
    }
    }
  }
