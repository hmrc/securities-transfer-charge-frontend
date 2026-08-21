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

package uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.stf.agents

import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.single.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.SecuritiesTargetPage
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.govuk.summarylist.*
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.implicits.*

object SecuritiesTargetSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[Seq[SummaryListRow]] = {
    answers.get(SecuritiesTargetPage).map { target =>

      val businessNameRow = SummaryListRowViewModel(
        key = "checkYourAnswers.businessName",
        value = ValueViewModel(HtmlFormat.escape(target.businessName).toString),
        actions = Seq(
          ActionItemViewModel("site.change", routes.SecuritiesTargetController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("checkYourAnswers.securitiesTarget.businessName.change.hidden"))
        )
      )

      val crnRow = SummaryListRowViewModel(
        key = "checkYourAnswers.companyCrn",
        value = ValueViewModel(target.crn.getOrElse(messages("site.notProvided"))),
        actions = Seq(
          ActionItemViewModel("site.change", routes.SecuritiesTargetController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("checkYourAnswers.securitiesTarget.crn.change.hidden"))
        )
      )

      Seq(businessNameRow, crnRow)
    }
  }
}
