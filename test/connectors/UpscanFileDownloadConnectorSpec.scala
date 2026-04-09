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

package connectors

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalatest.BeforeAndAfterAll
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.UpscanFileDownloadConnectorImpl
import utils.WireMockHelper

import java.io.InputStream
import scala.io.Source
import scala.util.Using

class UpscanFileDownloadConnectorSpec extends SpecBase with BeforeAndAfterAll with WireMockHelper {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(50, Millis))

  private lazy val app: Application =
    GuiceApplicationBuilder()
      .configure(
        "auditing.enabled" -> false
      )
      .build()

  private lazy val connector: UpscanFileDownloadConnectorImpl =
    app.injector.instanceOf[UpscanFileDownloadConnectorImpl]

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWireMock()
  }

  override def afterAll(): Unit = {
    stopWireMock()
    super.afterAll()
  }

  "download" - {

    "return an InputStream containing the downloaded bytes when the request succeeds" in {
      wireMockServer.stubFor(
        get(urlEqualTo("/file"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody("hello world")
          )
      )

      val result: InputStream =
        connector.download(s"http://localhost:$wireMockPort/file").futureValue

      val content =
        Using.resource(result) { inputStream =>
          Source.fromInputStream(inputStream).mkString
        }

      content mustBe "hello world"
    }

    "fail the future when the download endpoint returns an error status" in {
      wireMockServer.stubFor(
        get(urlEqualTo("/file"))
          .willReturn(
            aResponse()
              .withStatus(500)
              .withBody("boom")
          )
      )

      val thrown = the[Exception] thrownBy {
        connector.download(s"http://localhost:$wireMockPort/file").futureValue
      }

      thrown.getMessage must include("500")
    }
  }
}