package controllers

import play.modules.reactivemongo.MongoController
import be.objectify.deadbolt.scala.DeadboltActions
import play.api.mvc._
import security.CMSDeadboltHandler
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{BSONObjectID, BSONDocument}
import models.User
import scala.concurrent.Future
import be.objectify.deadbolt.core.DeadboltAnalyzer

trait CMSController extends MongoController with DeadboltActions { self: Controller =>
  private val userCollection = db[BSONCollection]("users")

  def withDeadbolt(f: CMSDeadboltHandler => Request[AnyContent] => Result): Action[AnyContent] = {
    Action { request =>

      val userId = request.session.get("userId")
      val account = userId.fold[Future[Option[User]]](Future(None))(
        id => userCollection.find(BSONDocument("_id" -> new BSONObjectID(id))).one[User]
      )

      val handler = new CMSDeadboltHandler(account)
      f(handler)(request)
    }
  }

  def hasAllRoles(roles: String*)(f: CMSDeadboltHandler => Request[AnyContent] => Result): Action[AnyContent] =
    withDeadbolt { deadbolt => request =>
      deadbolt.beforeAuthCheck(request).fold {
        (for {
          subject <- deadbolt.getUser
          if DeadboltAnalyzer.hasAllRoles(subject, roles.toArray)
          result = f(deadbolt)(request)
        } yield result).getOrElse(deadbolt.onAuthFailure(request))
      } (identity)
    }
}
