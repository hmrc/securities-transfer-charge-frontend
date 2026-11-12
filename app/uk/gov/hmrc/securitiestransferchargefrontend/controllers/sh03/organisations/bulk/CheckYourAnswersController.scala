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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.bulk

import com.google.inject.Inject
import play.api.i18n.{I18nSupport, Lang, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.SaveAndReturnButton.isReturn
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.ValidatedStcRow
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.BulkConfirmationDetails
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.BulkConfirmationDetailsPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.bulk.BulkCheckYourAnswersPage
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.ParsedStcRowsRepository
import uk.gov.hmrc.securitiestransferchargefrontend.services.TransactionSubmissionService
import uk.gov.hmrc.securitiestransferchargefrontend.services.sh03.organisations.bulk.CheckYourAnswersService
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.organisations.bulk.CheckYourAnswersView

import javax.inject.Named
import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            @Named("orgSh03") navigator: Navigator,
                                            @Named("organisations") stcAuthEnrolled: StcAuthEnrolledAction,
                                            getData: StcDataRetrievalAction,
                                            requireData: StcDataRequiredAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: CheckYourAnswersView,
                                            checkYourAnswersService: CheckYourAnswersService,
                                            parsedStcRowsRepository: ParsedStcRowsRepository,
                                            transactionSubmissionService: TransactionSubmissionService
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  lazy val backLinkCall: Mode => Option[UserAnswers] => Call = mode => userAnswers => navigator.previousPage(BulkCheckYourAnswersPage, mode, userAnswers)

  def onPageLoad(reference: String): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData).async { implicit request =>
    implicit val messages: Messages = messagesApi.preferred(request)
    implicit val lang: Lang = messages.lang

    parsedStcRowsRepository.findDocumentByReference(reference).map {
      case Some(parsedStcRowsDocument) =>
        val viewModel = checkYourAnswersService.buildViewModel(parsedStcRowsDocument, request.userAnswers)

        Ok(view(viewModel, backLinkCall(NormalMode)(Some(request.userAnswers))))

      case None =>
        Redirect(routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData).async {
    implicit request =>
      val fileUploadRef = request.userAnswers.getFileUploadReference()

      parsedStcRowsRepository.findDocumentByReference(fileUploadRef).flatMap {
        case Some(parsedStcRowsDocument) =>
          val rows = parsedStcRowsDocument.rows.map(ValidatedStcRow.withNoErrors)

          val details = BulkConfirmationDetails(
            submissionId = request.userAnswers.submissionId,
            paymentDueBy = checkYourAnswersService.paymentDueBy(parsedStcRowsDocument),
            taxDue = checkYourAnswersService.totalTaxDue(parsedStcRowsDocument),
            agentReference = None
          )
          
          for {
            answers   <- Future.fromTry(request.userAnswers.set(BulkConfirmationDetailsPage, details))
            nextPage  <- navigator.nextPage(BulkCheckYourAnswersPage, NormalMode, answers, isReturn(request))
            _          = transactionSubmissionService.submitBulkSh03(rows)
          } yield Redirect(nextPage)

        case None =>
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
  }
}