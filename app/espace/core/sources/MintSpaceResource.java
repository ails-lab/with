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


package espace.core.sources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;

import com.fasterxml.jackson.databind.JsonNode;

import espace.core.AutocompleteResponse;
import espace.core.AutocompleteResponse.DataJSON;
import espace.core.AutocompleteResponse.Suggestion;
import espace.core.CommonQuery;
import espace.core.HttpConnector;
import espace.core.ISpaceSource;
import espace.core.RecordJSONMetadata;
import espace.core.RecordJSONMetadata.Format;
import espace.core.SourceResponse;
import espace.core.SourceResponse.ItemsResponse;
import espace.core.SourceResponse.MyURL;
import espace.core.Utils;

public class MintSpaceResource extends ISpaceSource {

	// TODO keep track of the pages links and go to the requested page.

	private String sourceName;

	public MintSpaceResource() {
		this.sourceName = "Mint";
	}

	public String getSourceName() {
		return sourceName;
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {
		//q includes searchTerm, page, pageSize

		return new SourceResponse();
	}

}
