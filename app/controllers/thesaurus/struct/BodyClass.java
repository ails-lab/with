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


package controllers.thesaurus.struct;

import model.basicDataTypes.MultiLiteral;

public class BodyClass {
	public String uri;
	public MultiLiteral label;
	public int counter;
	
	public BodyClass(String uri, MultiLiteral label) {
		this.uri = uri;
		this.label = label;
	}

	public int hashCode() {
		return uri.hashCode();
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof BodyClass)) {
			return false;
		}
		
		return uri.equals(((BodyClass)obj).uri);
	}
}