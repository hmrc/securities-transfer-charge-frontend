/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers

import play.api.i18n.{I18nSupport, Lang, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.{StcAuthEnrolledAction, StcDataRequiredAction, StcDataRetrievalAction}
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.ConfirmationViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.BulkConfirmationDetails
import uk.gov.hmrc.securitiestransferchargefrontend.pages.BulkConfirmationDetailsPage
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.{SessionRepository, TransactionResponseRepository}
import uk.gov.hmrc.securitiestransferchargefrontend.services.stf.shared.FormattingService
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.shared.ConfirmationView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class ConfirmationController @Inject()(
                                        val controllerComponents: MessagesControllerComponents,
                                        @Named("any") stcAuthEnrolled: StcAuthEnrolledAction,
                                        getData: StcDataRetrievalAction,
                                        requireData: StcDataRequiredAction,
                                        sessionRepository: SessionRepository,
                                        transactionResponseRepository: TransactionResponseRepository,
                                        view: ConfirmationView,
                                        formatter: FormattingService
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData).async { implicit request =>
    implicit val messages: Messages = messagesApi.preferred(request)
    implicit val lang: Lang = messages.lang

    val isAgent = request.request.affinityGroupKey.contains("agent")
    val userAnswers = request.userAnswers

    sessionRepository.clear(request.userAnswers.userId)

    userAnswers.get(BulkConfirmationDetailsPage) match {
      case Some(details) =>
        Future.successful(Ok(view(ConfirmationViewModel(
          submissionId = details.submissionId.value,
          paymentDueBy = formatter.formatPaymentDueDate(details.paymentDueBy),
          reference = details.agentReference,
          taxDue = formatter.formatCurrency(details.taxDue),
          isAgent = isAgent,
          journeyType = userAnswers.journeyType,
          isBulk = true
        ))))

      case None =>
        transactionResponseRepository.retrieve(userAnswers.submissionId).map { responseDetails =>
          val viewModel = ConfirmationViewModel(
            submissionId = userAnswers.submissionId.value,
            paymentDueBy = formatter.formatPaymentDueDate(responseDetails.paymentDueBy),
            reference = responseDetails.agentReference,
            taxDue = formatter.formatCurrency(responseDetails.taxDue),
            isAgent = isAgent,
            journeyType = userAnswers.journeyType,
            isBulk = false
          )

          Ok(view(viewModel))
        }.recover {
          case _: NoSuchElementException =>
            Redirect(routes.JourneyRecoveryController.onPageLoad())
        }
    }
  }
}
