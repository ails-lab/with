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

public class RecordJSONMetadata {

	public enum Format {
		JSON_WITH,
		JSON_UNKNOWN, JSONLD_UNKNOWN, XML_UNKNOWN, 
		JSON_EDM, JSONLD_EDM, XML_EDM,
		JSONLD_DPLA,
		JSON_NLA, XML_NLA, 
		JSON_DNZ, XML_DNZ,
		JSON_YOUTUBE, JSON_RIJ, JSON_Historypin
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
	
	public void setJsonContent(String jsonContent) {
		this.jsonContent = jsonContent;
	}

	public boolean hasFormat(Format format){
		return this.format.equals(format);
	}

	public String getFormat() {
		return (String.valueOf(format).replaceAll("_", "-"));
	}

}
