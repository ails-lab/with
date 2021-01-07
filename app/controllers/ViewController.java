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


package controllers;

import db.DB;
import play.mvc.Controller;
import play.mvc.Result;

public class ViewController extends Controller {

	public Result swagger() {
		try {
			return redirect("/assets/lib/swagger-ui-dist/index.html?url=" +
					java.net.URLEncoder.encode(DB.getConf().getString("with.api"), "UTF-8") + "/assets/apispecs.json");
		} catch( Exception e ) {
			return badRequest( "Exception in redirect building." );
		}
	}
}

