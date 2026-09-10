package uk.gov.hmrc.securitiestransferchargefrontend.utils

import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject

trait HeaderCarrierCreator:
  def create(request: RequestHeader): HeaderCarrier
  
final class HeaderCarrierCreatorImpl @Inject() extends HeaderCarrierCreator:
  def create(request: RequestHeader): HeaderCarrier =
    HeaderCarrierConverter.fromRequestAndSession(request, request.session)


