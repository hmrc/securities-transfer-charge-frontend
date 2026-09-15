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

package uk.gov.hmrc.securitiestransferchargefrontend.services

import uk.gov.hmrc.securitiestransferchargefrontend.clients.registration.NrsClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.StcDataRequest
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.UploadedFile
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.CyaHtmlData

import java.nio.charset.StandardCharsets
import javax.inject.Inject
import scala.concurrent.Future


trait NrsService:
  def singleSubmissionNotableEvent(cyaHtml: CyaHtmlData)(implicit request: StcDataRequest[?]): Future[Unit]
  def bulkSubmissionNotableEvent(uploadedFile: UploadedFile)(implicit request: StcDataRequest[?]): Future[Unit]

class NrsServiceImpl @Inject()(
  nrsClient: NrsClient
) extends NrsService {

  def singleSubmissionNotableEvent(cyaHtml: CyaHtmlData)(implicit request: StcDataRequest[?]): Future[Unit] = {
    nrsClient.postHtmlPayload(cyaHtml.html.toString)
  }

  def bulkSubmissionNotableEvent(uploadedFile: UploadedFile)(implicit request: StcDataRequest[?]): Future[Unit] = {
    nrsClient.postXslxPayload(
      new String(uploadedFile.inputStream.readAllBytes(), StandardCharsets.UTF_8)
    )
  }
  
}
