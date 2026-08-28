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

package uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.sh03.organisations

import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.single.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.CompanyDetailsPage
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.govuk.summarylist.*
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.implicits.*

object CompanyDetailsSummary {

  def rows(answers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
    answers.get(CompanyDetailsPage).toSeq.flatMap { companyDetails =>

      val companyNameRow = SummaryListRowViewModel(
        key = "org.sh03.companyDetails.companyName.label",
        value = ValueViewModel(HtmlFormat.escape(companyDetails.companyName).toString),
        actions = Seq(
          ActionItemViewModel("site.change", routes.CompanyDetailsController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("org.sh03.companyDetails.companyName.change.hidden"))
        )
      )

      val crnRow = SummaryListRowViewModel(
        key = "org.sh03.companyDetails.crn.label",
        value = ValueViewModel(HtmlFormat.escape(companyDetails.companyRegistrationNumber).toString),
        actions = Seq(
          ActionItemViewModel("site.change", routes.CompanyDetailsController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("org.sh03.companyDetails.crn.change.hidden"))
        )
      )

      val isPlcRow = SummaryListRowViewModel(
        key = "org.sh03.companyDetails.isPlc.label",
        value = ValueViewModel(if (companyDetails.isPlc) messages("site.yes") else messages("site.no")),
        actions = Seq(
          ActionItemViewModel("site.change", routes.CompanyDetailsController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("org.sh03.companyDetails.isPlc.change.hidden"))
        )
      )

      Seq(companyNameRow, crnRow, isPlcRow)
    }
  }
}