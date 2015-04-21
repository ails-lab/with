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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import com.fasterxml.jackson.databind.JsonNode;

import play.Logger;
import play.data.Form;
import play.libs.F.Function0;
import play.libs.Json;
import play.libs.F.Promise;
import play.mvc.*;
import espace.core.CommonQuery;
import espace.core.ESpaceSources;
import espace.core.ISpaceSource;
import espace.core.ParallelAPICall;
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
	
	// here is how the ApiKey check can be build into the controllers
	// @With( CallAllowedCheck.class)
	@SuppressWarnings("unchecked")
	public static Promise<Result> search() {
		JsonNode json = request().body().asJson();

		if (json == null) {
			return Promise.pure((Result) badRequest("Expecting Json query"));
		} else {
			// Parse the query.
			try {
				final CommonQuery q = Utils.parseJson(json);
				Iterable<Promise<SourceResponse>> promises = new ArrayList<Promise<SourceResponse>>();
				//final long initTime = System.currentTimeMillis();
				BiFunction<ISpaceSource, CommonQuery, SourceResponse> methodQuery = 
						(ISpaceSource src, CommonQuery cq)-> src.getResults(cq);
				for (final ISpaceSource src : ESpaceSources.getESources()) {
					if (q.source == null || q.source.size() == 0 || q.source.contains(src.getSourceName())) {
						((List<Promise<SourceResponse>>) promises).add(
							ParallelAPICall.<ISpaceSource, CommonQuery, SourceResponse>createPromise(methodQuery, src, q));			
					}
				}
				//compose all futures, blocks until all futures finish
				return ParallelAPICall.<SourceResponse>combineResponses(
						r -> {Logger.info(r.source + " found " + r.count); return true;}, promises);		
			} catch (Exception e) {
				return Promise.pure((Result) badRequest(e.getMessage()));
			}
		}
	}

	public static Promise<Result> searchWithThreads() {
		System.out.println(request().body());
		JsonNode json = request().body().asJson();
		final CommonQuery q;

		if (json == null) {
			return Promise.pure((Result) badRequest("Expecting Json query"));
		} else {
			// Parse the query.
			try {
				q = Utils.parseJson(json);
			} catch (Exception e) {
				return Promise.pure((Result) badRequest(e.getMessage()));
			}
		}
		final long initTime = System.currentTimeMillis();
		return Promise.promise(new Function0<JsonNode>() {
			public JsonNode apply() {
				return Json.toJson(search(q));
			}
		}).map(new play.libs.F.Function<JsonNode, Result>() {
			public Result apply(JsonNode i) {
				Logger.debug("Total time for all sources to respond: " + (System.currentTimeMillis()-initTime));
				return ok(i);
			}
		});
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
