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


package model.resources;

import java.util.Date;
import java.util.List;


import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import utils.Serializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import model.basicDataTypes.Literal;
import model.basicDataTypes.MultiLiteral;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Entity("ThesaurusObject")
public class ThesaurusObject {

	@JsonInclude(value = JsonInclude.Include.NON_NULL)
	@Embedded
	public static class SKOSTerm {
		private String uri;
		private String type;

		private Literal prefLabel;
		private MultiLiteral altLabel;

		public SKOSTerm() {}
		
		public SKOSTerm(String uri, String type, Literal prefLabel, MultiLiteral altLabel) {
			this.uri = uri;
			this.type = type;
			this.prefLabel = prefLabel;
			this.altLabel = altLabel;
		}
		
		public String getUri() {
			return uri;
		}

		public void setUri(String uri) {
			this.uri = uri;
		}
		
		public String getType() {
			return type;
		}
		
		public void setType(String type) {
			this.type = type;
		}
		
		public Literal getPrefLabel() {
			return prefLabel;
		}
		
		public void setPrefLabel(Literal prefLabel) {
			this.prefLabel = prefLabel;
		}
		
		public MultiLiteral getAltLabel() {
			return altLabel;
		}
		
		public void setAltLabel(MultiLiteral altLabel) {
			this.altLabel = altLabel;
		}
	}
	
	@JsonInclude(value = JsonInclude.Include.NON_NULL)
	@Embedded
	public static class SKOSSemantic {
		private String uri;
		private String type;

		private Literal prefLabel;
		private MultiLiteral altLabel;

		private Literal scopeNote;
		private List<SKOSTerm> broader;
		private List<SKOSTerm> narrower;
		private List<SKOSTerm> broaderTransitive;
		private List<SKOSTerm> related;
		
		private List<SKOSTerm> topConcepts;
		private List<SKOSTerm> members;
		
		private List<String> inCollections;
		private List<String> inSchemes;
		private List<String> exactMatch;
		
		public String getUri() {
			return uri;
		}
		
		public void setUri(String uri) {
			this.uri = uri;
		}
		
		public String getType() {
			return type;
		}
		
		public void setType(String type) {
			this.type = type;
		}
		
		public Literal getPrefLabel() {
			return prefLabel;
		}
		
		public void setPrefLabel(Literal prefLabel) {
			this.prefLabel = prefLabel;
		}
		
		public MultiLiteral getAltLabel() {
			return altLabel;
		}
		
		public void setAltLabel(MultiLiteral altLabel) {
			this.altLabel = altLabel;
		}
		
		public Literal getScopeNote() {
			return scopeNote;
		}
		
		public void setScopeNote(Literal scopeNote) {
			this.scopeNote = scopeNote;
		}
		
		public List<SKOSTerm> getBroader() {
			return broader;
		}
		
		public void setBroader(List<SKOSTerm> broader) {
			this.broader = broader;
		}
		
		public List<SKOSTerm> getNarrower() {
			return narrower;
		}
		
		public void setNarrower(List<SKOSTerm> narrower) {
			this.narrower = narrower;
		}
		
		public List<SKOSTerm> getBroaderTransitive() {
			return broaderTransitive;
		}
		
		public void setBroaderTransitive(List<SKOSTerm> broaderTransitive) {
			this.broaderTransitive = broaderTransitive;
		}
		
		public List<SKOSTerm> getRelated() {
			return related;
		}
		
		public void setRelated(List<SKOSTerm> related) {
			this.related = related;
		}
		
		public List<SKOSTerm> getTopConcepts() {
			return topConcepts;
		}
		
		public void setTopConcepts(List<SKOSTerm> topConcepts) {
			this.topConcepts = topConcepts;
		}
		
		public List<SKOSTerm> getMembers() {
			return members;
		}
		
		public void setMembers(List<SKOSTerm> members) {
			this.members = members;
		}
		
		public List<String> getInCollections() {
			return inCollections;
		}
		
		public void setInCollections(List<String> inCollections) {
			this.inCollections = inCollections;
		}
		
		public List<String> getInSchemes() {
			return inSchemes;
		}
		
		public void setInSchemes(List<String> inSchemes) {
			this.inSchemes = inSchemes;
		}
		
		public List<String> getExactMatch() {
			return exactMatch;
		}
		
		public void setExactMatch(List<String> exactMatch) {
			this.exactMatch = exactMatch;
		}
	}
	
	@Embedded
	public static class SKOSAdmin {
		private Date created;
		
		private Date lastModified;

		private String externalId;

		public SKOSAdmin() {}
		
		public SKOSAdmin(Date created, Date lastModified, String externalId) {
			this.created = created;
			this.lastModified = lastModified;
			this.externalId = externalId;
		}
		
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

	@Id
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId dbid;

	private SKOSAdmin administrative;
	private SKOSSemantic semantic;
	

	public ThesaurusObject() {
		super();
		this.administrative = new SKOSAdmin();
		this.semantic = new SKOSSemantic();

	}
	
	public SKOSAdmin getAdministrative() {
		return administrative;
	}
	
	public void setAdministrative(SKOSAdmin admin) {
		this.administrative = admin;
	}

	public SKOSSemantic getSemantic() {
		return semantic;
	}

	public void setSemantic(SKOSSemantic semantic) {
		this.semantic = semantic;
	}

	
}
