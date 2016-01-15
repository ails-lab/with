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


package model.basicDataTypes;

import org.apache.commons.validator.routines.UrlValidator;
import org.mongodb.morphia.annotations.Embedded;

import sources.core.Utils;

@Embedded
public class LiteralOrResource extends Literal {
	
	public static class Resource {
		ResourceType uriType;
		String uri;
		
		public Resource() {
		}
		
		public Resource(ResourceType uriType, String uri) {
			this.uriType = uriType;
			this.uri = uri;
		}
	}
	
	private Resource resource;
	

	public LiteralOrResource() {
		super();
	}

	public LiteralOrResource(ResourceType resourceType, String uri) {
		this.resource = new Resource(resourceType, uri);
	}

	public LiteralOrResource(String label) {
		super(label);
	}

	// etc etc
	public String getResource(ResourceType resourceType) {
		if (resource.uriType.equals(resourceType))
			return resource.uri;
		else
			return "";
	}

	public void setResource(ResourceType resourceType, String uri ) {
		this.resource = new Resource(resourceType, uri);

	}
	
	
	public static LiteralOrResource build(String str){
		if (Utils.isValidURL(str)){
			return new LiteralOrResource(ResourceType.uri, str);
		}
		return new LiteralOrResource(str);
	}
}