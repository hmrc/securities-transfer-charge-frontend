package uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.sh03.organisations.single

import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.WhatReliefAreYouApplyingForViewPage
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.govuk.summarylist.*
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.implicits.*

object WhatReliefAreYouApplyingForViewSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(WhatReliefAreYouApplyingForViewPage).map {
      answer =>

        SummaryListRowViewModel(
          key     = "whatReliefAreYouApplyingForView.checkYourAnswersLabel",
          value   = ValueViewModel(HtmlFormat.escape(answer).toString),
          actions = Seq(
            ActionItemViewModel("site.change", routes.WhatReliefAreYouApplyingForViewController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("whatReliefAreYouApplyingForView.change.hidden"))
          )
        )
    }
}
