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
public class OldLiteralOrResource extends OldLiteral {
	
	public static class OldResource {
		ResourceType uriType;
		String uri;
		
		public OldResource() {
		}
		
		public OldResource(ResourceType uriType, String uri) {
			this.uriType = uriType;
			this.uri = uri;
		}
	}
	
	private OldResource resource;
	

	public OldLiteralOrResource() {
		super();
	}

	public OldLiteralOrResource(ResourceType resourceType, String uri) {
		this.resource = new OldResource(resourceType, uri);
	}

	public OldLiteralOrResource(String label) {
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
		this.resource = new OldResource(resourceType, uri);

	}
	
	
	public static OldLiteralOrResource build(String str){
		if (Utils.isValidURL(str)){
			return new OldLiteralOrResource(ResourceType.uri, str);
		}
		return new OldLiteralOrResource(str);
	}
}