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

import play.Logger;
import sources.utils.ListMap;
import utils.ListUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The Query class encapsulates all possible Queries that the system supports within and externally.
 *
 * @author Arne Stabenau
 *
 */
public class Query implements IFilterContainer{

	public static final Logger.ALogger log =Logger.of( Query.class );

	/**
	 * Which sources need to be queried.
	 */
	public List<Sources> sources = new ArrayList<>();

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
	private int start, count;

	/**
	 * Alternative to say the same. page is one based!
	 */
	private int page, pageSize;


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


	public static class Clause {
		List<Filter> filters = new ArrayList<Filter>();
		public static Clause create() {
			return new Clause();
		}

		public Clause add(String fieldId, String value ) {
			filters.add( new Filter( fieldId, value ));
			return this;
		}

		public Clause add(String fieldId, String value, boolean exact ) {
			filters.add( new Filter( fieldId, value, exact ));
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
	 * @param supportedFieldIds
	 * @return
	 */
	public Query pruneFilters(Sources source, Set<String> supportedFieldIds) {
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
					 .filter( f -> (supportedFieldIds == null )? true : supportedFieldIds.contains(f.fieldId ) )
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

	public int getPage() {
		return page;
	}

	public int getPageSize() {
		return pageSize;
	}

	public int getCount() {
		return count;
	}

	public int getStart() {
		return start;
	}

	//
	// Convenience builder functions
	//

	public void setStartCount( int start, int count ) {
		this.count = count;
		this.start = start;
		this.pageSize = count;
		this.page = (start/count)+1;
	}

	public void setPageAndSize( int page, int pageSize ) {
		this.page = page;
		this.pageSize = pageSize;
		this.count = pageSize;
		this.start = (page-1)*pageSize;
	}
	/**
	 * The given filters are assumed to be or-ed and are and-ed to this query
	 * @param filters
	 * @return
	 */
	public final Query addClause( Filter... filters ) {
		List<Filter> newTerm = new ArrayList<Filter>();
		newTerm.addAll( Arrays.asList( filters ));
		return addClause(newTerm);
	}

	/**
	 * The given filters are assumed to be or-ed and are and-ed to this query
	 * @param filters
	 * @return
	 */
	public final Query addClause( List<Filter> filters ) {
		this.filters.add( filters );
		return this;
	}

	/**
	 * Specify one fieldname and multiple possible values. They are added as being ored together and anded to existing Filters.
	 * @param fieldId
	 * @param allowedValues
	 * @return
	 */
	public Query andFieldvalues( String fieldId, String... allowedValues )  {
		List<Filter> clause = Arrays.stream( allowedValues )
		.map( val -> new Filter( fieldId, val ))
		.collect( Collectors.toCollection(()->new ArrayList<Filter>()));
		addClause( clause );
		return this;
	}

	/**
	 * in the CNF expression, the values of the filters with the same filed name inside the same clause
	 * are grouped in a map where the key is the filed name and the value is the list of values that have the same
	 * filter name in the original CNF.
	 * @return a List of the filters grouping values that share the same filed name.
	 */
	public List<HashMap<String,List<String>>> buildFactorizeFilters()  {
		List<HashMap<String,List<String>>> result = new ArrayList<>(filters.size());
		for (List<Filter> clause : filters) {
			ListMap<String,String> map = new ListMap<>();
			result.add(map);
			for (Filter filter : clause) {
				map.getOrSet(filter.fieldId).add(filter.value);
			}
		}
		return result;
	}
	
	@Override
	public List<String> getFilterRestriction(String filterID) {
		List<String> res = new ArrayList<>();
		for (List<Filter> clause : filters) {
			for (Filter filter : clause) {
				if (filterID.equals(filter.fieldId))
					res.add(filter.value);
			}
			if (!res.isEmpty())
				return res;
		}
		return null;
	}
	
	/**
	 * returns the filter with the specified filter name.
	 * @param filterName
	 * @return
	 */
	public Filter findFilter(String filterName){
		for (List<Filter> clause : filters) {
			for (Filter filter : clause) {
				if (filterName.equals(filter.fieldId))
					return filter;
			}
		}
		return null;
	}


	public Query addSource( Sources source ) {
		sources.add( source );
		return this;
	}
	
	public Query addSource( Sources... sources ) {
		for (Sources source : sources) {
			addSource(source);
		}
		return this;
	}

	public boolean containsSource( Sources findSource ) {
		for( Sources source: sources ) {
			if( findSource == source ) return true;
		}
		return false;
	}

	public Query or( List<List<Filter>> cnf ) {
		return new Query();
	}

	/**
	 * Log if some fields in here are not known
	 */
	public void validateFieldIds() {
		// first the query filters
		for( List<Filter> clause: filters ) {
			for( Filter f: clause ) {
				if(! Fields.validFieldId( f.fieldId ))
					log.warn( "FieldId '" + f.fieldId + "' in filters is not known.");
			}
		}
		for( String fieldId: facets) {
			if(! Fields.validFieldId( fieldId ))
				log.warn( "FieldId '" + fieldId+ "' in facets is not known.");
		}
	}

	public Map<Sources, Query> splitBySource() {
		Map<Sources, Query> res = new HashMap<>();
		for( Sources source: sources ) {
			Query newQuery = splitForSource(source);
			if( newQuery != null )
				res.put( source, newQuery );
		}
		return res;
	}
	
	public Query splitForSource(Sources source) {
		Set<String> supportedFieldIds = source.getDriver().supportedFieldIds();
		// this is not part of the list any more
		supportedFieldIds.add("anywhere");
		Query newQuery = this.pruneFilters(source, supportedFieldIds);
		return newQuery;
	}

	public HashSet<String> commonSupportedFields() {
		HashSet<String> commonFields = new HashSet<String>();		
		if( !sources.isEmpty()) {
			boolean first = true;
			for( Sources s:sources) {
				if( first ) {
					commonFields.addAll( s.getDriver().supportedFieldIds());
					first = false;
				} else {
					Iterator<String> i = commonFields.iterator();
					Set<String> newFields =  s.getDriver().supportedFieldIds();
					while( i.hasNext()) {
						if( ! newFields.contains(i.next())) i.remove();
					}
				}
			}
		}
		return commonFields;
	}
	
	/**
	 * Optionally remove facet requests from fields that are not supported.
	 */
	public void pruneFacets() {
		Set<String> common = commonSupportedFields();
		Iterator<String> i = facets.iterator();
		while( i.hasNext()) if( !common.contains(i.next())) i.remove();
	}
}
