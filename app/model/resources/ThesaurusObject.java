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

import java.util.Date;
import java.util.List;

public class SKOSObject {

	public static class SKOSTerm {
		String uri;
		String type;

		Literal prefLabel;
		MultiLiteral altLabel;
		
	}
	
	public static class SKOSSemantic {
		String uri;
		String type;

		Literal prefLabel;
		MultiLiteral altLabel;

		Literal scopeNote;
		List<SKOSTerm> broader;
		List<SKOSTerm> narrower;
		List<SKOSTerm> broaderTransitive;
		List<SKOSTerm> related;
		
		List<SKOSTerm> topConcepts;
		List<SKOSTerm> members;
		
		List<String> inCollections;
		List<String> inSchemes;
		List<String> exactMatch;
	}
	
	public static class SKOSAdmin {
		Date created;
		Date lastModified;

		String externalId;
		
		public Date getCreated() {
			return created;
		}

		public void setCreated(Date created) {
			this.created = created;
		}

		public Date getLastModified() {
			return lastModified;
		}

		public void setLastModified(Date lastModified) {
			this.lastModified = lastModified;
		}
		
		public String getExternalId() {
			return externalId;
		}

		public void setExternalId(String externalId) {
			this.externalId = externalId;
		}

	}
	
	protected SKOSAdmin administrative;
	
	protected SKOSSemantic semantic;
	
}
