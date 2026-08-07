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
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.ConfirmAddressPage
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.govuk.summarylist.*
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.implicits.*

object ConfirmAddressSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): SummaryListRow =
    answers.get(ConfirmAddressPage).map {
      confirmedAddress =>
        val addressLines = Seq(
          Some(confirmedAddress.lines.mkString("<br>")),
          Some(s"<br>${confirmedAddress.postcode}"),
          confirmedAddress.country.map(c => s"<br>${c.name}")
        ).flatten.mkString

        SummaryListRowViewModel(
          key     = "checkYourAnswers.confirmAddress",
          value   = ValueViewModel(HtmlContent(Html(addressLines))),
          actions = Seq(
            ActionItemViewModel("site.change", routes.ConfirmAddressController.onPageLoad().url)
              .withVisuallyHiddenText(messages("checkYourAnswers.confirmAddress.change.hidde"))
          )
        )
    }.getOrElse(
      SummaryListRowViewModel(
        key     = "checkYourAnswers.confirmAddress",
        value   = ValueViewModel(messages("site.notProvided")),
        actions = Seq(
          ActionItemViewModel("site.change", routes.ConfirmAddressController.onPageLoad().url)
            .withVisuallyHiddenText(messages("checkYourAnswers.confirmAddress.change.hidde"))
        )
      )
    )
}