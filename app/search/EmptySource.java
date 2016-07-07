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

/**
 * This is an empty implementation of Source, it will collect some usefull methods while the design is going on.
 * 
 * @author Arne Stabenau
 *
 */
public class EmptySource implements Source {
	
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
	public Promise<Response.SingleResponse> execute( Query query ) {
		return Promise.pure( Response.SingleResponse.EMPTY ); 
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
	
	/**
	 * If a source supports autocompleting a query, here it can do it. Return null if you don't autocomplete.
	 */
	public Promise<String[]> autocomplete( String partialQueryString ) {
		return null;
	}

	//
	//  Convenience methods
	//
	
	public Query pruneFilters( Query inputQuery ) {
		return inputQuery.pruneFilters( thisSource(), supportedFieldnames());
	}
	
}
