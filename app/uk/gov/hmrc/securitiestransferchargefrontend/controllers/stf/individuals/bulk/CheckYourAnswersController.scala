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
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.SaveAndReturnButton.isReturn
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{GroupIdentifier, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.STF
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.ValidatedStcRow
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.BulkConfirmationDetails
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.BulkConfirmationDetailsPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.bulk.BulkCheckYourAnswersPage
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.ParsedStcRowsRepository
import uk.gov.hmrc.securitiestransferchargefrontend.services.TransactionSubmissionService
import uk.gov.hmrc.securitiestransferchargefrontend.services.stf.bulk.CheckYourAnswersService
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.bulk.CheckYourAnswersView

import javax.inject.Named
import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            @Named("individuals") navigator: Navigator,
                                            @Named("individuals") stcAuthEnrolled: StcAuthEnrolledAction,
                                            getData: StcDataRetrievalAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: CheckYourAnswersView,
                                            checkYourAnswersService: CheckYourAnswersService,
                                            parsedStcRowsRepository: ParsedStcRowsRepository,
                                            transactionSubmissionService: TransactionSubmissionService,
                                            idClient: SubmissionIdClient
                                          )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport {

  lazy val backLinkCall: Mode => Option[UserAnswers] => Call = mode => _ => navigator.previousPage(BulkCheckYourAnswersPage, mode, None)

  def onPageLoad(reference: String): Action[AnyContent] = (stcAuthEnrolled andThen getData).async { implicit request =>
    implicit val messages: Messages = messagesApi.preferred(request)
    implicit val lang: Lang = messages.lang

    parsedStcRowsRepository.findDocumentByReference(reference).map {
      case Some(parsedStcRowsDocument) =>
        val viewModel = checkYourAnswersService.buildViewModel(parsedStcRowsDocument)

        Ok(view(viewModel, backLinkCall(NormalMode)(request.userAnswers), reference))

      case None => Redirect(routes.JourneyRecoveryController.onPageLoad())
    }
  }


  def onSubmit(reference: String): Action[AnyContent] = (stcAuthEnrolled andThen getData).async {
    implicit request =>
      val userId = UserId(request.request.internalId)
      val group = GroupIdentifier(request.request.groupIdentifier)

      parsedStcRowsRepository.findDocumentByReference(reference).flatMap {
        case Some(parsedStcRowsDocument) =>
          val rows = parsedStcRowsDocument.rows.map(ValidatedStcRow.withNoErrors)

          for {
            submissionId      <- idClient.nextSubmissionId()
            details            = BulkConfirmationDetails(
              submissionId = submissionId,
              paymentDueBy = checkYourAnswersService.paymentDueBy(parsedStcRowsDocument),
              taxDue = checkYourAnswersService.totalTaxDue(parsedStcRowsDocument),
              agentReference = None
            )
            answers           <- Future.fromTry(UserAnswers.empty(userId)(group)(submissionId)(STF)
              .copy(fileUploadReference = Some(reference))
              .set(BulkConfirmationDetailsPage, details)
            )
            nextPage          <- navigator.nextPage(BulkCheckYourAnswersPage, NormalMode, answers, isReturn(request))
            _                  = transactionSubmissionService.submitBulkStf(rows)(using StcDataRequest(request.request, answers))
          } yield Redirect(nextPage)

        case None                       => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))

      }
  }
}
