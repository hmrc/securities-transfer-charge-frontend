package uk.gov.hmrc.securitiestransferchargefrontend.viewmodels

import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.NameOfSellerPage
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.govuk.summarylist.*
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.implicits.*

object NameOfSellerSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(NameOfSellerPage).map {
      answer =>

        SummaryListRowViewModel(
          key     = "nameOfSeller.checkYourAnswersLabel",
          value   = ValueViewModel(HtmlFormat.escape(answer).toString),
          actions = Seq(
            ActionItemViewModel("site.change", routes.NameOfSellerController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("nameOfSeller.change.hidden"))
          )
        )
    }
}
