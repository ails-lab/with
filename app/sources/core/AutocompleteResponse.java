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


package sources.core;

import java.util.List;

import org.mongodb.morphia.annotations.Entity;

import com.fasterxml.jackson.databind.JsonNode;

//everythign public and in one place for brevity
public class AutocompleteResponse {
	
    public static class Suggestion {
    	public String value;
    	public DataJSON data;
    }
	public static class DataJSON {
		public String category;
		public int frequencey = -1;
		public String field = "";
	}
	
	
	public List<Suggestion> suggestions;
}
