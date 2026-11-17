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
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.{EtmpSubmissionClientImpl, SubmissionResponseException, SubmissionResponseParsingException}
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{SubscriptionId, TransferType}
import uk.gov.hmrc.securitiestransferchargefrontend.models.Headers
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.*
import utils.WireMockHelper

import java.time.LocalDate

class EtmpSubmissionClientImplSpec extends SpecBase with BeforeAndAfterAll with BeforeAndAfterEach with WireMockHelper {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(50, Millis))

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  // Built in beforeAll, once WireMock is running and its port is known.
  private var app: Application = _

  private def buildApp(): Application =
    GuiceApplicationBuilder()
      .configure(
        "auditing.enabled" -> false,
        "microservice.services.securities-transfer-charge-submissions.host" -> "localhost",
        "microservice.services.securities-transfer-charge-submissions.port" -> wireMockPort
      )
      .build()

  private def client = app.injector.instanceOf[EtmpSubmissionClientImpl]

  private val subscriptionId = SubscriptionId("XASTC0000123456")
  private val utrn           = "900459020010"
  private val path           = s"/securities-transfer-charge-submissions/submission/$utrn"

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

  private def stub(status: Int, body: JsObject): Unit =
    stubRaw(status, body.toString, "application/json")

  private def stubRaw(status: Int, body: String, contentType: String): Unit =
    wireMockServer.stubFor(
      get(urlPathEqualTo(path))
        .willReturn(aResponse().withStatus(status).withHeader("Content-Type", contentType).withBody(body))
    )

  // Shape returned by the submissions service, including its "isAmountUnasertainable" spelling.
  private val transactionJson: JsObject = Json.obj(
    "processingDate" -> "2026-03-31T12:00:00Z",
    "submissionId"   -> "sub-123",
    "submissionDate" -> "2026-03-30",
    "recordId"       -> 1,
    "transactionDetails" -> Json.obj(
      "transactionType"                -> 1,
      "typeOfSecurity"                 -> "Ordinary shares",
      "numberOfShares"                 -> 10,
      "originalChargingPoint"          -> "2026-03-30",
      "considerationActual"            -> 1000,
      "isConnectedPartiesTransactions" -> false,
      "companyName"                    -> "Company Ltd"
    ),
    "contingentDetails" -> Json.arr(Json.obj(
      "provisionalId"          -> "1",
      "provisionalDate"        -> "2026-03-30",
      "isAmountUnasertainable" -> true,
      "defermentOfPayment"     -> false,
      "actualDate"             -> "2026-05-01"
    )),
    "mainSellerDetails" -> Json.obj("sellerName" -> "Seller Ltd", "addr1" -> "1 Main St", "postcode" -> "AA1 1AA", "country" -> "GB"),
    "otherSellers"      -> Json.arr(Json.obj("sellerName" -> "Other Seller")),
    "mainBuyerDetails"  -> Json.obj(
      "buyerName" -> "Buyer Ltd", "addr1" -> "2 High St", "postcode" -> "BB2 2BB", "country" -> "GB",
      "email" -> "buyer@test.com", "taxRate" -> 1
    ),
    "otherBuyers" -> Json.arr(),
    "declaration" -> Json.obj(
      "role1" -> "1", "name" -> "Declarer", "addr1" -> "1 Main St", "postcode" -> "AA1 1AA", "country" -> "GB",
      "isCorrectInfo" -> true
    ),
    "charges" -> Json.arr(Json.obj(
      "chargeTypeDescription" -> "Securities Transfer Charge",
      "chargeReference"       -> "XA123",
      "chargeType"            -> "STF",
      "chargeAmount"          -> 10.00,
      "chargeDueDate"         -> "2026-04-30"
    ))
  )

  "EtmpSubmissionClientImpl.getTransaction" - {

    "return Found with the parsed transaction for a 200" in {
      stub(200, transactionJson)

      whenReady(client.getTransaction(subscriptionId, utrn)) {
        case GetTransactionResult.Found(t) =>
          t.submissionId mustBe "sub-123"
          t.submissionDate mustBe LocalDate.parse("2026-03-30")
          t.transactionDetails.transactionType mustBe TransferType.STF
          t.transactionDetails.isConnectedPartiesTransactions mustBe false
          t.contingentDetails.head.isAmountUnascertainable mustBe true
          t.contingentDetails.head.actualDate mustBe Some(LocalDate.parse("2026-05-01"))
          t.mainSellerDetails.map(_.sellerName) mustBe Some("Seller Ltd")
          t.otherSellers.map(_.sellerName) mustBe Seq("Other Seller")
          t.mainBuyerDetails.taxRate mustBe BuyerTaxRate.HalfPercent
          t.agentDetails mustBe None
          t.declaration.role1 mustBe Some(DeclarationRole.Director)
          t.declaration.isCorrectInfo mustBe true
          t.charges.map(_.chargeAmount) mustBe Seq(BigDecimal(10))
        case other => fail(s"Unexpected result: $other")
      }
    }

    "send the correlation-id and subscription-id headers" in {
      stub(200, transactionJson)

      client.getTransaction(subscriptionId, utrn).futureValue

      wireMockServer.verify(
        getRequestedFor(urlPathEqualTo(path))
          .withHeader(Headers.SubscriptionId, equalTo(subscriptionId.value))
          .withHeader(Headers.CorrelationId, matching("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
      )
    }

    "return UtrnNotFound for a 404 with the UTRN_NOT_FOUND code" in {
      stub(404, Json.obj("error" -> "UTRN not found", "code" -> "UTRN_NOT_FOUND"))

      client.getTransaction(subscriptionId, utrn).futureValue mustBe GetTransactionResult.UtrnNotFound
    }

    "return StcIdNotFound for a 404 with the STC_ID_NOT_FOUND code" in {
      stub(404, Json.obj("error" -> "STC ID not found", "code" -> "STC_ID_NOT_FOUND"))

      client.getTransaction(subscriptionId, utrn).futureValue mustBe GetTransactionResult.StcIdNotFound
    }

    "match on the code, not the human-readable message" in {
      stub(404, Json.obj("error" -> "reworded message", "code" -> "UTRN_NOT_FOUND"))

      client.getTransaction(subscriptionId, utrn).futureValue mustBe GetTransactionResult.UtrnNotFound
    }

    "fail with SubmissionResponseException for a 404 without a recognised code" in {
      stub(404, Json.obj("error" -> "UTRN not found"))

      client.getTransaction(subscriptionId, utrn).failed.futureValue mustBe a[SubmissionResponseException]
    }

    "fail with SubmissionResponseException for a 404 whose body is not JSON" in {
      stubRaw(404, "<html><body>Not Found</body></html>", "text/html")

      client.getTransaction(subscriptionId, utrn).failed.futureValue mustBe a[SubmissionResponseException]
    }

    "return InvalidRequest for a 400" in {
      stub(400, Json.obj("error" -> "invalid request"))

      client.getTransaction(subscriptionId, utrn).futureValue mustBe GetTransactionResult.InvalidRequest
    }

    "fail with SubmissionResponseException for a 500" in {
      stub(500, Json.obj("error" -> "failed to retrieve transaction"))

      client.getTransaction(subscriptionId, utrn).failed.futureValue mustBe a[SubmissionResponseException]
    }

    "fail with SubmissionResponseParsingException when a 200 body is missing fields" in {
      stub(200, Json.obj("processingDate" -> "2026-03-31T12:00:00Z"))

      client.getTransaction(subscriptionId, utrn).failed.futureValue mustBe a[SubmissionResponseParsingException]
    }

    "fail with SubmissionResponseParsingException when a 200 body is not JSON" in {
      stubRaw(200, "not json", "text/plain")

      client.getTransaction(subscriptionId, utrn).failed.futureValue mustBe a[SubmissionResponseParsingException]
    }

    "prefer the correctly spelt isAmountUnascertainable key when both spellings are present" in {
      val contingent = Json.obj(
        "provisionalId"           -> "1",
        "provisionalDate"         -> "2026-03-30",
        "isAmountUnasertainable"  -> false,
        "isAmountUnascertainable" -> true,
        "defermentOfPayment"      -> false
      )
      stub(200, transactionJson ++ Json.obj("contingentDetails" -> Json.arr(contingent)))

      whenReady(client.getTransaction(subscriptionId, utrn)) {
        case GetTransactionResult.Found(t) => t.contingentDetails.head.isAmountUnascertainable mustBe true
        case other                         => fail(s"Unexpected result: $other")
      }
    }
  }
}
