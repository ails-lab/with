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


import java.util.Iterator;
import java.util.List;
import java.util.Map;

import model.resources.RecordResource;
import model.resources.ThesaurusObject;
import model.resources.WithResource;
import model.resources.collection.CollectionObject;

import org.bson.types.ObjectId;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;

import db.DB;
import play.Logger;

public class ElasticReindexer {
	static private final Logger.ALogger log = Logger.of(ElasticIndexer.class);




	/*
	 * SLOW RE-INDEX
	 *
	 * Retrieve all documents from Mongo and slowly index all
	 * of them to the index.
	 * Documents with the same id are reindex (so they are updated)
	 * at the index. Documents that do not exist they are indexed
	 * for the first time.
	 *
	 * Therefore we are NOT going to have duplicates.
	 */
	public static boolean reindexAllDbDocuments() {
		BulkProcessor bulk = Elastic.getBulkProcessor();
		long countAllRR = DB.getRecordResourceDAO().find().countAll();
		long countAllCO = DB.getCollectionObjectDAO().find().countAll();

		/* Index all RecordResources */
		try {
			for (int i = 0; i <= (countAllRR / 1000); i++) {
				log.error("indexed "+ i + "000 records.");
				Query<RecordResource> q = DB.getDs().createQuery(RecordResource.class).offset(i * 1000).limit(1000);
				Iterator<RecordResource> resourceCursor = DB.getRecordResourceDAO().find(q).iterator();
				while (resourceCursor.hasNext()) {
					RecordResource rr = null;
					try {
						rr = resourceCursor.next();
						bulk.add(new IndexRequest(Elastic.index, ElasticUtils.defineInstanceOf(rr), rr.getDbId().toString())
								.source(rr.transform()));
					} catch( Exception e ) {
						if( rr != null )
							log.error( "Record cant be indexed " + rr.getDbId());
					}
				}
				bulk.flush();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		/* Index all CollectionObjects */
		int j = 0;
		try {
			for (int i = 0; i <= (countAllCO / 1000); i++) {
				Query<CollectionObject> q1 = DB.getDs().createQuery(CollectionObject.class).offset(i * 1000)
						.limit(1000);
				Iterator<CollectionObject> collectionCursor = DB.getCollectionObjectDAO().find(q1).iterator();
				while (collectionCursor.hasNext()) {
					if (j % 100 == 0)
						log.error("index " + j + " collections");
					j++;
					CollectionObject co = null;
					try {
						co = collectionCursor.next();
						bulk.add(new IndexRequest(Elastic.index, ElasticUtils.defineInstanceOf(co), co.getDbId().toString())
								.source(co.transform()));
					} catch( Exception e ) {
						if( co != null )
							log.error( "Error during CollectionObject " + co.getDbId());
					}
				}
				bulk.flush();
			}

			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return true;
	}

	public static boolean reindexCollectionRecordResources(ObjectId colId) {
		BulkProcessor bulk = Elastic.getBulkProcessor();
		List<RecordResource> list = DB.getRecordResourceDAO().getByCollection(colId);

		/* Index all RecordResources */
		try {
			for (RecordResource rr : list) {
				bulk.add(new IndexRequest(Elastic.index, ElasticUtils.defineInstanceOf(rr), rr.getDbId().toString())
							.source(rr.transform()));
			}
			bulk.flush();

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		
		return true;
	}
		
	/*
	 * FAST RE-INDEX
	 *
	 * Find which documents belong to Mongo DB but they
	 * are not in the index and index only them.
	 * Probably using a single bulk operation.
	 */
	public static boolean indexInconsistentDocs() {

		BulkProcessor bulk = Elastic.getBulkProcessor();
		List<Key<RecordResource>> recordKeys = DB.getRecordResourceDAO().find().asKeyList();
		List<Key<CollectionObject>> collectionKeys = DB.getCollectionObjectDAO().find().asKeyList();

		for(Key<RecordResource> k: recordKeys) {
			TermQueryBuilder termQ = QueryBuilders.termQuery("_id", k.getId());
			SearchResponse resp = Elastic.getTransportClient().prepareSearch(Elastic.index)
					.setSize(0)
					.setTerminateAfter(1)
					.setQuery(termQ)
					.setFetchSource(false)
					.execute().actionGet();

			if(resp.getHits().getTotalHits() == 0) {
				RecordResource rr = DB.getRecordResourceDAO().getById(new ObjectId(k.getId().toString()));
				bulk.add(new IndexRequest(Elastic.index, ElasticUtils.defineInstanceOf(rr), rr.getDbId().toString())
					.source(rr.transform()));
			}
		}
		for(Key<CollectionObject> k: collectionKeys) {
			TermQueryBuilder termQ = QueryBuilders.termQuery("_id", k.getId());
			SearchResponse resp = Elastic.getTransportClient().prepareSearch(Elastic.index)
					.setSize(0)
					.setTerminateAfter(1)
					.setQuery(termQ)
					.setFetchSource(false)
					.execute().actionGet();

			if(resp.getHits().getTotalHits() == 0) {
				CollectionObject co = DB.getCollectionObjectDAO().getById(new ObjectId(k.getId().toString()));
				bulk.add(new IndexRequest(Elastic.index, ElasticUtils.defineInstanceOf(co), co.getDbId().toString())
					.source(co.transform()));
			}
		}
		bulk.flush();
		return true;
	}

	/*
	 * CHANGE INDICE
	 *
	 * Reindex all documents of a indice to a newly created indice.
	 * Method iterates through a scroll search cursor and reindex every
	 * 5ms a maximum size of 1000 documents.
	 */
	public static boolean reindexOnANewIndice(String oldIndice, String newIndice) {
		SearchResponse scrollResp = Elastic.getTransportClient()
				.prepareSearch(oldIndice)
				.setSearchType(SearchType.SCAN)
				.setScroll(new TimeValue(60000))
				.setQuery(QueryBuilders.matchAllQuery())
				.setSize(100)
				.execute().actionGet();

		BulkProcessor bulk = Elastic.getBulkProcessor();

		while(true) {
			for(SearchHit hit: scrollResp.getHits()) {
				IndexRequest req = new IndexRequest(newIndice, hit.type(), hit.id());
				Map source = ((hit.getSource()));
				req.source(source);
				bulk.add(req);
			}

			scrollResp = Elastic.getTransportClient()
					.prepareSearchScroll(scrollResp.getScrollId())
					.setScroll(new TimeValue(600000))
					.execute().actionGet();

			if(scrollResp.getHits().getHits().length == 0) {
				log.info("Closing the bulk processor");
				bulk.flush();
		        break;
			}


		}

		return true;
	}

	/*
	 * Re-index all collection objects
	 */
	public static boolean reindexAllDbCollections() {

		BulkProcessor bulk = Elastic.getBulkProcessor();
		long countAllTH = DB.getCollectionObjectDAO().find().countAll();

		/* Index all CollectionObjects */
		for(int i = 0; i < (countAllTH/1000); i++) {
			Query<CollectionObject> q = DB.getDs().createQuery(CollectionObject.class).offset(i*1000).limit(1000);
			Iterator<CollectionObject> collectionCursor = DB.getCollectionObjectDAO().find(q).iterator();
			while(collectionCursor.hasNext()) {
				CollectionObject co = collectionCursor.next();
				bulk.add(new IndexRequest(Elastic.index, ElasticUtils.defineInstanceOf(co), co.getDbId().toString())
				.source(co.transform()));
			}
			bulk.flush();
		}

		Query<CollectionObject> q = DB.getDs().createQuery(CollectionObject.class).retrievedFields(false, "collectedResources").offset((int)(countAllTH/1000)*1000).limit(1000);
		@SuppressWarnings("unused")
		int count = (int) q.countAll();
		Iterator<CollectionObject> collectionCursor = DB.getCollectionObjectDAO().find(q).iterator();
		while(collectionCursor.hasNext()) {
			CollectionObject co = collectionCursor.next();
			bulk.add(new IndexRequest(Elastic.index, ElasticUtils.defineInstanceOf(co), co.getDbId().toString())
			.source(co.transform()));
		}

		bulk.flush();
		return true;

	}

	/*
	 *   Re-index all thesaurus objects
	 */
	public static boolean reindexAllDbThesaurus() {

		BulkProcessor bulk = Elastic.getBulkProcessor();
		long countAllTH = DB.getThesaurusDAO().find().countAll();

		/* Index all RecordResources */
		for(int i = 0; i < (countAllTH/1000); i++) {
			Query<ThesaurusObject> q = DB.getDs().createQuery(ThesaurusObject.class).offset(i*1000).limit(1000);
			Iterator<ThesaurusObject> thesaurusCursor = DB.getThesaurusDAO().find(q).iterator();
			while(thesaurusCursor.hasNext()) {
				ThesaurusObject th = thesaurusCursor.next();
				bulk.add(new IndexRequest(Elastic.index, ElasticUtils.defineInstanceOf(th), th.getDbId().toString())
				.source(th.transform()));
			}
			bulk.flush();
		}

		Query<ThesaurusObject> q = DB.getDs().createQuery(ThesaurusObject.class).offset((int)(countAllTH/1000)*1000).limit(1000);
		Iterator<ThesaurusObject> thesaurusCursor = DB.getThesaurusDAO().find(q).iterator();
		while(thesaurusCursor.hasNext()) {
			ThesaurusObject th = thesaurusCursor.next();
			bulk.add(new IndexRequest(Elastic.index, ElasticUtils.defineInstanceOf(th), th.getDbId().toString())
			.source(th.transform()));
		}

		bulk.flush();
		return true;

	}
}
