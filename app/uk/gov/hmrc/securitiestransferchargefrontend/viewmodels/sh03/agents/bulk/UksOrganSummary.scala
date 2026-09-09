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

package uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.sh03.agents.bulk

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.bulk.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.RoleAtPurchasingCompanyPage
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.govuk.summarylist.*
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.implicits.*

object UksOrganSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(RoleAtPurchasingCompanyPage).flatMap { answer =>
      val fileUploadRef = answers.getFileUploadReference()
      
      answer.uksOrgan match {
        case Some(organ) => {
          Some(SummaryListRowViewModel(
            key = "agent.sh03.roleAtPurchasingCompany.uksOrgan.label",
            value = ValueViewModel(HtmlContent(organ)),
            actions = Seq(
              ActionItemViewModel("site.change", routes.RoleAtPurchasingCompanyController.onPageLoad(CheckMode, fileUploadRef).url)
                .withVisuallyHiddenText(messages("agent.sh03.roleAtPurchasingCompany.uksOrgan.hidden"))
            )
          ))
        }
        case None        => None
      }
      

      
    }
}