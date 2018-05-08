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

import java.io.File
import play._
import play.api.mvc._
import play.api.mvc.Controller
import play.api.Logger
import play.api.mvc.Action
import _root_.filters.FilterUtils.withAjaxScript
import scala.concurrent.Future
import play.api.mvc.Results.Redirect
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
 

object CustomAssets {
   val log = Logger(this.getClass())

   def getFile( customName:String, path:String ):Action[AnyContent] = {
      if( path == "headers.js" )  withAjaxScript
      else {
        val customPath = "custom/"+customName+"/"+path;
        val basePath = "custom/base/" + path;

        Action.async { implicit request =>
            val resultF1 = Assets.at( "/public", customPath )(request)
            resultF1.flatMap {
                customRes:Result =>
                  customRes.header.status match {
                  case 200|304 => {
                    log.debug( "Direct custom hit at " + customPath )
                    Future.successful( customRes )
                  }
                  case _ => {
                     val resultF2 = Assets.at( "/public", basePath )(request)
                     resultF2.flatMap{ 
                       baseRes:Result => 
                         baseRes.header.status match {
                         case 200|304 => {
                           log.debug( "Hit at base " + basePath )
                           Future.successful( baseRes )
                         }
                         case _ => {
                            log.debug( "Fallback to public " + path )
                           Assets.at( "/public", path )(request)
                         }
                       }
                     }
                  }
                }
            }
        }
     }
   }

   def redirect( customName:String ) = Action { 
     Redirect( "/custom/"+customName+"/index.html")
   }
}