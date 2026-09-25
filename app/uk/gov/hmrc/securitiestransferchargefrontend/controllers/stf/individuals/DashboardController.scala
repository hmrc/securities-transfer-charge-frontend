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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{GroupIdentifier, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.services.DashboardService
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard.individual.SubmissionsViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.DashboardView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class DashboardController @Inject()(
                                     override val messagesApi: MessagesApi,
                                     enrolledIndividual: StcIndividualAuthEnrolledAction,
                                     val controllerComponents: MessagesControllerComponents,
                                     dashboardService: DashboardService,
                                     view: DashboardView
                                   )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = enrolledIndividual.async { implicit request =>
    val subscriptionId = request.subscriptionId
    val userId         = UserId(request.internalId)
    val groupId        = GroupIdentifier(request.groupIdentifier)
    val displayName    = request.name

    for {
      counts <- dashboardService.getCounts(userId, groupId, subscriptionId)
      recent <- dashboardService.getRecent(subscriptionId)
    } yield {
      val viewModel = SubmissionsViewModel.fromCounts(counts, recent.size)
      Ok(view(viewModel, displayName))
    }
  }
}