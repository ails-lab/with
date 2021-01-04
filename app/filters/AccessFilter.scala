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
import org.bson.types.ObjectId
import scala.collection.JavaConversions._
import play.api.http.HeaderNames._
import scala.collection.mutable.ArrayBuffer
import java.nio.charset.StandardCharsets
import com.google.inject.Inject
import akka.actor.ActorSystem

/**
 * The AccessFilter should
 *  - check if the session (timestamp, ip and user in session cookie) is expired and update it
 *  - check the apikey and find if the call is allowed
 *
 */
class AccessFilter @Inject()(actorSystem:ActorSystem,  sessionCookieBaker:SessionCookieBaker) extends Filter {
  val log = Logger(this.getClass())

  
   def apiKeyCheck(next: (RequestHeader) => Future[Result], rh: RequestHeader): Future[Result] = {
    implicit val timeout = new Timeout(1000.milliseconds)

    /**
     * Exor the right bits and return the apiKey string
     */
    
    def findApikey( time:String, auth:String, origin:String, params:String ): String = {
      log.debug( "Time: "+ time + " Auth:" + auth + " Origin:" + origin + " Params:" + params )
    	val authBytes = auth.grouped(2).map( Integer.parseInt( _, 16 ).byteValue()).toVector
      // we either process origin or params
      val someBytes = if( !origin.isEmpty()) 
          origin.getBytes(StandardCharsets.UTF_8)
       else
         params.getBytes(StandardCharsets.UTF_8)
         
      val apiVec = ArrayBuffer[Byte]()
      val max = if( someBytes.length > authBytes.length) someBytes.length else authBytes.length
      for( i <- 0 until max ) {
        if( i < authBytes.length )
          apiVec.add(( authBytes(i%authBytes.length) ^ someBytes(i%someBytes.length)).byteValue())
        else {
          apiVec(i%authBytes.length ) = ( apiVec(i%authBytes.length ) ^  someBytes(i%someBytes.length)).byteValue()
          log.debug( "Unexpected ")
        }
      } 
      
      if(! time.isEmpty() ) {
          // in case there is a timestamp we use this method
            var timeI = time.toLong
            val timeV = ArrayBuffer[Byte]()
            for( i <- 0 to 3 ) {
              val timeByte = ( timeI & 255 ).byteValue()
              timeV.add(timeByte)
              timeI = timeI >> 8;
            }   
            for( i <- 0 until authBytes.length) {
              apiVec(i) = (apiVec(i) ^ timeV(i%4)).byteValue()
            }
      } 
        
    	new String(apiVec.toArray, "UTF8" )
    }
       
    /**
     * If there is an api key in the headers, extract it.
     */
    def fromAuth( headers: Headers ): Option[String] = {
      // build the origin from referer      
      val origin = rh.headers.get( "Referer" ).flatMap( ".*://[^/]*".r.findFirstIn(_))
      // the origin header we should evaluate when we have other webapps using WITH backend
      //  val origin = rh.headers.get( "Origin" )
      log.debug( "Origin: " + origin )
      ( headers.get( "X-auth1"), headers.get( "X-auth2"), origin ) match {
        case (Some( time ), Some(auth), Some( ref )) => {
          // check the time is within the same minute ( ignore the hour/ god know what timezone they send )
          Some( findApikey( time, auth, ref, "" ))
        }
        case _ => { 
            rh.getQueryString("Xauth2") match {
              case Some( authParam ) => {
                val strippedParams = "Xauth2=[0-9A-F]*".r.replaceFirstIn(rh.path+rh.rawQueryString,"") 
                Some( findApikey( "", authParam,"", strippedParams ))
              }
              case _ => None
            }
        }
      }
    }
    
    if (log.isDebugEnabled) {
      log.debug("PATH: " + rh.path)
      if (!rh.session.isEmpty) {
        val ses = for (entry <- rh.session.data) yield entry._1 + ": " + entry._2
        log.debug("Session: " + ses.mkString("", "\n   ", "\n"))
      }
    } 
    
    // mostly this deals with apikeys that come plain with the request
    val access = new ApiKeyManager.Access
    access.call = rh.path
    access.apikey = rh.queryString.get("apikey") match {
    case Some(Seq(key, _*)) => key
    case _ => 
      rh.session.get("apikey")
        .orElse(rh.headers.get("X-apikey"))
        // or from X-auth1,X-auth2 header, Xauth2
        .orElse(fromAuth( rh.headers ))
        .getOrElse( null )
    }

    val apiActor = actorSystem.actorSelection("user/apiKeyManager");
    
    // 4 options . Allowed call, Allowed with proxy, Access not allowed, failed request for some reason
    (apiActor ? access).flatMap {
      response =>
        response match {
          case proxy: ObjectId => {
        	  val sessionData = rh.session + (("proxy", proxy.toString()))
            val newRh = FilterUtils.withSession(rh, sessionData, sessionCookieBaker)
          
            next(newRh).map { result =>
              result.withSession(Session(FilterUtils.outsession(result)) - ("proxy"))
            }
          }
          case ApiKey.Response.ALLOWED => next( rh )
          case r: ApiKey.Response => Future.successful(Results.BadRequest(r.toString()))
          case _ => Future.successful(Results.Forbidden)
        }
    }
  }

  def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader) = {
    if (DB.getConf().hasPath("apikey.ignorePattern")) {
      val pattern = DB.getConf().getString("apikey.ignorePattern").r.unanchored
      (pattern findFirstIn rh.path) match {
        case Some(_) => next(rh)
        case None => apiKeyCheck(next, rh)
      }
    } else {
      apiKeyCheck(next, rh)
    }
  }
}

/**
function sign( param, time ) {
  function ta( n ) {
    var r = new Array();
    for( var i=0; i< 4; i++ ) {
      r.push( n%256);
      n = n >>> 8;
    }
    return r;
  }
  function reHex( n ) {
    var res = "";
    for( var i=0; i<n.length; i++ ) {
           if( n[i] >15 ) { 
             res += n[i].toString( 16 ).toUpperCase();
           } else {
             res += "0"+ n[i].toString( 16 ).toUpperCase();
           }
    }
    return res;
  }

  var r = new Array();
  var n = %s;
  var max = Math.max( n.length, param.length )

  for( var i=0; i<max; i++ ) {
    if( i < n.length ) {   
       r[i] = (n[i%n.length]+1)^(param.charCodeAt(i%param.length)&255);
    } else {
      r[i%n.length] = r[i%n.length] ^ (param.charCodeAt(i%param.length)&255)      
    }
  }

  if( time != undefined ) {
    var a2 = ta( time );
    for( var i=0; i<r.length; i++ ) {
       r[i] = r[i] ^ (a2[i%4]);
    } 
  }
  return reHex( r );
} 

$.ajaxSetup({
    beforeSend: function(xhr, obj) {
        var utc = new Date().valueOf();
        xhr.setRequestHeader('X-auth1', utc );
        xhr.setRequestHeader('X-auth2', sign( document.location.origin, utc ));
    }
});
*/

