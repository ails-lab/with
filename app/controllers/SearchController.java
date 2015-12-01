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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import play.Logger;
import play.Logger.ALogger;
import play.data.Form;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessManager;
import utils.ListUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import elastic.Elastic;
import espace.core.CommonFilterLogic;
import espace.core.CommonFilterResponse;
import espace.core.CommonQuery;
import espace.core.ESpaceSources;
import espace.core.FiltersHelper;
import espace.core.ISpaceSource;
import espace.core.ParallelAPICall;
import espace.core.SearchResponse;
import espace.core.SourceResponse;
import espace.core.Utils;
import espace.core.sources.DPLASpaceSource;
import espace.core.sources.DigitalNZSpaceSource;
import espace.core.sources.EuropeanaFashionSpaceSource;
import espace.core.sources.EuropeanaSpaceSource;

public class SearchController extends Controller {

	final static Form<CommonQuery> userForm = Form.form(CommonQuery.class);
	public static final ALogger log = Logger.of(SearchController.class);

	public static Result searchslow() {
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
		if (log.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder();
			for (Map.Entry<String, String> e : session().entrySet()) {
				sb.append(e.getKey() + " = " + "'" + e.getValue() + "'\n");
			}
			log.debug(sb.toString());
		}

		if (json == null) {
			return Promise.pure((Result) badRequest("Expecting Json query"));
		} else {
			// Parse the query.
			try {
				final CommonQuery q = Utils.parseJson(json);
				if (session().containsKey("effectiveUserIds")) {
					List<String> userIds = AccessManager.effectiveUserIds(session().get("effectiveUserIds"));
					q.setEffectiveUserIds(userIds);
				}
				Iterable<Promise<SourceResponse>> promises = callSources(q);
				// compose all futures, blocks until all futures finish
				return ParallelAPICall.<SourceResponse> combineResponses(r -> {
					Logger.info(r.source + " found " + r.count);
					return true;
				} , promises);
			} catch (Exception e) {
				e.printStackTrace();
				return Promise.pure((Result) badRequest(e.getMessage()));
			}
		}
	}

	public static Promise<Result> searchwithfilter() {
		JsonNode json = request().body().asJson();

		if (json == null) {
			return Promise.pure((Result) badRequest("Expecting Json query"));
		} else {
			// Parse the query.
			try {
				final CommonQuery q = Utils.parseJson(json);
				if (session().containsKey("effectiveUserIds")) {
					List<String> userIds = AccessManager.effectiveUserIds(session().get("effectiveUserIds"));
					q.setEffectiveUserIds(userIds);
				}
				Iterable<Promise<SourceResponse>> promises = callSources(q);
				// compose all futures, blocks until all futures finish

				Function<CommonFilterLogic, CommonFilterResponse> f = (CommonFilterLogic o) -> {
					return o.export();
				};

				Promise<List<SourceResponse>> promisesSequence = Promise.sequence(promises);
				return promisesSequence.map(new play.libs.F.Function<Collection<SourceResponse>, Result>() {
					List<SourceResponse> finalResponses = new ArrayList<SourceResponse>();

					public Result apply(Collection<SourceResponse> responses) {
						finalResponses.addAll(responses);
						// Logger.debug("Total time for all sources to respond:
						// "
						// + (System.currentTimeMillis()- initTime));

						SearchResponse r1 = new SearchResponse();
						ArrayList<CommonFilterLogic> merge = new ArrayList<CommonFilterLogic>();
						for (SourceResponse sourceResponse : finalResponses) {
							// System.out.println(sourceResponse.filtersLogic);
							FiltersHelper.merge(merge, sourceResponse.filtersLogic);
							sourceResponse.filters = ListUtils.transform(sourceResponse.filtersLogic, f);
						}

						r1.filters = ListUtils.transform(merge, f);
						r1.responces = mergeResponses(finalResponses);

						return ok(Json.toJson(r1));
					}

					private List<SourceResponse> mergeResponses(List<SourceResponse> finalResponses2) {
						List<SourceResponse> res = new ArrayList<>();
						for (SourceResponse r : finalResponses2) {
							boolean merged = false;
							for (SourceResponse r2 : res) {
								if (r2.source.equals(r.source)) {
									// merge these 2 and replace r.
									res.remove(r2);
									res.add(r.merge(r2));
									merged = true;
									break;
								}
							}
							if (!merged) {
								res.add(r);
							}
						}
						return res;
					}

				});

			} catch (Exception e) {
				e.printStackTrace();
				return Promise.pure((Result) badRequest(e.getMessage()));
			}
		}
	}

	public static List<String> getTheSources() {
		Function<ISpaceSource, String> function = (ISpaceSource x) -> {
			return x.getSourceName();
		};
		return ListUtils.transform(ESpaceSources.getESources(), function);
	}

	private static Iterable<Promise<SourceResponse>> callSources(final CommonQuery q) {
		List<Promise<SourceResponse>> promises = new ArrayList<Promise<SourceResponse>>();
		BiFunction<ISpaceSource, CommonQuery, SourceResponse> methodQuery = (ISpaceSource src, CommonQuery cq) -> src
				.getResults(cq);
		for (final ISpaceSource src : ESpaceSources.getESources()) {
			if ((q.source == null) || (q.source.size() == 0) || q.source.contains(src.getSourceName())) {
				List<CommonQuery> list = src.splitFilters(q);
				for (CommonQuery commonQuery : list) {
					promises.add(ParallelAPICall.<ISpaceSource, CommonQuery, SourceResponse> createPromise(methodQuery,
							src, commonQuery));
				}
			}
		}
		return promises;
	}

	public static Promise<Result> searchWithThreads() {
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
				Logger.debug("Total time for all sources to respond: " + (System.currentTimeMillis() - initTime));
				return ok(i);
			}
		});
	}

	public static List<SourceResponse> search(CommonQuery q) {
		List<SourceResponse> result = ESpaceSources.fillResults(q);
		return result;
	}

	public static Result testsearch() {
		return buildresult(new CommonQuery("Zeus"));
	}

	private static Result buildresult(CommonQuery q) {
		// q.source = Arrays.asList(DigitalNZSpaceSource.LABEL);
		List<SourceResponse> res = search(q);
		SearchResponse r1 = new SearchResponse();
		r1.responces = res;
		ArrayList<CommonFilterLogic> merge = new ArrayList<CommonFilterLogic>();
		for (SourceResponse sourceResponse : res) {
			// System.out.println(sourceResponse.source + " Filters: " +
			// sourceResponse.filters);
			FiltersHelper.merge(merge, sourceResponse.filtersLogic);
		}
		Function<CommonFilterLogic, CommonFilterResponse> f = (CommonFilterLogic o) -> {
			return o.export();
		};
		List<CommonFilterResponse> merge1 = ListUtils.transform(merge, f);
		// System.out.println(" Merged Filters: " + merge1);

		return ok(views.html.testsearch.render(userForm, res, merge1));
	}

	public static Result reindex() {
		Promise.promise(new Function0<String>() {
			public String apply() throws Exception {
				log.info("Reindex started");
				Elastic.reindex();
				log.info("Reindex finished");
				return "ok";
			}
		});
		return (ok());

	}

	public static Result reindex_records() {
		Promise.promise(new Function0<String>() {
			public String apply() throws Exception {
				log.info("Reindex started");
				Elastic.reindex_records();
				log.info("Reindex finished");
				return "ok";
			}
		});
		return (ok());

	}
}
