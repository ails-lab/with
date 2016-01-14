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

import sources.FilterValuesMap;
import sources.utils.JsonContextRecord;
import model.resources.CulturalObject;
import model.resources.WithResource;

import com.fasterxml.jackson.databind.JsonNode;

public abstract class JsonContextRecordFormatReader<T extends WithResource> {
	
	protected T object;
	private FilterValuesMap map;
	
	public T fillObjectFrom(String text) {
		JsonContextRecord rec = new JsonContextRecord(text);
		return fillObjectFrom(rec);
	}
	
	protected T fillObjectFrom(JsonNode text) {
		JsonContextRecord rec = new JsonContextRecord(text);
		return fillObjectFrom(rec);
	}
	
	protected abstract T fillObjectFrom(JsonContextRecord text);
	
	public T readObjectFrom(JsonNode text){
		return fillObjectFrom(text);
	}

	public FilterValuesMap getMap() {
		return map;
	}

	public void setMap(FilterValuesMap map) {
		this.map = map;
	}

}
