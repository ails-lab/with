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


package espace.core.sources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import model.CollectionRecord;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.json.JSONArray;
import org.json.JSONException;

import play.Logger;
import play.libs.Json;

import com.fasterxml.jackson.databind.JsonNode;

import elastic.Elastic;
import elastic.ElasticSearcher;
import elastic.ElasticSearcher.SearchOptions;
import espace.core.AutocompleteResponse;
import espace.core.AutocompleteResponse.DataJSON;
import espace.core.AutocompleteResponse.Suggestion;
import espace.core.CommonQuery;
import espace.core.HttpConnector;
import espace.core.ISpaceSource;
import espace.core.RecordJSONMetadata;
import espace.core.RecordJSONMetadata.Format;
import espace.core.SourceResponse;
import espace.core.SourceResponse.ItemsResponse;
import espace.core.SourceResponse.Lang;
import espace.core.SourceResponse.MyURL;
import espace.core.Utils;

public class MintSpaceSource extends ISpaceSource {
	public static final Logger.ALogger log = Logger.of(MintSpaceSource.class);


	@Override
	public String getSourceName() {
		return "Mint";
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {
		ElasticSearcher searcher = new ElasticSearcher(Elastic.type_general);
		String term = q.getQuery();
		int count = Integer.parseInt(q.pageSize);
		int offset = (Integer.parseInt(q.page)-1)*count;
		
		SearchOptions elasticoptions = new SearchOptions(offset, count);
		elasticoptions.addFilter("source", "mint");
		if(q.user == null)	elasticoptions.addFilter("public", "true");
		else elasticoptions.setUser(q.getUser());
		
		SearchResponse resp = searcher.search(term, elasticoptions);
		searcher.closeClient();
		return new SourceResponse(resp, getSourceName(), count);
	}

	@Override
	public ArrayList<RecordJSONMetadata> getRecordFromSource(String recordId) {
		log.debug("Method not implemented yet");
		return null;
	}



}
