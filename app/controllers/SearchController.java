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

import java.util.List;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.JsonNode;

import play.data.Form;
import play.libs.Json;
import play.libs.F.*;
import play.mvc.*;
import espace.core.CommonQuery;
import espace.core.ESpaceSources;
import espace.core.SourceResponse;
import espace.core.Utils;

public class SearchController extends Controller {

	final static Form<CommonQuery> userForm = Form.form(CommonQuery.class);

	public static Result searchslow() {
		System.out.println(request().body());
		JsonNode json = request().body().asJson();
		CommonQuery q = null;
		if (json == null) {
			return badRequest("Expecting Json query");
		} else {
			// Parse the query.
			try {
				q = Utils.parseJson(json);
			} catch (Exception e) {
				return badRequest(e.getMessage());
			}
		}

		return ok(Json.toJson(search(q)));
	}

	public static Promise<Result> search() {
		System.out.println(request().body());
		JsonNode json = request().body().asJson();
		final CommonQuery q;

		if (json == null) {
			return Promise.pure(badRequest("Expecting Json query"));
		} else {
			// Parse the query.
			try {
				q = Utils.parseJson(json);
			} catch (Exception e) {
				return Promise.pure(badRequest(e.getMessage()));
			}
		}

		return Promise.promise(new Function0<JsonNode>() {
			public JsonNode apply() {
				return Json.toJson(search(q));
			}
		}).map(new Function<JsonNode, Result>() {
			public Result apply(JsonNode i) {
				return ok(i);
			}
		});

		// return ok(Json.toJson(search(q)));
	}

	public static List<SourceResponse> search(CommonQuery q) {
		List<SourceResponse> result = ESpaceSources.fillResults(q);
		return result;
	}

	public static Result testsearch() {
		return ok(views.html.testsearch.render(userForm, null));
	}

	public static Result posttestsearch() {
		System.out.println(userForm.bindFromRequest().toString());
		CommonQuery q = userForm.bindFromRequest().get();
		if (q == null || q.searchTerm == null) {
			q = new CommonQuery();
			q.searchTerm = "zeus";
		}
		q.validate();
		List<SourceResponse> res = search(q);
		return ok(views.html.testsearch.render(userForm, res));
	}

}
