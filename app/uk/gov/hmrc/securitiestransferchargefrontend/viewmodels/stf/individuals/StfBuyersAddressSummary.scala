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

package uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.stf.individuals

import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.single.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.StfBuyersAddressPage
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.govuk.summarylist.*
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.implicits.*

object StfBuyersAddressSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): SummaryListRow =
    answers.get(StfBuyersAddressPage).map {
      address =>
        val addressLines = Seq(
          Some(address.address.lines.mkString("<br>")),
          Some(s"<br>${address.address.postcode}"),
          Some(s"<br>${address.address.country.name}")
        ).flatten.mkString

        SummaryListRowViewModel(
          key     = "checkYourAnswers.buyerAddress",
          value   = ValueViewModel(HtmlContent(Html(addressLines))),
          actions = Seq(
            ActionItemViewModel("site.change", routes.AddressController.onPageLoad().url)
              .withVisuallyHiddenText(messages("checkYourAnswers.buyerAddress.change.hidden"))
          )
        )
    }.getOrElse(
      SummaryListRowViewModel(
        key     = "checkYourAnswers.buyerAddress",
        value   = ValueViewModel(""),
        actions = Seq.empty
      )
    )
}
