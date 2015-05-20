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

public class RecordJSONMetadata {

	public enum Format {
		JSON_UNKNOWN, JSONLD_UNKNOWN, XML_UNKNOWN, 
		JSON_EDM, JSONLD_EDM, XML_EDM,
		JSONLD_DPLA,
		JSON_NLA, XML_NLA, 
		JSON_DNZ, XML_DNZ,
		JSON_YOUTUBE
	}

	private String jsonContent;
	private Format format;

	public RecordJSONMetadata(Format format, String jsonContent) {
		this.jsonContent = jsonContent;
		this.format = format;
	}

	public String getJsonContent() {
		return jsonContent;
	}

	public String getFormat() {
		switch (format) {
		case JSON_UNKNOWN:
			return "JSON-UNKNOWN";
		case JSONLD_UNKNOWN:
			return "JSONLD-UNKNOWN";
		case XML_UNKNOWN:
			return "XML-UNKNOWN";
		case JSON_EDM:
			return "JSON-EDM";
		case JSONLD_EDM:
			return "JSONLD-EDM";
		case XML_EDM:
			return "XML-EDM";
		case JSONLD_DPLA:
			return "JSONLD-DPLA";
		case JSON_NLA:
			return "JSON-NLA";
		case XML_NLA:
			return "XML-NLA";
		case JSON_DNZ:
			return "JSON-DNZ";
		case XML_DNZ:
			return "XML-DNZ";
		case JSON_YOUTUBE:
			return "JSON-YOUTUBE";
		default:
			return "UKNOWN";

		}
	}

}
