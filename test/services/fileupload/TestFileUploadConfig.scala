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

package services.fileupload

import play.api.Configuration
import uk.gov.hmrc.securitiestransferchargefrontend.config.FileUploadConfig

object TestFileUploadConfig {

  def config(
              maxRows: Int = 1000,
              expectedWorksheetName: String = "Sheet1",
              firstDataRow: Int = 4,
              row1Hash: String = "dummy-hash-1",
              row2Hash: String = "dummy-hash-2",
              row3Hash: String = "dummy-hash-3"
            ): FileUploadConfig =
    new FileUploadConfig(
      Configuration(
        "file-upload.max-rows" -> maxRows,
        "file-upload.xlsx.expected-worksheet" -> expectedWorksheetName,
        "file-upload.first-data-row" -> firstDataRow,
        "file-upload.template-hashes.individuals.stf.row1" -> row1Hash,
        "file-upload.template-hashes.individuals.stf.row2" -> row2Hash,
        "file-upload.template-hashes.individuals.stf.row3" -> row3Hash
      )
    )
}