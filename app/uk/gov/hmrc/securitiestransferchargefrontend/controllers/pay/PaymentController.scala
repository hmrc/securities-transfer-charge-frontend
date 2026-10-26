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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.pay

import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.PayApiConnector
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions._
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.pay._
import uk.gov.hmrc.securitiestransferchargefrontend.services.DashboardService

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PaymentController @Inject()(
                                   val controllerComponents: MessagesControllerComponents,
                                   auth: StcAuthEnrolledAction,
                                   getData: StcDataRetrievalAction,
                                   dashboardService: DashboardService,
                                   payApiConnector: PayApiConnector,
                                   appConfig: FrontendAppConfig
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def startPayment(submissionId: String): Action[AnyContent] = (auth andThen getData).async { implicit request =>
    val subscriptionId = request.request.subscriptionId
    val subId = SubmissionId(submissionId)

    dashboardService.getSubmission(subscriptionId, subId).flatMap { sttSubmission =>

      val basketItems: Seq[BasketItem] = sttSubmission.transfers.flatMap { transfer =>
        transfer.charges.map { charge =>
          BasketItem(
            buyerName = transfer.buyerNames,
            sellerName = transfer.sellerNames.getOrElse("Not provided"),
            chargeReference = charge.chargeReference,
            chargeType = mapToOPSChargeType(charge.chargeTypeDescription),
            amountInPence = charge.chargeAmountPending.setScale(2).underlying().movePointRight(2).longValueExact()
          )
        }
      }

      val paymentRequest = StartPaymentJourneyRequest(
        returnUrl = appConfig.paymentReturnUrl,
        backUrl = appConfig.paymentBackUrl(submissionId),
        customerId = submissionId,
        submissionId = submissionId,
        basketDetails = BasketDetails(basketItems)
      )

      payApiConnector.startPaymentJourney(paymentRequest).map { response =>
        Redirect(response.nextUrl)
      }
    }
  }

  private def mapToOPSChargeType(etmpDescription: String): ChargeType = {
    val description = etmpDescription.toLowerCase
    if (description.contains("penalty")) {
      ChargeType.LatePaymentPenalty
    } else if (description.contains("interest")) {
      ChargeType.LatePaymentInterest
    } else {
      ChargeType.SecurityTransferCharge
    }
  }
}