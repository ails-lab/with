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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import espace.core.AutocompleteResponse;
import espace.core.ESpaceSources;
import espace.core.ISpaceSource;
import espace.core.ParallelAPICall;
import play.Logger;
import play.libs.F.Promise;
import play.libs.F.Tuple;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import utils.MethodCallable;

public class AutocompleteController extends Controller {
	
	
	public static Promise<Result> autocompleteExt(String term, List<String> sourceFromUI) {
		List<ISpaceSource> sourcesForAutocomplete = new ArrayList<ISpaceSource>();
		if (sourceFromUI.isEmpty())
			sourcesForAutocomplete = ESpaceSources.getESources();
		else {
			for (ISpaceSource s: ESpaceSources.getESources()) {
				if (sourceFromUI.contains(s.getSourceName()))
					sourcesForAutocomplete.add(s);
			}
		}
		return getSuggestionsResponse(sourcesForAutocomplete, term);
	}
	
	//the union of the suggestions collected from the sources APIs is returned
	//if no source returns suggestions, then empty content is returned
	@BodyParser.Of(BodyParser.Json.class)
	private static Promise<Result> getSuggestionsResponse(List<ISpaceSource> sourcesWithAutocomplete, final String term) {
		MethodCallable<Tuple<String, ISpaceSource>, AutocompleteResponse> methodQuery = new MethodCallable<Tuple<String, ISpaceSource>, AutocompleteResponse>() {
			public AutocompleteResponse call(Tuple<String, ISpaceSource> input) {
				try {
					String autocompleteQuery = input._1;
					ISpaceSource src = input._2;
					URL url = new URL(autocompleteQuery);
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			        conn.setRequestMethod("GET");
			        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			        StringBuffer response = new StringBuffer();
			        String line = "";
			        while ((line = rd.readLine()) != null) {
			            response.append(line);
			        }
			        rd.close();
			        //Logger.debug("Called " +  autocompleteQuery + " and got " + response.toString());
			        //transform response into standard json
			        AutocompleteResponse standardResponse = src.autocompleteResponse(response.toString());;
			        return standardResponse;
				} catch (IOException e) {
					e.printStackTrace();
					return new AutocompleteResponse();
				}	
		   }
		};
		Iterable<Promise<AutocompleteResponse>> promises = new ArrayList<Promise<AutocompleteResponse>>();
		for (final ISpaceSource source: sourcesWithAutocomplete) {
			final String autocompleteQuery = source.autocompleteQuery(term);
			if (!autocompleteQuery.isEmpty()) {
				((ArrayList<Promise<AutocompleteResponse>>) promises).add(
					ParallelAPICall.createPromise(methodQuery, new Tuple(autocompleteQuery, source))
				);
			}
		}
		MethodCallable<AutocompleteResponse, Boolean> responseCollectionMethod = new MethodCallable<AutocompleteResponse, Boolean>() {
			public Boolean call(AutocompleteResponse response) {
				return !response.suggestions.isEmpty();
			}
		};
		return ParallelAPICall.<AutocompleteResponse>combineResponses(responseCollectionMethod, promises);
	}
		

}
