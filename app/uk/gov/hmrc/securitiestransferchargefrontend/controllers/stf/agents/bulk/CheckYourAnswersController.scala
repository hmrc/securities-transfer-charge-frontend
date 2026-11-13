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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.bulk

import com.google.inject.Inject
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes.JourneyRecoveryController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard.routes as dashboardRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.SaveAndReturnButton.isReturn
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.ValidatedStcRow
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.BulkConfirmationDetails
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.BulkConfirmationDetailsPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.bulk.{BulkAgentReferencePage, BulkCheckYourAnswersPage}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.{CyaHtmlData, CyaHtmlRepository, ParsedStcRowsRepository}
import uk.gov.hmrc.securitiestransferchargefrontend.services.TransactionSubmissionService
import uk.gov.hmrc.securitiestransferchargefrontend.services.stf.agents.bulk.CheckYourAnswersService
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.agents.bulk.CheckYourAnswersView

import javax.inject.Named
import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            @Named("agents") navigator: Navigator,
                                            @Named("agents") stcAuthEnrolled: StcAuthEnrolledAction,
                                            getData: StcDataRetrievalAction,
                                            requireData: StcDataRequiredAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: CheckYourAnswersView,
                                            parsedStcRowsRepository: ParsedStcRowsRepository,
                                            checkYourAnswersService: CheckYourAnswersService,
                                            transactionSubmissionService: TransactionSubmissionService,
                                            cyaHtmlRepository: CyaHtmlRepository
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  lazy val backLinkCall: Mode => UserAnswers => Call = mode => userAnswers => navigator.previousPage(BulkCheckYourAnswersPage, mode, userAnswers)
    
  def onPageLoad(): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData).async {
    implicit request =>
      implicit val messages: Messages = messagesApi.preferred(request)
      val fileUploadRef = request.userAnswers.getFileUploadReference()
      parsedStcRowsRepository.findDocumentByReference(fileUploadRef).map {
        case Some(doc) => 
          val viewModel = checkYourAnswersService.buildViewModel(request.userAnswers, doc)
          val html = view(viewModel, backLinkCall(NormalMode)(request.userAnswers))
          cyaHtmlRepository.store(CyaHtmlData(request.userAnswers.submissionId,html))
          Ok(html)
          
        case _ => 
          Redirect(JourneyRecoveryController.onPageLoad())
        
      }
  }

  def onSubmit(): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData).async {
    implicit request =>
      val fileUploadRef = request.userAnswers.getFileUploadReference()

      parsedStcRowsRepository.findDocumentByReference(fileUploadRef).flatMap {
        case Some(doc) =>
          val rows = doc.rows.map(ValidatedStcRow.withNoErrors)

          val details = BulkConfirmationDetails(
            submissionId = request.userAnswers.submissionId,
            paymentDueBy = checkYourAnswersService.paymentDueBy(doc),
            taxDue = checkYourAnswersService.totalTaxDue(doc),
            agentReference = request.userAnswers.get(BulkAgentReferencePage).flatMap(_.agentReference)
          )

          for {
            answers   <- Future.fromTry(request.userAnswers.set(BulkConfirmationDetailsPage, details))
            nextPage  <-  navigator.nextPage(BulkCheckYourAnswersPage, NormalMode, answers, isReturn(request))
            _         =   transactionSubmissionService.submitBulkStf(rows)
          } yield Redirect(nextPage)

        case None => Future.successful(Redirect(JourneyRecoveryController.onPageLoad()))
      }
  }

  def onReturnToDashboard(): Action[AnyContent] = (stcAuthEnrolled andThen getData).async { implicit request =>
    request.userAnswers match
      case Some(answers) => navigator.returnToDashboard(answers).map(Redirect)
      case None => Future.successful(Redirect(dashboardRoutes.DashboardController.onPageLoad()))
  }
}