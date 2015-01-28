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

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import sun.security.pkcs.ParsingException;
import views.html.index;

import com.fasterxml.jackson.databind.JsonNode;

import espace.core.CommonQuery;
import espace.core.ESpaceSources;
import espace.core.Utils;

public class Application extends Controller {

	public static Result index() {
		return ok(index.render("Your new application is ready."));
	}

	public static Result search() {
		JsonNode json = request().body().asJson();
		CommonQuery q = new CommonQuery();
		if (json == null) {
			return badRequest("Expecting Json query");
		} else {
			// Parse the query.
			try {
				Utils.parseJson(json, q);
			} catch (ParsingException e) {
				return badRequest(e.getMessage());
			}
		}
		return search(q);
	}

	public static Result search(CommonQuery q) {
		Object result = ESpaceSources.fillResults(q);
		return ok(Json.toJson(result));
	}

	public static Result testsearch() {
		CommonQuery q = new CommonQuery();
		q.query = "Zeus";
		return search(q);
	}

}
