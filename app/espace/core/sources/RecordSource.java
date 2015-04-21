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

import com.fasterxml.jackson.databind.JsonNode;

import espace.core.HttpConnector;
import espace.core.RecordJSONMetadata;
import espace.core.RecordJSONMetadata.Format;

public class RecordSource {

	public enum Source {
		EUROPEANA, DPLA
	}

	public static class Request {
		public Request(String url, Format format) {
			this.url = url;
			this.format = format;
		}

		String url;
		Format format;
	}

	public Source source;
	public String recordId;
	private ArrayList<Request> requests;

	public RecordSource(Source source, String recordId) {
		this.source = source;
		this.recordId = recordId;
		requests = new ArrayList<Request>();
	}

	public ArrayList<RecordJSONMetadata> getRecordFromSource()
			throws IOException {
		switch (source) {
		case EUROPEANA:
			requests.add(new Request("http://www.europeana.eu/api/v2/record/"
					+ recordId + ".jsonld?wskey=ANnuDzRpW", Format.JSONLD));
			requests.add(new Request("http://www.europeana.eu/api/v2/record/"
					+ recordId + ".json?wskey=ANnuDzRpW", Format.JSON));
			break;
		case DPLA:
			break;
		default:
			break;

		}
		ArrayList<RecordJSONMetadata> jsonMetadata = new ArrayList<RecordJSONMetadata>();
		for (Request request : requests) {
			JsonNode response = HttpConnector.getURLContent(request.url);
			JsonNode record = response.get("object");
			jsonMetadata.add(new RecordJSONMetadata(request.format, record
					.toString()));
		}
		return jsonMetadata;
	}

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

	public String getRecordId() {
		return recordId;
	}

	public void setRecordId(String recordId) {
		this.recordId = recordId;
	}

}
