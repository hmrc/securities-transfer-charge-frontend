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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.bulk

import com.google.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes.JourneyRecoveryController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.SaveAndReturnButton.isReturn
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.bulk.BulkCheckYourAnswersPage
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.ParsedStcRowsRepository
import uk.gov.hmrc.securitiestransferchargefrontend.services.sh03.agents.bulk.CheckYourAnswersService
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.agents.bulk.CheckYourAnswersView

import javax.inject.Named
import scala.concurrent.ExecutionContext


class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            @Named("agentsSh03") navigator: Navigator,
                                            stcAuthEnrolled: StcAuthEnrolledAction,
                                            getData: StcDataRetrievalAction,
                                            requireData: StcDataRequiredAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: CheckYourAnswersView,
                                            parsedStcRowsRepository: ParsedStcRowsRepository,
                                            checkYourAnswersService: CheckYourAnswersService
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {
  
  lazy val backLinkCall: Mode => UserAnswers => Call = mode => userAnswers => navigator.previousPage(BulkCheckYourAnswersPage, mode, userAnswers)
  
  def onPageLoad(): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData).async {
    implicit request =>
      val fileUploadRef = request.userAnswers.getFileUploadReference()
      parsedStcRowsRepository.findDocumentByReference(fileUploadRef).map {
        case Some(doc) => {
          val viewModel = checkYourAnswersService.buildViewModel(request.userAnswers, doc)
          Ok(view(viewModel, backLinkCall(NormalMode)(request.userAnswers)))
        }
        case _ => {
          Redirect(JourneyRecoveryController.onPageLoad())
        }
      }
  }

  def onSubmit(): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData).async {
    implicit request =>
      for {
        nextPage <- navigator.nextPage(BulkCheckYourAnswersPage, NormalMode, request.userAnswers, isReturn(request))
      } yield Redirect(nextPage)
  }

}
