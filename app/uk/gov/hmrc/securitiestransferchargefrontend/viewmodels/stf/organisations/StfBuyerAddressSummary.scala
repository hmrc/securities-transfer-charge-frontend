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

package uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.stf.organisations

import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, Country, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.{ConfirmAddressPage, StfBuyersAddressPage}
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.govuk.summarylist.*
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.implicits.*

object StfBuyerAddressSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    val maybeAddress: Option[(List[String], String, Option[Country])] =
      answers.get(ConfirmAddressPage)
        .map(a => (a.lines, a.postcode, a.country))
        .orElse(
          answers.get(StfBuyersAddressPage)
            .map(a => (a.address.lines, a.address.postcode, Some(a.address.country)))
        )

    maybeAddress.map { case (lines, postcode, country) =>

      val addressLines = lines
        .map(line => HtmlFormat.escape(line).body)
        .mkString("<br/>")

      val countryHtml = country
        .map(c => HtmlFormat.escape(c.name).body)
        .getOrElse("")

      val value = Html(
        s"""
           |$addressLines<br/>
           |${HtmlFormat.escape(postcode).body}<br/>
           |$countryHtml
           |""".stripMargin
      )

      SummaryListRowViewModel(
        key = "org.stfBuyerAddress.checkYourAnswersLabel",
        value = ValueViewModel(HtmlContent(value)),
        actions = Seq(
          ActionItemViewModel("site.change", routes.AddressController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("org.stfBuyerAddress.change.hidden"))
        )
      )
    }
}
