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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.{StcAuthEnrolledAction, StcDataRetrievalAction}
import uk.gov.hmrc.securitiestransferchargefrontend.models.ConfirmationViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.TransactionResponseRepository
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.shared.ConfirmationView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ConfirmationController @Inject()(
                                        val controllerComponents: MessagesControllerComponents,
                                        stcAuthEnrolled: StcAuthEnrolledAction,
                                        getData: StcDataRetrievalAction,
                                        transactionResponseRepository: TransactionResponseRepository,
                                        view: ConfirmationView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (stcAuthEnrolled andThen getData).async {implicit request =>

    val isAgent = request.request.affinityGroupKey.contains("agent")
    val submissionId = request.userAnswers.map(_.submissionId).getOrElse(
      throw new IllegalStateException("Submission ID not found in UserAnswers")
    )

    transactionResponseRepository.retrieve(submissionId).map { responseDetails =>
      val viewModel = ConfirmationViewModel(
        submissionId = submissionId,
        paymentDueBy = responseDetails.paymentDueBy,
        reference = responseDetails.agentReference,
        taxDue = responseDetails.taxDue,
        isAgent = isAgent
      )
      
      Ok(view(viewModel))
    }.recover {
      case _: NoSuchElementException =>
        Redirect(routes.JourneyRecoveryController.onPageLoad())
    }
  }
}
