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
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.single.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.ConnectedPersonsPage
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.govuk.summarylist.*
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.implicits.*

object ConnectedPersonsSummary  {

  def row(userAnswers: UserAnswers)(implicit messages: Messages): SummaryListRow = {

    val connectedPersons = userAnswers.get(ConnectedPersonsPage).getOrElse(false)
    val value = if (connectedPersons) "site.yes" else "site.no"

    SummaryListRowViewModel(
      key     = "connectedPersons.checkYourAnswersLabel",
      value   = ValueViewModel(value),
      actions = Seq(
        ActionItemViewModel("site.change", routes.ConnectedPersonsController.onPageLoad(CheckMode).url)
          .withVisuallyHiddenText(messages("connectedPersons.change.hidden"))
      )
    )
  }
}
