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

package uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload

object StcReliefOptions {

  val allowedValues: Set[String] = Set(
    "Charities Relief",
    "Demutualisation of Insurance Companies",
    "Financial Institutions in Resolution Exemption",
    "Group Relief",
    "Growth Market Exemption",
    "Hybrid Capital Instruments",
    "Intermediary Relief",
    "Insolvency/Bankruptcy Relief",
    "Maintenance Funds for Historic Buildings",
    "PISCES",
    "Qualifying Asset Holding Companies Relief",
    "Reconstruction & Acquisition Relief",
    "Share Incentive Plans",
    "Stock borrowing and repurchase arrangements",
    "UK Listing Relief"
  )

  private val normalisedAllowedValues: Set[String] =
    allowedValues.map(normalise)

  def isAllowed(value: String): Boolean =
    normalisedAllowedValues.contains(normalise(value))

  private def normalise(value: String): String =
    value.trim.toLowerCase
}