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


package sources;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;

import sources.core.CommonFilter;
import sources.core.CommonFilters;
import sources.core.CommonQuery;
import sources.core.HttpConnector;
import sources.core.SourceResponse;
import sources.core.Utils;
import sources.core.Utils.Pair;
import utils.ListUtils;

public class EuropeanaCollectionSpaceSource extends EuropeanaSpaceSource{

	private String collectionName;
	
	public EuropeanaCollectionSpaceSource(String collectionName) {
		super();
		addDefaultWriter("europeana_collectionName", qfwriter("europeana_collectionName"));
		this.collectionName = collectionName;
	}
	
	private Function<List<String>, Pair<String>> qfwriter(String parameter) {
		Function<String, String> function = (String s) -> {
			return "%22" + Utils.spacesFormatQuery(s, "%20") + "%22";
		};
		return new Function<List<String>, Pair<String>>() {
			@Override
			public Pair<String> apply(List<String> t) {
				return new Pair<String>("qf", parameter + "%3A" + Utils.getORList(ListUtils.transform(t, function)));
			}
		};
	}
	
	@Override
	public SourceResponse getResults(CommonQuery q) {
		q.addFilter(new CommonFilter("europeana_collectionName",getCollectionName()));
		return super.getResults(q);
	}
	
	public SourceResponse getAllResults(CommonQuery q) {
		q.filters = Arrays.asList(new CommonFilter("europeana_collectionName",getCollectionName()));
		return super.getResults(q);
	}

	public String getCollectionName() {
		return collectionName;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}
	
	

}
