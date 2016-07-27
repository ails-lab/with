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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A Response encapsulates all information that is needed to answer a Query
 *
 * @author Arne Stabenau
 *
 */
public class Response {
	public static Response EMPTY = new Response();

	public static class ValueCount {
		// a string appearing in a result field
		public String value;

		// how often this happens in the query result
		public int count;

		public ValueCount() { };
		public ValueCount(String value, int count) {
			super();
			this.value = value;
			this.count = count;
		}

	}

	public static class ValueCounts extends HashMap<String,List<ValueCount>> {
		// keys are field ids
		// values are list of values and occurrence counts
	}

	public static class ValueList extends HashMap<String, List<String>> {
		// keys are fieldIds
		// value is list of occurring values in that field
		// its like facets without counts
	}


	public static class SingleResponse {
		/**
		 * limit of value counts returned in a single facet.
		 */
		public static final int FACETS_LIMIT = 10;


		public static SingleResponse EMPTY = new Response.SingleResponse();


		/**
		 * The source where this result belongs to
		 */
		public Sources source;

		/**
		 * The total count of results if available
		 */
		public int totalCount;

		/**
		 * A start offset (0 based) where this results are in the overall result set
		 */
		public int start;

		/**
		 * How many results this are.
		 */

		public int count;

		/**
		 * which page this would be (1 based ... pagesize is taken from query pageSize or query.count)
		 */
		public int page;


		/**
		 * Assumed pageSize, taken from query.pageSize or query.count
		 */
		public int pageSize;

		/**
		 * The actual result items. These are already translated into WITH types and are likely not complete.
		 * Sources should consider to limit the amount of data to something sensible.
		 */
		public List items = new ArrayList();

		/**
		 * If a source supports facets, they will be delivered here. All the requested facets should be returned,
		 * if the fieldids are supported. A facet is a fieldId with a list of all values for that field and their occurrence
		 * count for the WHOLE search result.
		 */
		public ValueCounts facets = new ValueCounts();

		/**
		 * If a source cannot support real facets, simple counts for the values will be provided here. They just cover this
		 * page (Result). If a paging query asks for more results, counts are freshly calculated. Values for the field that occur in
		 * a later page of the result will NOT be included.
		 */
		public ValueCounts counts = new ValueCounts();

		/**
		 * in case there was something wrong with the call the error message can be found here,
		 * otherwise this field should be null.
		 */
		public String errorMessage;
		
		//
		//  Convenience Functions to build the SingleResponse
		//

		public void addItem( Object item ) {
			items.add( item );
		}

		public void addValueToCounts( String fieldId, String value ) {

		}

		public void addFacet( String fieldId, String value, int count  ) {
			ValueCount vc = new ValueCount(value, count);
			if(facets.containsKey(fieldId)) {
				facets.get(fieldId).add(vc);
			} else {
				List<ValueCount> vcs = new ArrayList<Response.ValueCount>();
				vcs.add(vc);
				facets.put(fieldId, vcs);
			}
		}
	}

	/**
	 * the results entries are per source and can be independently calculated
	 */
	public List<SingleResponse> results = new ArrayList<SingleResponse>();

	/**
	 * All the sources this response is containing
	 */
	public List<Sources> sources = new ArrayList<Sources>();

	/**
	 * Merging facet counts is not very meaningful. This is a merge without the counts. If possible the most
	 * frequent values are in the front of the list, but no guarantees. AccumulatedValues get updated with continuation,
	 * representing all the values that were delivered from all requested sources.
	 */
	public ValueList accumulatedValues = new ValueList();

	/**
	 * If this is a part of multipart response and we expect more parts to come, the next part can be retrieved with this id.
	 */
	public String continuationId;

	/**
	 * The query this response is answering.
	 */
	public Query query;

	//
	// Some convenience functions
	//

	/**
	 * Call this to get a SingleResponse to fill up.
	 * @param source
	 * @return
	 */
	public SingleResponse getOrMakeSingleResponse( Sources source ) {
		for( SingleResponse sr: results ) {
			if( sr.source == source) return sr;
		}
		SingleResponse res = new SingleResponse();
		res.source = source;
		results.add( res );
		sources.add( source );
		return res;
	}

	// Merge a value list into a hashmap of value, count
	private void valueCountsIntoHashmap( List<ValueCount> vals, HashMap<String, Integer> res ) {
		for( ValueCount vc: vals ) {
			if( res.containsKey(vc.value )) {
				int count = res.get( vc.value );
				count += vc.count;
				res.put( vc.value, count );
			} else {
				res.put( vc.value, vc.count);
			}
		}
	}

	private void accumulateValueCountsList( HashMap<String,HashMap<String,Integer>> accumulator, ValueCounts vcs ) {
		for( Map.Entry<String,List<Response.ValueCount>> me: vcs.entrySet() ) {
			HashMap<String,Integer> valCounts = accumulator.get( me.getKey());
			if( valCounts == null ) {
				valCounts = new HashMap<String, Integer>();
				accumulator.put( me.getKey(), valCounts );
			}
			valueCountsIntoHashmap(me.getValue(), valCounts);
		}
	}
	/**
	 * This creates accumulatedValues to represent the current content of facets and values in this Response.
	 * Don't use for continuation.
	 * @param sr
	 */
	public void createAccumulated( ) {
		HashMap<String,HashMap<String, Integer>> accumulated = new HashMap<String,HashMap<String, Integer>>();

		for( SingleResponse sr: results ) {
			accumulateValueCountsList(accumulated, sr.facets);
			accumulateValueCountsList(accumulated, sr.counts);
		}

		// convert the Hash of hash of string count fieldId->value->count
		// to the list valueList with sorting of values by count downwards
		accumulatedValues = new ValueList();
		for( Map.Entry<String, HashMap<String, Integer>> entry: accumulated.entrySet() ) {
			String fieldId = entry.getKey();
			List<String> values = entry.getValue().entrySet().stream()
			.sorted( (a,b)-> {
				return (Integer.compare(a.getValue(), b.getValue())*(-1));
			})
			// here the entries are sorted with more frequent occuring ones first
			.map( e2 -> e2.getKey())
			// we collect only the strings into a list
			.collect( Collectors.toCollection(()->new ArrayList<String>()));
			accumulatedValues.put( fieldId, values);
		}
	}

	/**
	 * Get value lost for this field or make one if it doesn't exist.
	 * @param fieldId
	 * @return
	 */
	private List<String> findOrAddVl( String fieldId ) {
		List<String> values =  accumulatedValues.get(fieldId );
		if( values == null ) {
			values = new ArrayList<String>();
			accumulatedValues.put( fieldId, values);
		}

		return values;
	}

	/**
	 * Merge another Response accumulatedValues into this. This will make it impossible to keep count sorted values in accumulated,
	 * we will just append the values for the same fields (-duplicates).
	 * @param sr
	 */
	public void mergeAccumulated( Response other) {
		other.accumulatedValues.entrySet().forEach(
			entry -> {
				List<String> thisVl =findOrAddVl( entry.getKey() );
				HashSet<String> index = new HashSet<String>();
				index.addAll( thisVl );
				entry.getValue().forEach(s -> {
					if(! index.contains(s)) thisVl.add( s );
				});
			}
		);
	}

	/**
	 * This call does update the sources, but not the accumulatedValues.
	 * @param sr
	 */
	public void addSingleResponse( SingleResponse sr ) {
		results.add( sr );
		sources.add( sr.source );
	}

}
