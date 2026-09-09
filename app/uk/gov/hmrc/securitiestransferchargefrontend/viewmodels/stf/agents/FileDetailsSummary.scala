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
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.govuk.summarylist.*
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.implicits.*

object FileDetailsSummary {
  
  def row(fileName: String)(implicit messages: Messages): Option[SummaryListRow] =
        Some(SummaryListRowViewModel(
          key = messages("agent.checkYourAnswers.fileDetails.key"),
          value = ValueViewModel(HtmlFormat.escape(fileName).toString),
          actions = Seq(
            ActionItemViewModel("site.change", routes.ChangeFileCheckController.onPageLoad().url)
              .withVisuallyHiddenText(messages("agent.checkYourAnswers.changeFile.hidden"))
          )
        )
        )
}
