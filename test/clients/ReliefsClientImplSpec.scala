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

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.{ReliefsClientImpl, ReliefsUnavailableException}
import uk.gov.hmrc.securitiestransferchargefrontend.models.Relief
import utils.WireMockHelper

class ReliefsClientImplSpec extends SpecBase with BeforeAndAfterAll with BeforeAndAfterEach with WireMockHelper {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(50, Millis))

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private var app: Application = _

  private def buildApp(): Application =
    GuiceApplicationBuilder()
      .configure(
        "auditing.enabled" -> false,
        "microservice.services.securities-transfer-charge-submissions.host" -> "localhost",
        "microservice.services.securities-transfer-charge-submissions.port" -> wireMockPort
      )
      .build()

  private def client = app.injector.instanceOf[ReliefsClientImpl]

  private val path = "/securities-transfer-charge-submissions/reliefs"

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWireMock()
    app = buildApp()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    wireMockServer.resetAll()
  }

  override def afterAll(): Unit = {
    try {
      if (app != null) app.stop().futureValue
      stopWireMock()
    } finally super.afterAll()
  }

  private def stub(status: Int, body: String): Unit =
    wireMockServer.stubFor(
      get(urlPathEqualTo(path))
        .willReturn(aResponse().withStatus(status).withHeader("Content-Type", "application/json").withBody(body))
    )

  "ReliefsClientImpl.getReliefs" - {

    "must return the reliefs, reading the submissions service's percentage as the rate" in {
      stub(200, """[{"name": "Charities Relief", "percentage": 50}, {"name": "Group Relief", "percentage": 100}]""")

      client.getReliefs().futureValue mustBe Seq(Relief("Charities Relief", 50), Relief("Group Relief", 100))
    }

    "must return an empty list when there are no reliefs" in {
      stub(200, "[]")

      client.getReliefs().futureValue mustBe Seq.empty
    }

    "must fail when the submissions service returns an error status" in {
      stub(502, """{"error": "RELIEFS_UNAVAILABLE"}""")

      client.getReliefs().failed.futureValue mustBe a[ReliefsUnavailableException]
    }

    "must fail when the body is not in the expected shape" in {
      stub(200, """[{"name": "No percentage"}]""")

      client.getReliefs().failed.futureValue mustBe a[ReliefsUnavailableException]
    }
  }
}
