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

import java.util.ArrayList;

import org.elasticsearch.action.search.SearchResponse;

import play.Logger;
import elastic.ElasticSearcher;
import espace.core.CommonQuery;
import espace.core.ISpaceSource;
import espace.core.RecordJSONMetadata;
import espace.core.SourceResponse;

/*
 * This source is for internal search to WITH collections
 */
public class ElasticSource extends ISpaceSource {
	public static final Logger.ALogger log = Logger.of(ElasticSource.class);

	@Override
	public String getSourceName() {
		return "elastic";
	}

	@Override
	public String getHttpQuery(CommonQuery q) {
		log.debug("Method not implemented yet");
		return null;
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {
		ElasticSearcher searcher = new ElasticSearcher();
		String term = q.getQuery();
		SearchResponse resp = searcher.search(term);
		searcher.closeClient();
		return new SourceResponse(resp, "With", Integer.parseInt(q.page));
	}

	@Override
	public ArrayList<RecordJSONMetadata> getRecordFromSource(String recordId) {
		log.debug("Method not implemented yet");
		return null;
	}

}
