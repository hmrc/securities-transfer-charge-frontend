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
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{GroupIdentifier, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.pagination.PaginationModel
import uk.gov.hmrc.securitiestransferchargefrontend.services.DashboardService
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard.SubmissionsViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.dashboard.SubmissionsView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SubmissionsController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       auth: StcAuthEnrolledAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: SubmissionsView,
                                       dashboardService: DashboardService
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {


  def onPageLoad(
                  draftsPage: Option[Int] = None,
                  readyToPayPage: Option[Int] = None,
                  overduePage: Option[Int] = None,
                  recentPage: Option[Int] = None
                ): Action[AnyContent] = auth.async { implicit request =>

    val subscriptionId = request.subscriptionId
    val userId = UserId(request.internalId)
    val groupId = GroupIdentifier(request.groupIdentifier)

    for {
      allDrafts <- dashboardService.getDrafts(userId, groupId)
      allReadyToPay <- dashboardService.getReadyToPay(subscriptionId)
      allOverdue <- dashboardService.getOverdue(subscriptionId)
      allRecent <- dashboardService.getRecent(userId, groupId, subscriptionId)
    } yield {

      val paginationModel = PaginationModel.forSubmissions(
        draftsPage = draftsPage.getOrElse(1),
        readyToPayPage = readyToPayPage.getOrElse(1),
        overduePage = overduePage.getOrElse(1),
        recentPage = recentPage.getOrElse(1),
        totalDrafts = allDrafts.size,
        totalReadyToPay = allReadyToPay.size,
        totalOverdue = allOverdue.size,
        totalRecent = allRecent.size
      )

      val viewModel = SubmissionsViewModel.build(
        allDrafts = allDrafts,
        allReadyToPay = allReadyToPay,
        allOverdue = allOverdue,
        allRecent = allRecent,
        isAgent = request.isAgent,
        paginationModel = paginationModel
      )

      Ok(view(viewModel))
    }
  }
}