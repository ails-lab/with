/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package controllers

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import actors.ApiKeyManager
import scala.concurrent.Future
import play.api.libs.concurrent.Akka
import play.api.Play.current
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import model.ApiKey
import play.api.mvc.Controller
import db.DB
import play.api.Logger
import org.bson.types.ObjectId
import scala.collection.JavaConversions._
import play.api.http.HeaderNames._
import scala.collection.mutable.ArrayBuffer
import java.nio.charset.StandardCharsets

/**
 * The AccessFilter should
 *  - check if the session (timestamp, ip and user in session cookie) is expired and update it
 *  - check the apikey and find if the call is allowed
 *
 */
class EffektiveUserFilter extends Filter {
  val log = Logger(this.getClass())

  /**
   * Returns an array of all the user/group ids the given user ids have rights on
   */
  def effectiveUserIds(userId: Option[String], proxyId: Option[String]): Seq[String] = {
    val result = scala.collection.mutable.ArrayBuffer.empty[String]
    
    // maybe valid user and proxy maybe none
    val userOpt = userId.flatMap{ id => Option( DB.getUserDAO.get(new ObjectId(id))) }
    val proxyOpt = proxyId.flatMap{ id => Option( DB.getUserDAO.get(new ObjectId(id))) }
    
    for (user <- userOpt) {
      result.add(user.getDbId().toString())
    }
    
    for (user <- proxyOpt) {
      result.add(user.getDbId().toString())
    }

    for (user <- userOpt) {
    	  val groupIds = user.getUserGroupsIds().map { x => x.toString() }
    	  result.addAll(groupIds)
    }

    for (user <- proxyOpt) {
    	  val groupIds = user.getUserGroupsIds().map { x => x.toString() }
    	  result.addAll(groupIds)
    }
    result.toSeq
  }
  
  /**
   * get user and proxy from session on the way in, add effective user array, and remove it in outgoing sessions
   */
  def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader) = {
		  var ignoreRequest = false
		  if( DB.getConf().hasPath( "session.ignore")) {
			  val ignore = DB.getConf().getString( "session.ignore").r.unanchored
					  ignore findFirstIn( rh.path ) match {
					  case Some(_) => ignoreRequest = true
					  case None => ignoreRequest = false
			  }
		  }

		  if( ignoreRequest ) next( rh )
		  else {
			  val userId = rh.session.get("user")
					  val proxyId = rh.session.get("proxy")

					  val userIds = effectiveUserIds(userId, proxyId).mkString(",")
					  val sessionData = rh.session + (("effectiveUserIds", userIds))
					  val newRh = FilterUtils.withSession(rh, sessionData.data)

					  next(newRh).map { result =>
					  FilterUtils.outsession(result) match {
					  case Some(session) => result.withSession(Session(session) - ("effectiveUserIds"))
					  case None => result
					  }
			  }
		  }
  }
}
