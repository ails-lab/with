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
import play.api.Logger
import play.api.mvc.Controller
import play.api.mvc.Action

object CustomAssets extends Controller {
   val log = Logger(this.getClass())

   def getFile( customName:String, path:String ) = {
      val customDir = new File(  new File( Play.application().path(), "custom" ), customName )
      val filePath = new File( customDir, path )
      log.info(customName + " " + path+ " " + filePath.getAbsolutePath())
      
      if( path == "headers.js" )  FilterUtils.withAjaxScript
      else {
    	  if( filePath.canRead())
    		  Action { Ok.sendFile( filePath, true ) }
        else
    		  Assets.at( "/public", path )
      }
  }
   
   def redirect( customName:String ) = Action { 
     Redirect( "/custom/"+customName+"/index.html")
   }
}