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
 * The Query class encapsulates all possible Queries that the system supports within and externally.
 * 
 * @author Arne Stabenau
 *
 */
public class Query {
	public Sources[] sources;
	
	/**
	 * This is the CNF of the query. The inner array filters a meant to be ORed together, the outer  
	 */
	public Filter[][] filters;
	
	
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
}
