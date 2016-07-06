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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	public List<Sources> sources = new ArrayList<Sources>();
	
	/**
	 * This is the CNF of the query. The inner array filters a meant to be ORed together, the outer array ANDs the inner terms.  
	 */
	public List<List<Filter>> filters = new ArrayList<List<Filter>>();
	
	/**
	 * An array of fieldnames where you want to get either a facet (value and count ovver the whole query result) or at least a 
	 * summary of value and counts for the returned subset. 
	 */
	public List<String> facets = new ArrayList<String>();
	
	/**
	 * How many we request from each source and at what offset. start is zero based.
	 */
	public int start, count;
	
	/**
	 * Alternative to say the same. page is one based! 
	 */
	public int page, pageSize;
	
	
	/**
	 * If a source can deliver results in multiple languages, it can be asked to limit its output to
	 * this language. Records should not be suppressed by this, but massive multilingual responses can be reduced to
	 * the responseLanguage. This does not affect the filtering, language selection is available via fieldnames.
	 * 
	 * We might consider a comma separated list of more than one here?
	 */
	public String responseLanguage;
	
	/**
	 * If you specify a continuationId, the backend will continue a search 
	 */
	public String continuationId;

	/**
	 * If you want quick answers, request continuation
	 */
	public boolean continuation = false;

	
	public static class Term {
		List<Filter> filters = new ArrayList<Filter>();
		public static Term create() {
			return new Term();
		}
		
		public Term add(String fieldname, String value ) {
			filters.add( new Filter( fieldname, value ));
			return this;
		}
		public List<Filter> filters() {
			return filters;
		}
	}
	
	/**
	 * Convenience Method to create a Query that just has the filters that are supported.
	 * If there are no filters left or if the source is not requested, return null.
	 * 
	 *  If there are no supportedFieldnames, return a clone.
	 * @param supportedFieldnames
	 * @return
	 */
	public Query pruneFilters(Sources source, Set<String> supportedFieldnames) {
		Query res = new Query();
		
		// put the source we are filtering for in the sources array
		if( sources.stream()
				.filter(elem-> elem==source )
				.findAny()
				.isPresent()) {
			res.sources.add( source );
		} else {
			return null;
		}
		
		res.filters.addAll( filters.stream()
			.map( term -> {
				return 
						// iterate over the contained Filters and only return the ones that 
						// have supported fieldnames
				   term.stream()
					 .filter( f -> (supportedFieldnames == null )? true : supportedFieldnames.contains(f.fieldname ) )
					 .collect( Collectors.toList());	
				})
			// throw out terms with no conditions
			.filter( newTerm -> (newTerm.size() > 0 ))
			.collect( Collectors.toList()));
		
		if( res.filters.size() == 0 ) return null;
		
		res.continuation = continuation;
		res.continuationId = continuationId;
		res.start = start;
		res.count = count;
		res.page = page;
		res.pageSize = pageSize;
		
		res.facets = facets;
		return res;
	}
	
	// 
	// Convenience builder functions
 	//
	
	/**
	 * The given filters are assumed to be or-ed and are and-ed to this query
	 * @param filters
	 * @return
	 */
	public final Query addTerm( Filter... filters ) {
		List<Filter> newTerm = new ArrayList<Filter>();
		newTerm.addAll( Arrays.asList( filters ));
		return this;
	}
	
	/**
	 * The given filters are assumed to be or-ed and are and-ed to this query
	 * @param filters
	 * @return
	 */
	public final Query addTerm( List<Filter> filters ) {
		this.filters.add( filters );
		return this;
	}
	
	/**
	 * Specify one fieldname and multiple possible values. They are added as being ored together and anded to existing Filters.
	 * @param fieldname
	 * @param allowedValues
	 * @return
	 */
	public Query andFieldvalues( String fieldname, String... allowedValues )  {
		List<Filter> term = Arrays.stream( allowedValues )
		.map( val -> new Filter( fieldname, val ))
		.collect( Collectors.toCollection(()->new ArrayList<Filter>()));
		addTerm( term );
		return this;
	}
	
	
	public Query addSource( Sources source ) {
		sources.add( source );
		return this;
	}
	
	public boolean containsSource( Sources findSource ) {
		for( Sources source: sources ) {
			if( findSource == source ) return true;
		}
		return false;
	}
	
	public Map<Sources, Query> splitBySource() {
		Map<Sources, Query> res = new HashMap<Sources, Query>();
		for( Sources source: sources ) {
			Set<String> supportedFieldnames = source.getDriver().supportedFieldnames();
			Query newQuery = this.pruneFilters(source, supportedFieldnames);
			res.put( source, newQuery );
		}
		return res;
	}
}
