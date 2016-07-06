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
import java.util.List;

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
	}
	
	public static class ValueCounts {
		// a field for facets
		public String fieldname;
		public List<ValueCount> valueCounts;
	}
	
	public static class ValueList {
		// facets without counts...just values per field
		public String fieldname;
		public List<String> values;
	}
	
	public static class SingleResponse {
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
		public List<Object> items;
		
		/**
		 * If a source supports facets, they will be delivered here. All the requested facets should be returned,
		 * if the fieldnames are supported.
		 */
		public List<ValueCounts> facets = new ArrayList<ValueCounts>();
		
		/**
		 * If a source cannot support real facets, simple counts for the values will be provided here. They just cover this
		 * page (Result). If a paging query asks for more results, counts are freshly calculated.
		 */
		public List<ValueCounts> counts = new ArrayList<ValueCounts>();
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
	 * Mergeing facet counts is not very meaningful. This is a merge without the counts. If possible the most 
	 * frequent values are in the front of the list, but no guarantees.
	 */
	public List<ValueList> accumulatedValues = new ArrayList<ValueList>();

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
	
}
