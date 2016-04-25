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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.elasticsearch.action.suggest.SuggestResponse;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import elastic.Elastic;
import elastic.ElasticSearcher;
import elastic.ElasticSearcher.SearchOptions;
import elastic.ElasticUtils;
import model.basicDataTypes.ProvenanceInfo.Sources;
import play.Logger;
import play.Logger.ALogger;
import play.data.Form;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import sources.core.CommonFilterLogic;
import sources.core.CommonFilterResponse;
import sources.core.CommonQuery;
import sources.core.ESpaceSources;
import sources.core.FiltersHelper;
import sources.core.ISpaceSource;
import sources.core.ParallelAPICall;
import sources.core.SearchResponse;
import sources.core.SourceResponse;
import sources.core.Utils;
import utils.AccessManager;
import utils.ListUtils;

public class SearchController extends Controller {

	final static Form<CommonQuery> userForm = Form.form(CommonQuery.class);
	public static final ALogger log = Logger.of(SearchController.class);

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
				q.setTypes(Elastic.allTypes);
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
	
	public static Result searchSources() {
		List<Sources> res = new ArrayList<>();
		for (final ISpaceSource src : ESpaceSources.getESources()) {
			Sources sourceByID = Sources.getSourceByID(src.LABEL);
			if (sourceByID!=null && !sourceByID.equals(Sources.Europeana))
				res.add(sourceByID);
		}
		return ok(Json.toJson(res));
	}

	public static Promise<Result> searchwithfilter() {
		JsonNode json = request().body().asJson();
		if (json == null) {
			return Promise.pure((Result)badRequest("Expecting Json query"));
		} else {
			// Parse the query.
			try {
				final CommonQuery q = Utils.parseJson(json);
				q.setTypes(Elastic.allTypes);
				if (session().containsKey("effectiveUserIds")) {
					List<String> userIds = AccessManager.effectiveUserIds(session().get("effectiveUserIds"));
					q.setEffectiveUserIds(userIds);
				}
				Promise<SearchResponse> myResults = getMyResutlsPromise(q);
				play.libs.F.Function<SearchResponse, Result> function = 
				new play.libs.F.Function<SearchResponse, Result>() {
				  public Result apply(SearchResponse r) {
				    return ok(Json.toJson(r));
				  } 
				};
				return myResults.map(function);

			} catch (Exception e) {
				e.printStackTrace();
				return Promise.pure((Result)badRequest(e.getMessage()));
			}
		}
	}
	
	public static Promise<Result> searchwithfilterGET(CommonQuery q) {
//		Form<CommonQuery> qf = Form.form(CommonQuery.class).bindFromRequest();
//		CommonQuery q = qf.get();
		// Parse the query.
		try {
			q.setTypes(Elastic.allTypes);
			if (session().containsKey("effectiveUserIds")) {
				List<String> userIds = AccessManager.effectiveUserIds(session().get("effectiveUserIds"));
				q.setEffectiveUserIds(userIds);
			}
			Promise<SearchResponse> myResults = getMyResutlsPromise(q);
			play.libs.F.Function<SearchResponse, Result> function = new play.libs.F.Function<SearchResponse, Result>() {
				public Result apply(SearchResponse r) {
					return ok(Json.toJson(r));
				}
			};
			return myResults.map(function);

		} catch (Exception e) {
			e.printStackTrace();
			return Promise.pure((Result) badRequest(e.getMessage()));
		}
	}
	
	public static Promise<Result> getfilters() {
		JsonNode json = request().body().asJson();
		if (json == null) {
			return Promise.pure((Result)badRequest("Expecting Json query"));
		} else {
			// Parse the query.
			try {
				final CommonQuery q = Utils.parseJson(json);
				q.searchTerm=null;
				q.setTypes(Elastic.allTypes);
				if (session().containsKey("effectiveUserIds")) {
					List<String> userIds = AccessManager.effectiveUserIds(session().get("effectiveUserIds"));
					q.setEffectiveUserIds(userIds);
				}
				Promise<SearchResponse> myResults = getMyResutlsPromise(q);
				play.libs.F.Function<SearchResponse, Result> function = 
				new play.libs.F.Function<SearchResponse, Result>() {
				  public Result apply(SearchResponse r) {
				    return ok(Json.toJson(r.filters));
				  } 
				};
				return myResults.map(function);

			} catch (Exception e) {
				e.printStackTrace();
				return Promise.pure((Result)badRequest(e.getMessage()));
			}
		}
	}
	
	public static Result mergeFilters(){
		ArrayList<CommonFilterLogic> merge = new ArrayList<CommonFilterLogic>();
		JsonNode json = request().body().asJson(); 
		Collection<Collection<CommonFilterResponse>> filters = new ArrayList<>();
		ObjectMapper m = new ObjectMapper();
		try {
			filters = m.readValue(json.toString(), 
					new TypeReference<Collection<Collection<CommonFilterResponse>>>() {
			        }
					);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (Collection<CommonFilterResponse> fresponse : filters) {
			if (fresponse!=null){
				FiltersHelper.mergeAux(merge, fresponse);
			}
		}
		return ok(Json.toJson(ListUtils.transform(merge, (x)->x.export())));
	}

	static Promise<SearchResponse> getMyResutlsPromise(final CommonQuery q) {
		Iterable<Promise<SourceResponse>> promises = callSources(q);
		// compose all futures, blocks until all futures finish
		Function<CommonFilterLogic, CommonFilterResponse> f = (CommonFilterLogic o) -> {
			return o.export();
		};

		Promise<List<SourceResponse>> promisesSequence = Promise.sequence(promises);
		return promisesSequence.map(new play.libs.F.Function<Collection<SourceResponse>, SearchResponse>() {
			List<SourceResponse> finalResponses = new ArrayList<SourceResponse>();

			public SearchResponse apply(Collection<SourceResponse> responses) {
				finalResponses.addAll(responses);
				// Logger.debug("Total time for all sources to respond:
				// "
				// + (System.currentTimeMillis()- initTime));
				SearchResponse r1 = new SearchResponse();
				ArrayList<CommonFilterLogic> merge = new ArrayList<CommonFilterLogic>();
				for (SourceResponse sourceResponse : finalResponses) {
					if (sourceResponse!=null){
						FiltersHelper.merge(merge, sourceResponse.filtersLogic);
						sourceResponse.filters = ListUtils.transform(sourceResponse.filtersLogic, f);
					}
				}
				r1.filters = ListUtils.transform(merge, f);
				r1.responses = mergeResponses(finalResponses);
				return r1;
			}

			private List<SourceResponse> mergeResponses(List<SourceResponse> finalResponses2) {
				List<SourceResponse> res = new ArrayList<>();
				for (SourceResponse r : finalResponses2) {
					boolean merged = false;
					for (SourceResponse r2 : res) {
						if ((r2!=null) && (r!=null)){
							if (r2.source.equals(r.source)) {
								// merge these 2 and replace r.
								res.remove(r2);
								res.add(r.merge(r2));
								merged = true;
								break;
							}
						}
					}
					if (!merged) {
						res.add(r);
					}
				}
				return res;
			}

		});
	}

	public static List<String> getTheSources() {
		Function<ISpaceSource, String> function = (ISpaceSource x) -> {
			return x.getSourceName();
		};
		return ListUtils.transform(ESpaceSources.getESources(), function);
	}

	private static Iterable<Promise<SourceResponse>> callSources(final CommonQuery q) {
		List<Promise<SourceResponse>> promises = new ArrayList<Promise<SourceResponse>>();
		BiFunction<ISpaceSource, CommonQuery, SourceResponse> methodQuery = (ISpaceSource src, CommonQuery cq) -> {
			try{
				SourceResponse res = src
						.getResults(cq);
					if (res.source==null){
						System.out.println("Error "+src.getSourceName());
					}
					return res;
			} catch(Exception e){
				e.printStackTrace();
				return null;
			}
			};
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
	

	/*
	 * public static Result testsearch() { return buildresult(new
	 * CommonQuery("Zeus")); }
	 * 
	 * private static Result buildresult(CommonQuery q) { // q.source =
	 * Arrays.asList(DigitalNZSpaceSource.LABEL); List<SourceResponse> res =
	 * search(q); SearchResponse r1 = new SearchResponse(); r1.responses = res;
	 * ArrayList<CommonFilterLogic> merge = new ArrayList<CommonFilterLogic>();
	 * for (SourceResponse sourceResponse : res) { //
	 * System.out.println(sourceResponse.source + " Filters: " + //
	 * sourceResponse.filters); FiltersHelper.merge(merge,
	 * sourceResponse.filtersLogic); } Function<CommonFilterLogic,
	 * CommonFilterResponse> f = (CommonFilterLogic o) -> { return o.export();
	 * }; List<CommonFilterResponse> merge1 = ListUtils.transform(merge, f); //
	 * System.out.println(" Merged Filters: " + merge1);
	 * 
	 * return ok(views.html.testsearch.render(userForm, res, merge1));
	 */

	public static Promise<Result> searchForMLTRelatedItems() {
		JsonNode json = request().body().asJson();

		try {
			List<String> ids = Arrays.asList(json.get("ids").asText().split(","));
			List<String> fields = Arrays.asList(json.get("fields").asText().split(","));

			SearchOptions options = new SearchOptions();
			options.setOffset(json.get("page").asInt());
			options.setCount(json.get("pageSize").asInt());

			ElasticSearcher similar = new ElasticSearcher();
			similar.setTypes(Arrays.asList(json.get("types").asText().split(",")));


			org.elasticsearch.action.search.SearchResponse similars = similar.relatedWithMLT("", ids, fields, options);
			Map<String, List<?>> resourcesPerType = ElasticUtils.getResourcesPerType(similars);

			return Promise.pure((Result)ok(Json.toJson(resourcesPerType)));
		} catch(NullPointerException npe) {

			return Promise.pure((Result)badRequest("Some fields are missing from the provided json"));
		} catch(Exception e) {

			return Promise.pure((Result)internalServerError("We are sorry something happened excecuting your query!"));
		}

	}

	public static Promise<Result> searchForDisMaxRelatedItems() {
		JsonNode json = request().body().asJson();

		try {
			String terms = json.get("terms").asText();
			String provider = json.get("provider").asText();
			String exclude = json.get("exclude").asText();

			SearchOptions options = new SearchOptions(0, 10);
			options.setScroll(false);
			options.setFilterType("and");
			options.setOffset(json.get("page").asInt());
			options.setCount(json.get("pageSize").asInt());
			/*options.addFilter("dataProvider", "");
			options.addFilter("dataProvider", "");
			options.addFilter("provider", "");*/

			ElasticSearcher similar = new ElasticSearcher();
			similar.setTypes(Arrays.asList(json.get("types").asText().split(",")));

			org.elasticsearch.action.search.SearchResponse similars = similar.relatedWithDisMax(terms, provider, exclude, options);
			Map<String, List<?>> resourcesPerType = ElasticUtils.getResourcesPerType(similars);

			return Promise.pure((Result)ok(Json.toJson(resourcesPerType)));
		} catch(NullPointerException npe) {

			return Promise.pure((Result)badRequest("Some fields are missing from the provided json"));
		} catch(Exception e) {

			return Promise.pure((Result)internalServerError("We are sorry something happened excecuting your query!"));
		}

	}

	public static Result suggestions() {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();

		try {

			String text = json.get("suggested").asText();
			String field = json.get("field").asText();

			ElasticSearcher suggester = new ElasticSearcher();
			SuggestResponse resp = suggester.searchSuggestions(text, field, new SearchOptions());

			Map<String, List<String>> suggestions = new HashMap<String, List<String>>();
			resp.getSuggest().getSuggestion(text).forEach( (w) ->
			{
				List<String> words = new ArrayList<String>();
				w.forEach( (s) -> words.add(s.getText().toString()) );
				suggestions.put(w.getText().toString(), words);
			});
			result.put("suggestions", Json.toJson(suggestions));

		} catch(Exception e) {
			log.error("Cannot bring suggestions to user", e);
			return internalServerError(e.getMessage());
		}

		return ok(result);
	}


}
