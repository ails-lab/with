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

import scala.collection.mutable.ArrayBuffer
import db.DB
import play.api.mvc._
import play.api.http.HeaderNames._

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
