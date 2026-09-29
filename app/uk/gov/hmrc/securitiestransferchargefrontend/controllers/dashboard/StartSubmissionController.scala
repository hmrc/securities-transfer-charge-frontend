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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard

import play.api.mvc.*
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.StcAuthEnrolledAction
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.shared.routes as sh03Routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.routes as stfAgentRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.routes as stfIndividualRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.routes as stfOrganisationRoutes

import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes

import javax.inject.{Inject, Singleton}

@Singleton
class StartSubmissionController @Inject()(
                                           val controllerComponents: MessagesControllerComponents,
                                           stcAuthEnrolled: StcAuthEnrolledAction
                                         ) extends FrontendBaseController {

  def startStf(): Action[AnyContent] = stcAuthEnrolled { implicit request =>
    request.affinityGroup match {
      case AffinityGroup.Individual =>
        Redirect(stfIndividualRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad())

      case AffinityGroup.Agent =>
        Redirect(stfAgentRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad())

      case AffinityGroup.Organisation =>
        Redirect(stfOrganisationRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad())
    }
  }

  def startSh03(): Action[AnyContent] =
    stcAuthEnrolled { implicit request =>
      request.affinityGroup match {

        case AffinityGroup.Agent | AffinityGroup.Organisation =>
          Redirect(sh03Routes.BeforeYouStartController.onPageLoad())

        case _ =>
          Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
    }
}
