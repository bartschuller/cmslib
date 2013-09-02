package security

import be.objectify.deadbolt.scala.{DeadboltHandler, DynamicResourceHandler}
import play.api.mvc.Request
import be.objectify.deadbolt.core.DeadboltAnalyzer

class CMSDynamicResourceHandler extends DynamicResourceHandler {
  def isAllowed[A](name: String, meta: String, deadboltHandler: DeadboltHandler, request: Request[A]): Boolean = {
    name match {
      case "ownProfile" =>
        val subject = deadboltHandler.getSubject(request)
        if (subject.isEmpty)
          false
        else if (DeadboltAnalyzer.hasRole(subject.get, "admin"))
          true
        else
          meta == subject.get.getIdentifier
    }
  }

  def checkPermission[A](permissionValue: String, deadboltHandler: DeadboltHandler, request: Request[A]): Boolean = ???
}
