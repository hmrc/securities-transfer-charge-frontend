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

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.organisations.bulk.TemplateInstructionsView
import uk.gov.hmrc.securitiestransferchargefrontend.utils.CommonHelpers.linkToTemplateFor
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.SH03
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator

import scala.concurrent.ExecutionContext
import javax.inject.{Inject, Named}

class TemplateInstructionsController @Inject()(
                                                override val messagesApi: MessagesApi,
                                                @Named("organisations") stcAuthEnrolled: StcAuthEnrolledAction,
                                                getData: StcDataRetrievalAction,
                                                requireData: StcDataRequiredAction,
                                                @Named("organisations") navigator: Navigator,
                                                val controllerComponents: MessagesControllerComponents,
                                                view: TemplateInstructionsView
                                              ) (implicit ec: ExecutionContext)extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData) { implicit request =>
    val templateUrl = linkToTemplateFor(SH03)(request.request.affinityGroup)
    Ok(view(templateUrl))
  }

  def onReturnToDashboard(): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData).async { implicit request =>
    navigator.returnToDashboard(request.userAnswers)
      .map(Redirect)
  }
}