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


package elastic;


import java.util.Map;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.junit.Test;

import play.Logger;

public class ElasticReindexer {
	static private final Logger.ALogger log = Logger.of(ElasticIndexer.class);

	private String oldIndex = "with2";
	private String newIndex = "with_v2";

	public ElasticReindexer() {

	}

	/*public ElasticReindexer(String oldName, String newName) {
		this.oldIndex = oldName;
		this.newIndex = newName;
	}

	public static void main(String args[]) {
		ElasticReindexer reindexer = new ElasticReindexer(args[0], args[1]);
		reindexer.reindex();
	}*/

	@Test
	public void reindex() {
		SearchResponse scrollResp = Elastic.getTransportClient()
				.prepareSearch(oldIndex)
				.setSearchType(SearchType.SCAN)
				.setScroll(new TimeValue(60000))
				.setQuery(QueryBuilders.matchAllQuery())
				.setSize(100)
				.execute().actionGet();

		int BULK_ACTIONS_THRESHOLD = 1000;
		int BULK_CONCURRENT_REQUESTS = 1;
		BulkProcessor bulkProcessor = BulkProcessor.builder(Elastic.getTransportClient(), new BulkProcessor.Listener() {
		    @Override
		    public void beforeBulk(long executionId, BulkRequest request) {
		        log.info("Bulk Going to execute new bulk composed of {} actions", request.numberOfActions());
		    }

		    @Override
		    public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
		        log.info("Executed bulk composed of {} actions", request.numberOfActions());
		    }

		    @Override
		    public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
		        log.warn("Error executing bulk", failure);
		    }
		    })
		    .setBulkActions(BULK_ACTIONS_THRESHOLD)
		    .setConcurrentRequests(BULK_CONCURRENT_REQUESTS)
		    .setFlushInterval(TimeValue.timeValueMillis(5))
		    .build();

		while(true) {
			scrollResp = Elastic.getTransportClient()
					.prepareSearchScroll(scrollResp.getScrollId())
					.setScroll(new TimeValue(600000))
					.execute().actionGet();
			if(scrollResp.getHits().getHits().length == 0) {
				log.info("Closing the bulk processor");
		        bulkProcessor.close();
		        break;
			}

			for(SearchHit hit: scrollResp.getHits()) {
				IndexRequest req = new IndexRequest(newIndex, hit.type(), hit.id());
				Map source = ((hit.getSource()));
				req.source(source);
				bulkProcessor.add(req);
			}
		}

	}

	public String getNewIndex() {
		return newIndex;
	}

	public void setNewIndex(String newIndex) {
		this.newIndex = newIndex;
	}

	public String getOldIndex() {
		return oldIndex;
	}

	public void setOldIndex(String oldIndex) {
		this.oldIndex = oldIndex;
	}


}
