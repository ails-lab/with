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


package filters

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
import play.api.mvc.Headers
import play.api.mvc.Headers

import play.libs.Crypto;
import play.libs.Json;
import controllers.UserManager



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
  
  def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader):Future[Result] = {
    var ignoreRequest = false
    val bearerRegexp = "Bearer (.*)".r
    rh.headers.get( "Authorization" ) match {
      case Some( bearerRegexp( token ))  => {
        val optUser = UserManager.useridFromToken( token )
        if( optUser.isPresent()) {
          val newRh = FilterUtils.withSession( rh, Map(("user", optUser.get())))
          return (next( newRh ).map { result => 
            result.withSession( Session())
          } )
        }
      } 

    } 
      
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
      // expire session
    	  val timeout = rh.session.get( "lastAccessTime")
    			  .map { x => x.toLong }
    	      .map { t => (System.currentTimeMillis() > (t + sessionTimeout )) }

        timeout match {
           // no accessTime in the session
           case None => next(rh)
           
           // timeout, remove user from incoming session
           case Some( true ) => {
        	   val sessionData = rh.session - ("user") + ("lastAccessTime" -> System.currentTimeMillis().toString())
        		 val newRh = FilterUtils.withSession( rh, sessionData.data )
        		 next(newRh)
           }
           
           // no timeout, update the lastAccessTime in the cookie
           case Some( false ) => {
              next( rh ).map { result => 
                FilterUtils.outsession(result) match {
                  case Some( session ) => result.withSession( Session(session) + ("lastAccessTime" -> System.currentTimeMillis().toString()))
                  case None => result.withSession( rh.session + ("lastAccessTime" -> System.currentTimeMillis().toString()))
                }
              } 
           }
         }
     }  
  }
}
