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

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Query class encapsulates all possible Queries that the system supports within and externally.
 * 
 * @author Arne Stabenau
 *
 */
public class Query {
	/**
	 * Which sources need to be queried.
	 */
	public Sources[] sources;
	
	/**
	 * This is the CNF of the query. The inner array filters a meant to be ORed together, the outer array ANDs the inner terms.  
	 */
	public Filter[][] filters;
	
	/**
	 * An array of fieldnames where you want to get either a facet (value and count ovver the whole query result) or at least a 
	 * summary of value and counts for the returned subset. 
	 */
	public String[] facets;
	
	/**
	 * How many we request from each source and at what offset. start is zero based.
	 */
	public int start, count;
	
	/**
	 * Alternative to say the same. page is one based! 
	 */
	public int page, pageSize;
	
	
	/**
	 * If you specify a continuationId, the backend will continue a search 
	 */
	public String continuationId;

	/**
	 * If you want quick answers, request continuation
	 */
	public boolean continuation = false;

	
	/**
	 * Convenience Method to create a Query that just has the filters that are supported.
	 * If there are no filters left or if the source is not requested, return null. 
	 * @param supportedFieldnames
	 * @return
	 */
	public Query pruneFilters(Sources source, Set<String> supportedFieldnames) {
		Query res = new Query();
		
		// put the source we are filtering for in the sources array
		if( Arrays.stream(sources)
				.filter(elem-> elem==source )
				.findAny()
				.isPresent()) {
			res.sources = new Sources[1];
			res.sources[0]  = source;
		} else {
			return null;
		}
		
		Filter[][] newFilters = 
		Arrays.stream( filters )
			.map( term -> {
				return 
						// iterate over the contained Filters and only return the ones that 
						// have supported fieldnames
				    Arrays.stream( term )
					 .filter( f -> supportedFieldnames.contains(f.fieldname ) )
					 .collect( Collectors.toList());	
				})
			// throw out terms with no conditions
			.filter( newTerm -> (newTerm.size() > 0 ))
			// back to Arrays
			.map( newTerm -> newTerm.toArray( new Filter[0]) )
			.collect( Collectors.toList())
			// back to Array of Array
			.toArray( new Filter[0][0] );
		
		if( newFilters.length == 0 ) return null;
		
		res.filters = newFilters;
		res.continuation = continuation;
		res.continuationId = continuationId;
		res.start = start;
		res.count = count;
		res.page = page;
		res.pageSize = pageSize;
		
		res.facets = facets;
		return res;
	}
}
