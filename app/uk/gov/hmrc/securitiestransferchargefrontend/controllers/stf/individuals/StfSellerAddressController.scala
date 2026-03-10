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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.AlfAddressConnector
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.AbstractAddressController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.{StcAuthEnrolledAction, StcDataRequiredAction, StcDataRetrievalAction}
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, Mode, NormalMode}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.{AddressPage, StfSellerAddressPage}

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class StfSellerAddressController @Inject()(val controllerComponents: MessagesControllerComponents,
                                           alf: AlfAddressConnector,
                                           auth: StcAuthEnrolledAction,
                                           getData: StcDataRetrievalAction,
                                           requireData: StcDataRequiredAction,
                                           @Named("individuals") val navigator: Navigator,
                                           config: FrontendAppConfig)
                                          (implicit ec: ExecutionContext) extends AbstractAddressController(alf):

  val addressPage: AddressPage = StfSellerAddressPage

  def onPageLoad(mode:Mode): Action[AnyContent] = auth.async {
    implicit request =>
      val returnUrl = mode match {
        case NormalMode => config.alfSellerContinueUrl
        case CheckMode => config.alfStfSellerContinueChangeUrl
      }
      super.pageLoad(config.sellerAlfConfigFileLocation, returnUrl)
  }

  def onReturn(addressId: String, mode:Mode): Action[AnyContent] = (auth andThen getData andThen requireData).async {
    implicit request =>
      for {
        address <- super.alfReturn(addressId)
        userAnswers <- Future.fromTry(request.userAnswers.set(StfSellerAddressPage, address))
        nextPage <- navigator.nextPage(StfSellerAddressPage, mode, userAnswers)
      } yield Redirect(nextPage)
  }