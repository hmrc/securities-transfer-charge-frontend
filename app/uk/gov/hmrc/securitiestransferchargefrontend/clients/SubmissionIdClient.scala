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

package uk.gov.hmrc.securitiestransferchargefrontend.clients

import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.http.HttpReads.Implicits.*


import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait SubmissionIdClient:
  def nextSubmissionId()(implicit hc: HeaderCarrier): Future[SubmissionId]

@Singleton
class SubmissionIdClientImpl @Inject()(
                                        httpClient: HttpClientV2,
                                        appConfig: FrontendAppConfig
                                      )(implicit ec: ExecutionContext)
  extends SubmissionIdClient {

  override def nextSubmissionId()(implicit hc: HeaderCarrier): Future[SubmissionId] = {

    httpClient
      .post(url"${appConfig.saveAndReturnUrl}/submission-id")
      .execute[SubmissionId]
  }
}
