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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.single

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SubmissionIdClient
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.AlfAddressConnector
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.AbstractAddressController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.{StcAuthEnrolledAction, StcDataRetrievalAction}
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{GroupIdentifier, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.STF
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.StfBuyersAddressPage

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class AddressController @Inject()(val controllerComponents: MessagesControllerComponents,
                                  alf: AlfAddressConnector,
                                  auth: StcAuthEnrolledAction,
                                  getData: StcDataRetrievalAction, 
                                  @Named("organisations") val navigator: Navigator,
                                  config: FrontendAppConfig,
                                  idClient: SubmissionIdClient)
                                 (implicit ec: ExecutionContext) extends AbstractAddressController(alf):
  
  def onPageLoad: Action[AnyContent] = auth.async {
    implicit request =>
      super.pageLoad(config.orgAlfConfigFileLocation, config.alfStfOrgContinueUrl)
  }

  def onReturn(addressId: String): Action[AnyContent] = (auth andThen getData).async {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      val userId = UserId(request.request.internalId)
      val group = GroupIdentifier(request.request.groupIdentifier)

      for {
        address     <- super.alfReturn(addressId)
        answers <- request.userAnswers.fold {
          idClient.nextSubmissionId().map(submissionId => UserAnswers.empty(userId)(group)(submissionId)(STF))
        }(Future.successful)
        userAnswers <- Future.fromTry(answers.set(StfBuyersAddressPage, address))
        nextPage    <- navigator.nextPage(StfBuyersAddressPage, NormalMode, userAnswers)
      } yield Redirect(nextPage)
  }