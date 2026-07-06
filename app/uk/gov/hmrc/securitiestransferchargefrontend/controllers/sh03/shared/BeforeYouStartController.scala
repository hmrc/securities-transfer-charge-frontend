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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.shared

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.routes as agentRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.routes as orgRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.shared.BeforeYouStartView
import uk.gov.hmrc.auth.core.AffinityGroup
import scala.concurrent.Future

import javax.inject.Inject

class BeforeYouStartController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          stcAuthEnrolled: StcAuthEnrolledAction,
                                          getData: StcDataRetrievalAction,
                                          val controllerComponents: MessagesControllerComponents,
                                          view: BeforeYouStartView
                                        ) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = stcAuthEnrolled {
    implicit request =>
      Ok(view())
  }

  def onSubmit(): Action[AnyContent] = (stcAuthEnrolled andThen getData).async { implicit request =>
    val innerRequest = request.request

    val call = innerRequest.affinityGroup match {
      case AffinityGroup.Organisation =>
        orgRoutes.HowToNotifyAboutShareBuybackController.onPageLoad()
      case AffinityGroup.Agent =>
        agentRoutes.HowToNotifyAboutShareBuybackController.onPageLoad()
      case _ =>
        routes.JourneyRecoveryController.onPageLoad()
    }
    Future.successful(Redirect(call))
  }
}
