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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.single

import com.google.inject.Inject
import play.api.i18n.{I18nSupport, Lang, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.StcDataRequest
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.SaveAndReturnButton.isReturn
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.stf.TaxDueCalculationService
import uk.gov.hmrc.securitiestransferchargefrontend.services.stf.shared.FormattingService
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.govuk.summarylist.*
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.stf.individuals.*
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.stf.shared.single.CheckYourAnswersViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.shared.CheckYourAnswersView

import javax.inject.Named
import scala.concurrent.ExecutionContext

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            @Named("individuals") navigator: Navigator,
                                            stcAuthEnrolled: StcAuthEnrolledAction,
                                            getData: StcDataRetrievalAction,
                                            requireData: StcDataRequiredAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: CheckYourAnswersView,
                                            taxDueCalculationService: TaxDueCalculationService,
                                            formattingService: FormattingService,
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  lazy val backLinkCall: Mode => UserAnswers => Call = mode => userAnswers => navigator.previousPage(CheckYourAnswersPage, mode, userAnswers)

  def onPageLoad(): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData).async {
    implicit request =>
      implicit val messages: Messages = messagesApi.preferred(request)
      implicit val lang: Lang = messages.lang

      navigator.userAnswersValidator.validate(request.userAnswers).map {
        case Left(redirectCall) => Redirect(redirectCall)
        case Right(true) => buildCheckYourAnswersPage(request.userAnswers)
        case Right(false) => Redirect(navigator.errorPage(CheckYourAnswersPage))
      }
  }

  private def buildCheckYourAnswersPage(userAnswers: UserAnswers)(implicit messages: Messages, lang: Lang, request: StcDataRequest[AnyContent]): play.api.mvc.Result = {
    val summaryLists = Seq(
      buildSellerDetailsRows(userAnswers)(messages),
      buildTransferDetailsRows(userAnswers)(messages),
      buildSecuritiesDetailsRows(userAnswers)(messages)
    ).map(rows => SummaryListViewModel(rows = rows))

    val taxDueFormatted = taxDueCalculationService.calculateTaxDue(userAnswers)
      .map(formattingService.formatTaxDue)

    val paymentDueDateFormatted = taxDueCalculationService.calculatePaymentDueDate(userAnswers)
      .map(date => formattingService.formatPaymentDueDate(date)(lang))

    val viewModel = CheckYourAnswersViewModel.fromSummaryLists(
      summaryLists = summaryLists,
      taxDueFormatted = taxDueFormatted,
      paymentDueDateFormatted = paymentDueDateFormatted
    )
    Ok(view(viewModel, backLinkCall(NormalMode)(request.userAnswers), routes.CheckYourAnswersController.onSubmit()))
  }

  def onSubmit(): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData).async {
    implicit request =>
      for {
        nextPage <- navigator.nextPage(CheckYourAnswersPage, NormalMode, request.userAnswers, isReturn(request))
      } yield Redirect(nextPage)
  }
  
  def buildYourDetailsRows(userAnswers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
    val addressRow = userAnswers.get(ConfirmAddressPage)
      .map(_ => ConfirmAddressSummary.row(userAnswers))
      .orElse(userAnswers.get(StfBuyersAddressPage).map(_ => StfBuyersAddressSummary.row(userAnswers)))
      .getOrElse(ConfirmAddressSummary.row(userAnswers))

    Seq(addressRow)
  }

  def buildSellerDetailsRows(userAnswers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
    Seq(
      NameOfSellerSummary.row(userAnswers),
      StfSellerAddressSummary.row(userAnswers)
    ).flatten
  }

  def buildTransferDetailsRows(userAnswers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
    val applyingForRelief = userAnswers.get(ApplyingForReliefPage).contains(true)

    val baseRows = Seq(
      ConnectedPersonsSummary.row(userAnswers),
      ApplyingForReliefSummary.row(userAnswers)
    )

    val reliefRow = if (applyingForRelief) {
      WhatReliefAreYouApplyingForSummary.row(userAnswers).toSeq
    } else {
      Seq.empty
    }

    val additionalRows = Seq(
      ChargingPointSummary.row(userAnswers),
      TaxRateSummary.row(userAnswers)
    )

    baseRows ++ reliefRow ++ SecuritiesTargetSummary.row(userAnswers) ++ additionalRows
  }

  def buildSecuritiesDetailsRows(userAnswers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
    val purchasingShares = userAnswers.get(PurchasingSharesPage).getOrElse(false)
    val isConnectedPersons = userAnswers.get(ConnectedPersonsPage).contains(true)
    val whatTypeRow = PurchasingSharesSummary.row(userAnswers)

    if (purchasingShares)
      buildSharesDetailsRows(userAnswers, whatTypeRow, isConnectedPersons)
    else
      buildOtherSecuritiesDetailsRows(userAnswers, whatTypeRow, isConnectedPersons)
  }

  private def buildSharesDetailsRows(
                                      userAnswers: UserAnswers,
                                      whatTypeRow: SummaryListRow,
                                      isConnectedPersons: Boolean
                                    )(implicit messages: Messages): Seq[SummaryListRow] = {
    val shareDetailsRows = DetailsOfThisTransferSummary.rows(userAnswers, showMarketValue = isConnectedPersons)
    Seq(whatTypeRow) ++ shareDetailsRows
  }

  private def buildOtherSecuritiesDetailsRows(
                                               userAnswers: UserAnswers,
                                               whatTypeRow: SummaryListRow,
                                               isConnectedPersons: Boolean
                                             )(implicit messages: Messages): Seq[SummaryListRow] = {
    val otherTypeRow = OtherSecuritiesTypeSummary.row(userAnswers).toSeq
    val amountPaidRow = Seq(AmountPaidForSecuritiesSummary.row(userAnswers))
    val marketValueRow = if (isConnectedPersons) {
      Seq(TotalMarketValueSummary.row(userAnswers))
    } else {
      Seq.empty
    }

    Seq(whatTypeRow) ++ otherTypeRow ++ amountPaidRow ++ marketValueRow
  }
}
