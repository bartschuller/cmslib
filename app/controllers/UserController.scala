package controllers

import play.api.mvc._
import models.User
import org.mindrot.jbcrypt.BCrypt
import reactivemongo.api.collections.default.BSONCollection
import play.api.Logger
import reactivemongo.bson.{BSONDocument, BSONObjectID}

object UserController extends Controller with CMSController {
  val collection = db[BSONCollection]("users")

  def createForm = withDeadbolt { implicit deadbolt => implicit request =>
    Ok(views.html.user(None, User.form))
  }

  def signup = withDeadbolt { implicit deadbolt => implicit request =>
    User.form.bindFromRequest.fold(
      formWithErrors => Ok(views.html.user(None, formWithErrors)),
      userNoCrypt => {
        val user = userNoCrypt.copy(password = BCrypt.hashpw(userNoCrypt.password, BCrypt.gensalt))
        AsyncResult {
          collection.insert(user).map(lastError =>
            if (lastError.ok)
              Redirect(routes.Application.index).flashing("success" -> "User successfully created.")
            else {
              Logger.error(s"MongoDB error saving User ${user.name}: ${lastError.errMsg.getOrElse("unknown")}")
              Redirect(routes.Application.index).flashing("error" -> "Error saving User.")
          })
        }
      }
    )
  }

  def editForm(id: String) = withDeadbolt { implicit deadbolt => implicit request =>
    Async {
      collection.find(BSONDocument("_id" -> new BSONObjectID(id))).one[User].map( ou =>
        ou.fold[Result](NotFound)(user => Ok(views.html.user(Some(id), User.form.fill(user))))
      )
    }
  }

  def edit(id: String) = hasAllRoles("admin") { implicit deadbolt => implicit request =>
    User.form.bindFromRequest.fold(
      formWithErrors => Ok(views.html.user(Some(id), formWithErrors)),
      userNoCrypt => {
        val user = userNoCrypt.copy(password = BCrypt.hashpw(userNoCrypt.password, BCrypt.gensalt))
        AsyncResult {
          collection.update(BSONDocument("_id" -> new BSONObjectID(id)), user).map(lastError =>
            if (lastError.ok)
              Redirect(routes.Application.index).flashing("success" -> "User successfully updated.")
            else {
              Logger.error(s"MongoDB error saving User ${user.name}: ${lastError.errMsg.getOrElse("unknown")}")
              Redirect(routes.Application.index).flashing("error" -> "Error saving User.")
            })
        }
      }
    )
  }
}
