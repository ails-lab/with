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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import model.resources.WithResource;

public class ResourcesListImporter extends SourceImporter{

	private JsonContextRecordFormatReader itemReader;
	
	public ResourcesListImporter(JsonContextRecordFormatReader itemReader) {
		super();
		this.itemReader = itemReader;
	}
	@Override
	public List<WithResource<?,?>> process(JsonNode items) {
		List<WithResource<?,?>> res = new ArrayList<>();
		for (JsonNode jsonNode : items) {
			WithResource object = itemReader.fillObjectFrom(jsonNode);
			throwResource(object);
			res.add(object);
		}
		return res;
	}
	public JsonContextRecordFormatReader getItemReader() {
		return itemReader;
	}
	public void setItemReader(JsonContextRecordFormatReader itemReader) {
		this.itemReader = itemReader;
	}
	
	
	
}