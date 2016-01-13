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
class AccessFilter extends Filter {
  val log = Logger(this.getClass())

  def effectiveUserIds(userId: Option[String], proxyId: Option[String]): Seq[String] = {
    val result = scala.collection.mutable.ArrayBuffer.empty[String]
    for (id <- userId) {
      result.add(id)
    }
    for (proxy <- proxyId) {
      result.add(proxy)
    }
    for (id <- userId) {
      val user = DB.getUserDAO.get(new ObjectId(id))
      val groupIds = user.getUserGroupsIds().map { x => x.toString() }
      result.addAll(groupIds)
    }
    for (proxy <- proxyId) {
      val user = DB.getUserDAO.get(new ObjectId(proxy))
      val groupIds = user.getUserGroupsIds().map { x => x.toString() }
      result.addAll(groupIds)
    }
    result.toSeq
  }

  def apiKeyCheck(next: (RequestHeader) => Future[Result], rh: RequestHeader): Future[Result] = {
    implicit val timeout = new Timeout(1000.milliseconds)
    val access = new ApiKeyManager.Access

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
                val strippedParams = "Xauth2=[0-9A-F]*".r.replaceFirstIn(rh.rawQueryString,"") 
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

    rh.queryString.get("apikey") match {
      case Some(Seq(key, _*)) => access.apikey = key
      case _ => {
        rh.session.get("apikey") match {
          case Some(key) => access.apikey = key
          case None => { 
            rh.headers.get("X-apikey") match {
              case Some(key) => access.apikey = key
              case None => {
                fromAuth( rh.headers ) match {
                  case Some(apikey) => access.apikey = apikey
//                  case None => access.ip = rh.remoteAddress 
                  case None => 
		    log.info( "No KEY! ")
		    access.apikey = "empty"
		    
                  
                }
              }  
            }
          }
        }
      }
    }

    val userId = rh.session.get("user")
    access.call = rh.path
    val apiActor = Akka.system.actorSelection("user/apiKeyManager");
    (apiActor ? access).flatMap {
      response =>
        response match {
          case o: ObjectId => {
            val userIds = effectiveUserIds(userId, Some(o.toString())).mkString(",")
            val sessionData = rh.session + (("effectiveUserIds", userIds))
            val newRh = FilterUtils.withSession(rh, sessionData.data)
            log.debug("EffectiveUserIds: " + userIds)
          
            next(newRh).map { result =>
              FilterUtils.outsession(result) match {
                case Some(session) => result.withSession(Session(session) - ("effectiveUserIds"))
                case None => result
              }
            }
          }
          case ApiKey.Response.ALLOWED => {
            val userIds = effectiveUserIds(userId, None).mkString(",")
            val sessionData = rh.session + (("effectiveUserIds", userIds ))
            log.debug("EffectiveUserIds: " + userIds)

            val newRh = FilterUtils.withSession(rh, sessionData.data)
            next(newRh).map { result =>
              FilterUtils.outsession(result) match {
                case Some(session) => result.withSession(Session(session) - ("effectiveUserIds"))
                case None => result
              }
            }
          }
          case r: ApiKey.Response => Future.successful(Results.BadRequest(r.toString()))
          case _ => Future.successful(Results.Forbidden)
        }
    }
  }

  def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader) = {
	//the author of this code struggled with scala
	var s = false
	if (DB.getConf().hasPath("apikey.ignorePattern2")){
      		val pattern = DB.getConf().getString("apikey.ignorePattern2").r.unanchored
      		(pattern findFirstIn rh.path) match {
        		case Some(_) => s=true
        		case None =>
      		}
	}

	if (DB.getConf().hasPath("apikey.ignorePattern")){
      		val pattern2 = DB.getConf().getString("apikey.ignorePattern").r.unanchored
      		(pattern2 findFirstIn rh.path) match {
        		case Some(_) => s=true
        		case None => 
      		}
	} 
	
	if(s){
		next(rh)
	}else{
		apiKeyCheck(next, rh)
	}
  }
}

object FilterUtils {
  /**
   * Add a new session to the requestHeader, make a new one and return it.
   */
  def withSession(rh: RequestHeader, sessionData: Map[String, String]): RequestHeader = {
    // make a new session cookie
    val newCookie = Session.encode(sessionData)

    // replace cookie in header
    val oldHeaders = scala.collection.mutable.Map.empty ++ rh.headers.toMap
    val cookies = ArrayBuffer.empty[Cookie]
    for (headerlines <- oldHeaders.get(COOKIE); header <- headerlines) {
      val headerCookies = Cookies.decode(header)
      for (cookie <- headerCookies) {
        cookies += cookie
      }
    }
    // remove the old cookies
    oldHeaders -= COOKIE
    val newCookies = cookies.map { c =>
      if (c.name == Session.COOKIE_NAME) {
        new Cookie(c.name, newCookie, c.maxAge, c.path, c.domain, c.secure, c.httpOnly)
      } else c
    }
    oldHeaders += ((COOKIE, Seq(Cookies.encode(newCookies))))

    rh.copy(headers = new Headers { val data: Seq[(String, Seq[String])] = (oldHeaders.toSeq) })
  }

  def outsession(result: Result): Option[Map[String, String]] = {
    Cookies(result.header.headers.get(SET_COOKIE))
      .get(Session.COOKIE_NAME).map(_.value).map(Session.decode)
  }
  
  def withAjaxScript =  Action {
    val apikey = DB.getApiKeyDAO.getByName("WITH")
    if( apikey != null ) {
      val arr = apikey.getKeyString().getBytes("UTF8").map( _ -1 )
      val script = """
function sign(c,f){function e(a){for(var c=[],b=0;4>b;b++)c.push(a%256),a>>>=8;return c}for(var d=[],b=%s,g=Math.max(b.length,c.length),a=0;a<g;a++)a<b.length?d[a]=b[a%b.length]+1^c.charCodeAt(a%c.length)&255:d[a%b.length]^=c.charCodeAt(a%c.length)&255;if(void 0!=f)for(b=e(f),a=0;a<d.length;a++)d[a]^=b[a%4];return function(a){for(var c="",b=0;b<a.length;b++)c=15<a[b]?c+a[b].toString(16).toUpperCase():c+("0"+a[b].toString(16).toUpperCase());return c}(d)}
$.ajaxSetup({beforeSend:function(c,f){var e=(new Date).valueOf();c.setRequestHeader("X-auth1",e);c.setRequestHeader("X-auth2",sign(document.location.origin,e))}});""".replace("%s", "[" + arr.mkString( "," ) + "]" )
       play.api.mvc.Results.Ok( script ).as("application/javascript")      
    } else {
             play.api.mvc.Results.BadRequest( "Bad ApiKey" ).as("application/javascript")      
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
