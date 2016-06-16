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

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import model.resources.WithResource;
import play.Logger;
import play.Logger.ALogger;
import sources.utils.JsonContextRecord;
import sources.utils.Listeners;
import sources.utils.Listeners.ObjectListeners;

public abstract class SourceImporter {
	public static final ALogger log = Logger.of( SourceImporter.class );
	
	
	private Listeners.ObjectListeners<WithResource> resourceListeners;

	public Listeners.ObjectListeners<WithResource> getResourceListeners() {
		return resourceListeners;
	}

	public SourceImporter() {
		super();
		resourceListeners = new ObjectListeners<>();
	}
	
	protected void throwResource(WithResource obj){
		getResourceListeners().accept(obj);
	}
	
	public abstract List<WithResource<?,?>> process(JsonNode items);

	public List<WithResource<?,?>> process(File jsonFile) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return process(mapper.readTree(jsonFile));
		} catch (JsonProcessingException e) {
			log.error("",e);
		} catch (IOException e) {
			log.error("",e);
		}
		return null;
	}
}
