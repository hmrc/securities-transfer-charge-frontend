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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.{SubscriptionConnector, SubscriptionStatusErrorException}
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.organisations.ConfirmAddressPage
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.SubscriptionDataRepository
import uk.gov.hmrc.securitiestransferchargefrontend.services.AddressService
import uk.gov.hmrc.securitiestransferchargefrontend.utils.CommonHelpers.FutureOptionOps
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.organisations.ConfirmAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionDataNotFoundException(msg: String) extends RuntimeException(msg)

class ConfirmAddressController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          stcAuthEnrolled: StcAuthEnrolledAction,
                                          getData: StcDataRetrievalAction,
                                          requireData: StcDataRequiredAction,
                                          subscriptionConnector: SubscriptionConnector,
                                          subscriptionDataRepository: SubscriptionDataRepository,
                                          val controllerComponents: MessagesControllerComponents,
                                          view: ConfirmAddressView,
                                          addressService: AddressService,
                                          navigator: Navigator
                                        )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] =
    (stcAuthEnrolled andThen getData).async { implicit request =>

      subscriptionConnector.getValidSubscription(request.request.subscriptionId)
        .map { subscription =>
          Ok(view(addressService.extractConfirmableAddress(subscription)))
        }
        .recover {
          //Need to confirm Kick out page
          case _: SubscriptionStatusErrorException => Redirect(routes.JourneyRecoveryController.onPageLoad())
        }
    }

  def onSubmit: Action[AnyContent] =
    (stcAuthEnrolled andThen getData andThen requireData).async { implicit request =>
      (
        for {
          subscriptionData <- subscriptionDataRepository.getSubscriptionData(request.request.subscriptionId)
            .getOrFail(new SubscriptionDataNotFoundException("Subscription data not found"))
          address = addressService.extractConfirmableAddress(subscriptionData.subscriptionDetails)
          updatedAnswers <- Future.fromTry(request.userAnswers.set(ConfirmAddressPage, address))
          nextPage <- navigator.nextPage(ConfirmAddressPage, NormalMode, updatedAnswers)
        } yield Redirect(nextPage)
        ).recover {
        case _: SubscriptionDataNotFoundException => Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
    }
}