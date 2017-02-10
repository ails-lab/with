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


package search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;

import controllers.WithController;
import db.DB;
import model.resources.RecordResource;
import play.libs.F.Promise;
import search.Response.SingleResponse;
import sources.core.ParallelAPICall;

public abstract class SimilarSearch {

	public SimilarSearch() {
		// TODO Auto-generated constructor stub
	}
	

	public RecordResource<?> getTheRecord(SimilarsQuery q){
		RecordResource record = DB.getRecordResourceDAO().get(
				new ObjectId(q.getRecordId()));
		return record;
	}
	
	public abstract Promise<RecordsList> query(SimilarsQuery q);


	public static Promise<Map<Sources,SingleResponse>> executeQuery(Query q){
		if( q.getPage() > 0 ) {
			q.setPageAndSize( q.getPage(), q.getPageSize());
		} else {
			q.setStartCount( q.getStart(), q.getCount());
		}
		// check if the query needs readability additions for WITHin
		if( q.containsSource( Sources.WITHin)) {
			// add conditions for visibility in WITH
			Query.Clause visible = Query.Clause.create()
					.add( "administrative.isPublic", "true", true );
			for( String userId: WithController.effectiveUserIds()) {
				visible.add( "administrative.access.READ", userId, true );
				visible.add( "administrative.access.WRITE", userId, true );
				visible.add( "administrative.access.OWN", userId, true );
			}
			q.addClause( visible.filters());
		}
		// print warnings in the log for fields not known
		q.validateFieldIds();

		// split the query
		Map<Sources, Query> queries = q.splitBySource();
		// create promises
		Iterable<Promise<Response.SingleResponse>> promises =
				queries.entrySet().stream()
				.map( 
						entry -> 
						entry.getKey().getDriver().execute(entry.getValue())
					)
				.collect( Collectors.toList());

			// normal query
			Promise<List<SingleResponse>> allResponses  =
					Promise.sequence(promises, ParallelAPICall.Priority.FRONTEND.getExcecutionContext());
			return allResponses.map((List<SingleResponse> list)->{
				Map<Sources, SingleResponse> res = new HashMap<>();
				for (SingleResponse r : list) {
					res.put(r.source, r);
				}
				return res;
			});
	}
}
