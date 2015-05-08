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



/**
 * The AccessFilter should
 *  - check if the session (timestamp, ip and user in session cookie) is expired and update it 
 *  - check the apikey and find if the call is allowed
 *  
 */
class SessionFilter extends Filter {
  val log = Logger(this.getClass())
  val sessionTimeout = { if( DB.getConf().hasPath("session.timeout")) {
      DB.getConf().getLong( "session.timeout")
    } else {
      1000l*86400l*7 // one week
    }
  }
  
  def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader) = {
    var ignoreRequest = false
    if( DB.getConf().hasPath( "session.ignore")) {
      val ignore = DB.getConf().getString( "session.ignore").r.unanchored
      ignore findFirstIn( rh.path ) match {
        case Some(_) => ignoreRequest = true
        case None => ignoreRequest = false
      }
    }
   
    if( ignoreRequest || rh.session.isEmpty  ) {
      next( rh )  
    } else {
      // check ip in cookie with remote address
      // expire session
      val ip = rh.session.get( "sourceIp")
      val sourceCheck = ip match {
        case Some( sourceIp ) if( sourceIp==rh.remoteAddress ) => true
        case Some( _ ) => false
        case None => true
      }
      
      val timeout = rh.session.get( "lastAccessTime")
        .map { x => x.toLong }
        .map { t => (System.currentTimeMillis() > (t + sessionTimeout )) }
       
      (sourceCheck, timeout) match {
        // there was a lastAccessTime in the session
        case (true, Some( false )) =>  next(rh).map{ result =>
            val session = rh.session
            result.withSession(session + ( "lastAccessTime" -> System.currentTimeMillis().toString()))
        }
        // no accessTime in the session
        case ( true, None ) =>  next(rh)
        // timeout, the user is removed from the session, lastAccessTime and sourceIp stay
        // Action can complain about expired session (or not, if no session user is needed)
        case( true, Some( true )) => next(rh).map{ result =>
            val session = rh.session
            result.withSession(session - "user" )
          } 
        case( false, _ ) => Future.successful( Results.BadRequest( "Session invalid, ip don't match" ))
     }

    }  
  }
}
