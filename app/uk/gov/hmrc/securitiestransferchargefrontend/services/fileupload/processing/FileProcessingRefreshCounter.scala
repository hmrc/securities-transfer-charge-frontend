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

package uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.processing

import play.api.mvc.Request
import play.api.mvc.Result
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig

import javax.inject.Inject

trait FileProcessingRefreshCounter:
  def currentCount: Int
  def isTimedOut: Boolean
  def withIncrementedCounter(in: Result): Result
  def reset(in: Result): Result

final class SessionFileProcessingRefreshCounter(appConfig: FrontendAppConfig, request: Request[?]) extends FileProcessingRefreshCounter:

  private val retryCountKey = "retryCount"
  private val maxRetries = appConfig.spinnerPageRefreshTimeout / appConfig.spinnerPageRefreshInterval

  override def currentCount:  Int = request.session.get(retryCountKey).map(_.toInt).getOrElse(0)

  override def isTimedOut: Boolean =
    currentCount >= maxRetries

  override def withIncrementedCounter(in: Result): Result =
    in.withSession(request.session + (retryCountKey -> (currentCount + 1).toString))

  override def reset(in: Result): Result =
    in.withSession(request.session - retryCountKey)

trait FileProcessingRefreshCounterFactory:
  def apply(request: Request[?]): FileProcessingRefreshCounter

final class DefaultFileProcessingRefreshCounterFactory @Inject()(appConfig: FrontendAppConfig) extends FileProcessingRefreshCounterFactory:
  override def apply(request: Request[?]): FileProcessingRefreshCounter =
    new SessionFileProcessingRefreshCounter(appConfig, request)
