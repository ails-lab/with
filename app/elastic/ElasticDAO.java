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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;

import play.Logger;
import play.Logger.ALogger;

/**
 * This class should contain methods that translate application logic requests to 
 * Elastic searches and return objects, collections or values that are not Elastic specific
 * @author stabenau
 *
 */
public class ElasticDAO {
	
	public static final ALogger log = Logger.of(ElasticDAO.class);
	private static ElasticDAO instance = null;
	
	public static ElasticDAO instance() {
		if( instance == null ) {
			instance = new ElasticDAO();
		}
		return instance;
	}
	
	/**
	 * Return all Record Ids
	 * @return
	 */
	public List<String> findAllRecordIds() {
		return findAllIdsByType( "culturalobject");
	}
	
	/**
	 * Return all Collection Ids
	 * @return
	 */
	public List<String> findAllCollectionIds() {
		return findAllIdsByType( "simplecollection");
	}
	
	/**
	 * Return all _id s from a certain type
	 * @param type
	 * @return
	 */
	public List<String> findAllIdsByType( String type ) {
		List<String> res = new ArrayList<String>();

		SearchResponse response = null;
		response = Elastic.getTransportClient()
				.prepareSearch()
				.setTypes( type )
				.setScroll(new TimeValue(60000))
				.setNoFields()
				.setSize(100)
				.execute()
				.actionGet();

		while (true) {
		    for (SearchHit hit : response.getHits().getHits()) {
				res.add(hit.getId());
		    }
		    response = Elastic.getTransportClient()
		    		.prepareSearchScroll(response.getScrollId())
		    		.setScroll(new TimeValue(60000))
		    		.execute()
		    		.actionGet();
		    
		    //Break condition: No hits are returned
		    if (response.getHits().getHits().length == 0) {
		        break;
		    }
		}
		return res;
	}
	
	/**
	 * Remove records by ids. Its more effcient to use with known type. Set type to null if
	 * not known or mixed.
	 * @param ids
	 */
	public void removeById( Collection<String> ids, String... types ) {
		try {
			List<String> typeList;
			if( types.length == 0 ) 
				typeList = Elastic.allTypes;
			else 
				typeList = Arrays.asList( types );
			
			for( String singleType: typeList ) {
				for( String id: ids ) {
					Elastic.getBulkProcessor().add(new DeleteRequest(
							Elastic.index,
							singleType,
							id.toString()));
				}
			}
			Elastic.getBulkProcessor().flush();
		} catch( Exception e) {
			log.error( "Bulk delete problem", e );
		}
	}
}
