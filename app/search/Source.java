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

import java.util.HashSet;
import java.util.Set;

import play.libs.F.Promise;
import scalaz.concurrent.PromiseInstances;

/**
 * Source have the following functionality
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
 *  
 * To create a specific Source, extend this source or a suitable other source. Add it to the enumeration Sources.
 * @author Arne Stabenau
 *
 */
public class Source {
	
	/**
	 * Overwrite this and return the Sources entry that you represent.
	 * @return
	 */
	public Sources thisSource() {
		return Sources.Europeana;
	}
	
	/**
	 * Execute this query. Apply some form of load limiting to the backend used. All this requests are
	 * potential UI requests and should have priority.
	 * @param query
	 * @return
	 */
	public Promise<Response> execute( Query query ) {
		return Promise.pure( Response.EMPTY ); 
	}
	
	/**
	 * Take the incompleteRecord, which is some WITH Record and try to complete it.
	 * If you need to contact the backend, this is considered a non-priority request.
	 * @param incompleteRecord
	 * @return
	 */
	public Promise<Object> completeRecord( final Object incompleteRecord ) {
		return Promise.pure( incompleteRecord );
	}
	
	/**
	 * Retrieve by Id. Every backend source should be able to get a record by ids that is supplies to its records.
	 */
	public Promise<Object> getById( String id ) {
		return null;
	}
	
	/**
	 * If you supply this set, you can use the pruneQuery method to have just the filters that
	 * apply to this source, even to find if the Query actually should be executed on thsi source.
	 * If no filters remain after pruning, the Query should not be executed.
	 * @return
	 */
	public Set<String> supportedFieldnames() {
		return new HashSet<String>();
	}
	
	public Query pruneFilters( Query inputQuery ) {
		return inputQuery.pruneFilters( thisSource(), supportedFieldnames());
	}
	
	/**
	 * If a source supports autocompleting a query, here it can do it. Return null if you don't autocomplete.
	 */
	public Promise<String[]> autocomplete( String partialQueryString ) {
		return null;
	}
}
