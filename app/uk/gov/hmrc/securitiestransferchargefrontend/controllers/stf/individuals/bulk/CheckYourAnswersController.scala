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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.bulk

import com.google.inject.Inject
import play.api.i18n.{I18nSupport, Lang, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SubmissionIdClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.StcDataRequest
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{GroupIdentifier, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.STF
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.ValidatedStcRow
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.BulkConfirmationDetails
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.BulkConfirmationDetailsPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.bulk.BulkCheckYourAnswersPage
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.{CyaHtmlData, CyaHtmlRepository, ParsedStcRowsRepository}
import uk.gov.hmrc.securitiestransferchargefrontend.services.{AnswerPersistenceService, TransactionSubmissionService}
import uk.gov.hmrc.securitiestransferchargefrontend.services.stf.bulk.CheckYourAnswersService
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.bulk.CheckYourAnswersView

import javax.inject.Named
import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            @Named("individuals") navigator: Navigator,
                                            @Named("individuals") stcAuthEnrolled: StcAuthEnrolledAction,
                                            getData: StcDataRetrievalAction,
                                            requireData: StcDataRequiredAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: CheckYourAnswersView,
                                            checkYourAnswersService: CheckYourAnswersService,
                                            parsedStcRowsRepository: ParsedStcRowsRepository,
                                            transactionSubmissionService: TransactionSubmissionService,
                                            idClient: SubmissionIdClient,
                                            cyaHtmlRepository: CyaHtmlRepository,
                                            answerPersistenceService: AnswerPersistenceService

                                          )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport {

  lazy val backLinkCall: Mode => Option[UserAnswers] => Call = mode => _ => navigator.previousPage(BulkCheckYourAnswersPage, mode, None)

  def onPageLoad(reference: String): Action[AnyContent] = (stcAuthEnrolled andThen getData).async { implicit request =>
    implicit val messages: Messages = messagesApi.preferred(request)
    implicit val lang: Lang = messages.lang
    val userId = UserId(request.request.internalId)
    val group = GroupIdentifier(request.request.groupIdentifier)

    parsedStcRowsRepository.findDocumentByReference(reference).flatMap {
      case Some(parsedStcRowsDocument) =>
        val viewModel = checkYourAnswersService.buildViewModel(parsedStcRowsDocument)

        for {
          answers <- request.userAnswers.fold {
            idClient.nextSubmissionId().map { submissionId =>UserAnswers.empty(userId)(group)(submissionId)(STF).setFileUploadReference(reference)}
          }(Future.successful)
          _       <- answerPersistenceService.save(answers)
          html     = view(viewModel, backLinkCall(NormalMode)(Some(answers)))
          _       <- cyaHtmlRepository.store(CyaHtmlData(answers.submissionId, html))
        } yield Ok(html)

      case None => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  def onSubmit(): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData).async {
    implicit request =>
      val fileReference = request.userAnswers.getFileUploadReference()
      parsedStcRowsRepository.findDocumentByReference(fileReference).flatMap {
        case Some(parsedStcRowsDocument) =>
          val rows = parsedStcRowsDocument.rows.map(ValidatedStcRow.withNoErrors)
          val details = BulkConfirmationDetails(
            submissionId = request.userAnswers.submissionId,
            paymentDueBy = checkYourAnswersService.paymentDueBy(parsedStcRowsDocument),
            taxDue = checkYourAnswersService.totalTaxDue(parsedStcRowsDocument),
            agentReference = None
          )

          for {
            answers  <- Future.fromTry(request.userAnswers.set(BulkConfirmationDetailsPage, details))
            nextPage <- navigator.nextPage(BulkCheckYourAnswersPage, NormalMode, answers)
            _         = transactionSubmissionService.submitBulkStf(rows)(using StcDataRequest(request.request, answers))
          } yield Redirect(nextPage)

        case _ => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  def onReturnToDashboard(): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData).async { implicit request =>
    navigator.returnToDashboard(request.userAnswers)
      .map(Redirect)
  }
}
