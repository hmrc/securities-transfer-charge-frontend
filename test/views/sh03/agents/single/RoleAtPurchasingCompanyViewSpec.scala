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

package views.sh03.agents.single

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.shared.RoleAtPurchasingCompanyFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.agents.single.RoleAtPurchasingCompanyView
import views.ViewBaseSpec

class RoleAtPurchasingCompanyViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder(affinityGroup = agentAffinity).build()

  private val messageKeyPrefix = "agent.sh03.roleAtPurchasingCompany"

  private val viewInstance = app.injector.instanceOf[RoleAtPurchasingCompanyView]
  private val formProvider = new RoleAtPurchasingCompanyFormProvider()
  private val testBackLinkRoute: Call = Call("GET", "/back-link")

  private val form = formProvider(affinityGroupKeyAgent)

  def view(): Document = Jsoup.parse(
    viewInstance(form, NormalMode, testBackLinkRoute)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title: String = messages(s"$messageKeyPrefix.title")
    val heading: String = messages(s"$messageKeyPrefix.heading")
    val caption: String = messages("sh03.section.name")

    val director: String = messages(s"$messageKeyPrefix.director")
    val secretary: String = messages(s"$messageKeyPrefix.secretary")
    val personAuthorised: String = messages(s"$messageKeyPrefix.personAuthorised")
    val administrator: String = messages(s"$messageKeyPrefix.administrator")
    val receiver: String = messages(s"$messageKeyPrefix.receiver")
    val receiverManager: String = messages(s"$messageKeyPrefix.receiverManager")
    val cicManager: String = messages(s"$messageKeyPrefix.cicManager")
    val ukSocietas: String = messages(s"$messageKeyPrefix.ukSocietas")
    val uksOrganLabel: String = messages(s"$messageKeyPrefix.uksOrgan.label")
    val orDivider: String = messages("site.or")
    val notProvided: String = messages(s"$messageKeyPrefix.notProvided")
    val requiredError: String = messages(s"$messageKeyPrefix.error.required")

    val saveAndContinue: String = messages("site.save-and-continue.button")
    val saveAndReturn: String = messages("site.save-and-return.button")
  }

  "The RoleAtPurchasingCompanyView" - {

    "render view" - {

      val doc = view()

      "have the correct title" in {
        doc.title() must include(ExpectedContent.title)
      }

      "have the correct heading (rendered as legend)" in {
        doc.select("h1").text() must include(ExpectedContent.heading)
      }

      "have the correct radio buttons" in {
        val radios = doc.select(".govuk-radios").text()

        radios must include(ExpectedContent.director)
        radios must include(ExpectedContent.secretary)
        radios must include(ExpectedContent.personAuthorised)
        radios must include(ExpectedContent.administrator)
        radios must include(ExpectedContent.receiver)
        radios must include(ExpectedContent.receiverManager)
        radios must include(ExpectedContent.cicManager)
        radios must include(ExpectedContent.ukSocietas)
        radios must include(ExpectedContent.orDivider)
        radios must include(ExpectedContent.notProvided)
      }

      "have the conditionally revealed input for UK Societas" in {
        doc.select("#uksOrgan").size() mustBe 1
        doc.select("label[for=uksOrgan]").text() must include(ExpectedContent.uksOrganLabel)
      }

      "have a save and continue button" in {
        doc.select(".govuk-button").first().text() mustBe ExpectedContent.saveAndContinue
      }

      "have a save and return button" in {
        doc.select(".govuk-button--secondary").text() mustBe ExpectedContent.saveAndReturn
      }

      "have a back link" in {
        doc.hasBackLink mustBe true
      }
    }

    "render errors" - {

      "must show an error summary and field error when nothing is selected" in {
        val formWithErrors = form.bind(Map("role" -> ""))
        val doc = Jsoup.parse(viewInstance(formWithErrors, NormalMode, testBackLinkRoute)(fakeRequest, messages).body)

        doc.hasErrorSummary mustBe true
        doc.select(".govuk-error-summary").text() must include(messages("agent.sh03.roleAtPurchasingCompany.error.required"))
      }

      "must show an error when UK Societas is selected but no organ name is provided" in {
        val formWithErrors = form.bind(Map(
          "role" -> "ukSocietas",
          "uksOrgan" -> ""
        ))
        val doc = Jsoup.parse(viewInstance(formWithErrors, NormalMode, testBackLinkRoute)(fakeRequest, messages).body)

        doc.hasErrorSummary mustBe true
        doc.text() must include(messages(s"$messageKeyPrefix.uksOrgan.error.required"))
      }
    }
  }
}