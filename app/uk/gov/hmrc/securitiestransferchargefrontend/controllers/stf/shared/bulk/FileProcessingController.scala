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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.bulk

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.processing.FileProcessingRefreshCounterFactory
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.shared.bulk.{BulkUploadErrorView, FileProcessingView}

import javax.inject.Inject

class FileProcessingController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          stcAuthEnrolled: StcAuthEnrolledAction,
                                          spinnerView: FileProcessingView,
                                          timeoutView: BulkUploadErrorView,
                                          createCounter: FileProcessingRefreshCounterFactory,
                                          val controllerComponents: MessagesControllerComponents,
                                          appConfig: FrontendAppConfig
                                        ) extends FrontendBaseController with I18nSupport {


  private val retryCountKey = "retryCount"
  private val refreshInterval = appConfig.spinnerPageRefreshInterval

  def currentCount: Request[_] => Int = _.session.get(retryCountKey).map(_.toInt).getOrElse(0)

  def onPageLoad(): Action[AnyContent] =
    stcAuthEnrolled { implicit request =>
      val counter = createCounter(request)
      if (counter.isTimedOut) {
        Redirect(routes.FileProcessingController.onTimeout())
      } else {
        counter.withIncrementedCounter(Ok(spinnerView(refreshInterval)))
      }
    }

  def onTimeout(): Action[AnyContent] =
    stcAuthEnrolled { implicit request =>
      Ok(timeoutView())
    }
}
