package uk.gov.hmrc.securitiestransferchargefrontend.viewmodels

import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.HowToNotifyAboutSecuritiesTransferPage
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.govuk.summarylist.*
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.implicits.*

object HowToNotifyAboutSecuritiesTransferSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(HowToNotifyAboutSecuritiesTransferPage).map {
      answer =>

        val value = ValueViewModel(
          HtmlContent(
            HtmlFormat.escape(messages(s"howToNotifyAboutSecuritiesTransfer.$answer"))
          )
        )

        SummaryListRowViewModel(
          key     = "howToNotifyAboutSecuritiesTransfer.checkYourAnswersLabel",
          value   = value,
          actions = Seq(
            ActionItemViewModel("site.change", routes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("howToNotifyAboutSecuritiesTransfer.change.hidden"))
          )
        )
    }
}
