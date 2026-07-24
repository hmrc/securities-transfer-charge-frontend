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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents

import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.bulk.routes as bulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.single.routes as singleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.shared.routes as sharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.shared.HowToNotifyAboutShareBuybackFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.HowToNotifyAboutShareBuyback
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.agents.HowToNotifyAboutShareBuybackView

import javax.inject.Inject
import scala.concurrent.Future
import scala.language.postfixOps

class HowToNotifyAboutShareBuybackController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       stcAuthEnrolled: StcAuthEnrolledAction,
                                       getData: StcDataRetrievalAction,
                                       formProvider: HowToNotifyAboutShareBuybackFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: HowToNotifyAboutShareBuybackView
                                     ) extends FrontendBaseController with I18nSupport {

  val backLinkRoute: Call = sharedRoutes.BeforeYouStartController.onPageLoad()

  def onPageLoad(): Action[AnyContent] = (stcAuthEnrolled andThen getData) {implicit request =>

    val innerRequest = request.request
    val form: Form[HowToNotifyAboutShareBuyback] = formProvider(innerRequest.affinityGroupKey)

    Ok(view(form, NormalMode, innerRequest.affinityGroupKey,backLinkRoute))
  }


  def onSubmit(): Action[AnyContent] = (stcAuthEnrolled andThen getData).async {
    implicit request =>

      val innerRequest = request.request
      val form: Form[HowToNotifyAboutShareBuyback] = formProvider(innerRequest.affinityGroupKey)

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(
            BadRequest(view(formWithErrors, NormalMode, innerRequest.affinityGroupKey,backLinkRoute))
          ),

        {
          case HowToNotifyAboutShareBuyback.OneAtATime =>
            Future.successful(Redirect(singleRoutes.AgentReferenceController.onPageLoad(NormalMode)))

          case HowToNotifyAboutShareBuyback.MoreThanOneAtATime =>
            Future.successful(Redirect(bulkRoutes.AgentReferenceController.onPageLoad(NormalMode)))
        }
      )
  }
}
