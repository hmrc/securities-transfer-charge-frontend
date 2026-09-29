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

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{GroupIdentifier, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.nrs.IdentityData
import uk.gov.hmrc.securitiestransferchargefrontend.services.DashboardService
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard.SubmissionsViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.dashboard.DashboardView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class DashboardController @Inject()(
                                     override val messagesApi: MessagesApi,
                                     stcAuthEnrolled: StcAuthEnrolledAction,
                                     val controllerComponents: MessagesControllerComponents,
                                     dashboardService: DashboardService,
                                     view: DashboardView
                                   )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {


  def onPageLoad(): Action[AnyContent] = stcAuthEnrolled.async { implicit request =>
    val subscriptionId = request.subscriptionId
    val userId = UserId(request.internalId)
    val groupId = GroupIdentifier(request.groupIdentifier)
    val displayName = getName(request.affinityGroup, request.identityData)
    for {
      counts <- dashboardService.getCounts(userId, groupId, subscriptionId)
    } yield {
      val viewModel = SubmissionsViewModel(counts.overdue, counts.readyToPay, counts.drafts)
      Ok(view(viewModel, displayName, request.isIndividual))
    }
  }

  private def getName(affinityGroup: AffinityGroup, data: IdentityData): String = {
    val defaultName = "Securities Transfer Tax"

    affinityGroup match {
      case AffinityGroup.Individual =>
        data.itmpName.flatMap { name =>
            val fullName = s"${name.givenName.getOrElse("")} ${name.familyName.getOrElse("")}".trim
            Option.when(fullName.nonEmpty)(fullName)
          }
          .getOrElse(defaultName)

      case AffinityGroup.Agent =>
        data.agentInformation.flatMap(_.agentFriendlyName).getOrElse(defaultName)

      case _ => defaultName
    }
  }
}