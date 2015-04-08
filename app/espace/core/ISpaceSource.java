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


package espace.core;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;

public abstract class ISpaceSource {

	public abstract String getSourceName();

	public abstract String getHttpQuery(CommonQuery q);

	public abstract SourceResponse getResults(CommonQuery q);

	public String autocompleteQuery(String term, int limit) {
		return "";
	}
	
	public AutocompleteResponse autocompleteResponse(String response) {
		return new AutocompleteResponse();
	};
}
