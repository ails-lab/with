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

import org.mongodb.morphia.annotations.Embedded;

@Embedded
public class OldMultiLiteralOrResource extends OldMultiLiteral {

	// private OldLiteralOrResource.OldResource resource;
	//
	// public OldMultiLiteralOrResource() {
	// super();
	// }

	// public OldMultiLiteralOrResource(ResourceType resourceType, String uri) {
	// this.resource = new OldLiteralOrResource.OldResource(resourceType, uri);
	// }
	//
	// public OldMultiLiteralOrResource(String label) {
	// super(label);
	// }
	//
	// // etc etc
	// public String getResource(ResourceType resourceType) {
	// if (resource.uriType.equals(resourceType))
	// return resource.uri;
	// else
	// return "";
	// }
	//
	// public void setResource(ResourceType resourceType, String uri ) {
	// this.resource = new OldLiteralOrResource.OldResource(resourceType, uri);
	//
	// }
}