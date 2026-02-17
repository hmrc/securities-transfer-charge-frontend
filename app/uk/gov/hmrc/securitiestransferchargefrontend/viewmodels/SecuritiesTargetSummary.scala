package uk.gov.hmrc.securitiestransferchargefrontend.viewmodels

import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.SecuritiesTargetPage
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.govuk.summarylist.*
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.implicits.*

object SecuritiesTargetSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(SecuritiesTargetPage).map {
      answer =>

      val value = HtmlFormat.escape(answer.BusinessName).toString + "<br/>" + HtmlFormat.escape(answer.CRN).toString

        SummaryListRowViewModel(
          key     = "securitiesTarget.checkYourAnswersLabel",
          value   = ValueViewModel(HtmlContent(value)),
          actions = Seq(
            ActionItemViewModel("site.change", routes.SecuritiesTargetController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("securitiesTarget.change.hidden"))
          )
        )
    }
}
