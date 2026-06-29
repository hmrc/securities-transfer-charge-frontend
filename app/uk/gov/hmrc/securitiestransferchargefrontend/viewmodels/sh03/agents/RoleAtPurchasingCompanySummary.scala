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

package uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.sh03.agents

import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.single.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.agents.RoleAtPurchasingCompanyPage
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.govuk.summarylist.*
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.implicits.*

object RoleAtPurchasingCompanySummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(RoleAtPurchasingCompanyPage).map { answer =>
      
      val roleText = messages(s"agent.sh03.roleAtPurchasingCompany.${answer.role}")
      
      val valueHtml = answer.uksOrgan match {
        case Some(organ) => s"${HtmlFormat.escape(roleText).toString}<br>${HtmlFormat.escape(organ).toString}"
        case None        => HtmlFormat.escape(roleText).toString
      }

      SummaryListRowViewModel(
        key = "agent.sh03.roleAtPurchasingCompany.checkYourAnswersLabel",
        value = ValueViewModel(HtmlContent(valueHtml)),
        actions = Seq(
          ActionItemViewModel("site.change", routes.RoleAtPurchasingCompanyController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("agent.sh03.roleAtPurchasingCompany.change.hidden"))
        )
      )
    }
}