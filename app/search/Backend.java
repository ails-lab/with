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


/**
 * Backends have the following functionality
 *  - execute Querys to the backend search API
 *  - return results in WITH format (translate native response to WITH type)
 *  - optionally retrieve a complete record based on a search result
 *  Unclear whether this functionalities have to be exposed
 *  - translate filter fieldnames to native fieldnames
 *  - translate filter values to native values
 *  
 *  
 * Problematic is, that some of these functionalities will be executed in varying execution Contexts, sometimes the result
 * is needed asap and sometimes queuing is requested or required. Lastly, some of the functionalities may need to be executed
 * on different machines.
 *  
 * @author Arne Stabenau
 *
 */
public class Backend {
	/**
	 * Execute this query. Apply some form of load limiting to the backend used. All this requests are
	 * potential UI requests and should have priority.
	 * @param query
	 * @return
	 */
	public Response execute( Query query ) {
		return Response.EMPTY;
	}
	
	/**
	 * Take the incompleteRecord, which is some WITH Record and try to complete it.
	 * If you need to contact the backend, this is considered a non-priority request.
	 * @param incompleteRecord
	 * @return
	 */
	public Object completeRecord( final Object incompleteRecord ) {
		return incompleteRecord;
	}
	
	/**
	 * Goes over the query filters and makes a new query with 
	 * @param query
	 * @return
	 */
	public Query translateFilters( Query query ) {
		return new Query();
	}
	
	
	
}
