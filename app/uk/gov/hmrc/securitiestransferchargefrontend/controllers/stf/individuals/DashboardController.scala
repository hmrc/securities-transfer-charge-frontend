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
import uk.gov.hmrc.securitiestransferchargefrontend.clients.{DashboardClient, SaveAndReturnClient}
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.domain.UserId
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard.individual.SubmissionsViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.DashboardView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class DashboardController @Inject()(
                                     override val messagesApi: MessagesApi,
                                     enrolledIndividual: StcIndividualAuthEnrolledAction,
                                     val controllerComponents: MessagesControllerComponents,
                                     dashboardClient: DashboardClient,
                                     saveAndReturnClient: SaveAndReturnClient,
                                     view: DashboardView
                                   )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {


  def onPageLoad(): Action[AnyContent] = enrolledIndividual.async { implicit request =>
    val subscriptionId = request.subscriptionId
    val userId = UserId(request.internalId)
    val displayName = request.name

    val fromDate = LocalDate.now().minusMonths(18)
    val toDate = LocalDate.now()
    val dateRange = s"$fromDate-$toDate"

    for {
      overdue <- dashboardClient.getOverdueTransactionsCount(subscriptionId)
      readyToPay <- dashboardClient.getReadyToPayTransactionsCount(subscriptionId)
      drafts <- saveAndReturnClient.listByUser(userId).map(_.size)
      recent <- dashboardClient.getRecentTransactionsCount(subscriptionId, dateRange)
    } yield {
      val viewModel = SubmissionsViewModel(
        overdueCount = overdue,
        readyToPayCount = readyToPay,
        draftCount = drafts,
        recentCount = recent
      )
      Ok(view(viewModel,displayName))
    }
  }
}