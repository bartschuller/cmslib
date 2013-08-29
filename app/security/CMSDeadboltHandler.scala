package security

import be.objectify.deadbolt.scala.{DynamicResourceHandler, DeadboltHandler}
import play.api.mvc._
import be.objectify.deadbolt.core.models.Subject
import models.User
import play.api.i18n.Messages
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class CMSDeadboltHandler(val user: Future[Option[User]]) extends DeadboltHandler {
  def getUser: Option[User] = Await.result(user, 10 seconds)

  def beforeAuthCheck[A](request: Request[A]): Option[Result] = None

  def getSubject[A](request: Request[A]): Option[Subject] = getUser

  def onAuthFailure[A](request: Request[A]): Result =
    Results.Redirect(controllers.routes.Application.index()).flashing("warning" -> Messages("error.authorisation"))

  def getDynamicResourceHandler[A](request: Request[A]): Option[DynamicResourceHandler] = None
}
