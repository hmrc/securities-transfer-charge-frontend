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

package clients

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.EtmpSubmissionClient
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.{StcCharge, StcChargeSuccess, StcTransactionCreateProcessed, StcTransactionCreateProcessedBody, StcTransactionCreateResponse, SubmissionBatchPayload}

import scala.concurrent.Future

class StubEtmpSubmissionClient extends EtmpSubmissionClient:
  override def submitSingleStf(submissionId: SubmissionId, payload: SubmissionBatchPayload)(implicit hc: HeaderCarrier): Future[StcTransactionCreateResponse] =
    Future.successful(
      StcTransactionCreateProcessed(
        StcTransactionCreateProcessedBody(
          processingDate = "2026-09-08",
          charges =  List[StcCharge](
            StcChargeSuccess(
              recordId = 1,
              utrn = "12345678",
              chargeTypeDescription = "Securities Transfer Tax",
              chargeReference = "415990",
              chargeType = "STT",
              chargeAmount = BigDecimal(100.50),
              chargeDueDate = "2026-10-08"
            )
          )
        )
      )
    )
