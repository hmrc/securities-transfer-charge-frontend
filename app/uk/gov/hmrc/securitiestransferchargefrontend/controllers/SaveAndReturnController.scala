package uk.gov.hmrc.securitiestransferchargefrontend.controllers

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.StcAuthEnrolledAction
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SaveAndReturnController @Inject()( override val messagesApi: MessagesApi,
                                         val controllerComponents: MessagesControllerComponents,
                                         stcAuthEnrolled: StcAuthEnrolledAction,
                                         navigator: Navigator)
                                       ( implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport:

  def restore(submissionId: SubmissionId): Action[AnyContent] =
    stcAuthEnrolled.async { implicit request =>
      for {
        userAnswers <- navigator.restore(submissionId, request.internalId)
        nextPage = userAnswers.nextPage.getOrElse(Navigator.startPage)
      } yield Redirect(nextPage)
    }
