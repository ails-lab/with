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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.utils.IndexType;

import utils.Deserializer;
import utils.Serializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import model.basicDataTypes.Language;
import model.basicDataTypes.Literal;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.MultiLiteralOrResource;
import model.basicDataTypes.WithAccess;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Indexes({
	@Index(fields = @Field(value = "administrative.externalId", type = IndexType.ASC), options = @IndexOptions()),
	@Index(fields = @Field(value = "semantic.vocabulary.name", type = IndexType.ASC), options = @IndexOptions()),
	@Index(fields = @Field(value = "semantic.uri", type = IndexType.ASC), options = @IndexOptions())
})
@Entity("ThesaurusObject")
public class ThesaurusObject {

	@JsonInclude(value = JsonInclude.Include.NON_NULL)
	@Embedded
	public static class SKOSTerm {
		protected String uri;
		protected String type;

		protected Literal prefLabel;
		protected MultiLiteral altLabel;
		
		protected String thesaurus;
		protected String version;

		public SKOSTerm() {	}

		public SKOSTerm(String uri, String type, Literal prefLabel, MultiLiteral altLabel, String thesaurus, String version) {
			this.uri = uri;
			this.type = type;
			this.prefLabel = prefLabel;
			this.altLabel = altLabel;
			
			this.thesaurus = thesaurus;
			this.version = version;

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
		
		public String getThesaurus() {
			return thesaurus;
		}

		public void setThesaurus(String thesaurus) {
			this.thesaurus = thesaurus;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
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

		public int hashCode() {
			return uri.hashCode();
		}

		public boolean equals(Object obj) {
			if (obj instanceof SKOSTerm) {
				return uri.equals(((SKOSTerm)obj).uri);
			}

			return false;
		}

		public String toString() {
			if (prefLabel != null) {
				return prefLabel.get("en");
			} else {
				return "";
			}
		}
	}

	@JsonInclude(value = JsonInclude.Include.NON_NULL)
	@Embedded
	public static class SKOSVocabulary {

		private String name;
		private String version;
		
		public SKOSVocabulary() {}

		public SKOSVocabulary(String name, String version) {
			this.name = name;
			this.version = version;
		}
		
		public String getName() {
			return name;
		}
		
		public String getVersion() {
			return version;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public void setVersion(String version) {
			this.version = version;
		}
		
	}

	@JsonInclude(value = JsonInclude.Include.NON_NULL)
	@Embedded
	public static class SKOSSemantic extends SKOSTerm {

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
		
		private SKOSVocabulary vocabulary;
		
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
		
		public SKOSVocabulary getVocabulary() {
			return vocabulary;
		}

		public void setVocabulary(SKOSVocabulary vocabulary) {
			this.vocabulary = vocabulary;
		}
	}


	@Embedded
	public static class SKOSAdmin {

		@JsonSerialize(using = Serializer.DateSerializer.class)
		@JsonDeserialize(using = Deserializer.DateDeserializer.class)
		private Date created;

		@JsonSerialize(using = Serializer.DateSerializer.class)
		@JsonDeserialize(using = Deserializer.DateDeserializer.class)
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
			if (this.lastModified == null) {
				this.lastModified = created;
			}
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
	private ObjectId dbId;

	private SKOSAdmin administrative;
	private SKOSSemantic semantic;

	public ThesaurusObject() {
		super();
		this.administrative = new SKOSAdmin();
		this.semantic = new SKOSSemantic();
	}
	
	public ThesaurusObject(ObjectId id) {
		super();
		this.administrative = new SKOSAdmin();
		this.semantic = new SKOSSemantic();
		this.setDbId(id);
	}

	public ObjectId getDbId() {
		return dbId;
	}

	public void setDbId(ObjectId dbid) {
		this.dbId = dbid;
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

	public Map<String, Object> transform() {

//		Map<String, Object> map = new TreeMap<>();
//		Map<String, List<String>> langAcc = new HashMap<>();
//
//		map.put("uri", semantic.uri);
//		
//		if (semantic.vocabulary != null) {
//			map.put("vocabulary", semantic.vocabulary.getName());
//		}
//
//		if (semantic.prefLabel != null) {
//			List<String> allPrefLabels = new ArrayList<>();
//
//			Map<String, String> prefLabel = new HashMap<>();
//
//			for (Map.Entry<String, String> entry : semantic.prefLabel.entrySet()) {
//				String k = entry.getKey();
//				String v = entry.getValue();
//				if (!k.equals(Language.DEFAULT) && (v != null) && (v.length() > 0)) {
//					prefLabel.put(k, v);
//					allPrefLabels.add(v);
//					addToLangAll(langAcc, k, v);
//				}
//			}
//
//			if (!prefLabel.isEmpty()) {
//				map.putAll(flattenLiteralMap("prefLabel", prefLabel));
//				map.put("prefLabel_all", allPrefLabels);
//			}
//		}
//
//		if (semantic.altLabel != null) {
//			List<String> allAltLabels = new ArrayList<>();
//
//			Map<String, List<String>> altLabel = new HashMap<>();
//			for (Map.Entry<String, List<String>> entry : semantic.altLabel.entrySet()) {
//				String k = entry.getKey();
//				List<String> v = entry.getValue();
//				if (!k.equals(Language.DEFAULT) && (v != null) && (v.size() > 0)) {
//					ArrayList<String> vc = new ArrayList<>();
//
//					for (String vv : v) {
//						if ((vv != null) && (vv.length() > 0)) {
//							vc.add(vv);
//							allAltLabels.add(vv);
//							addToLangAll(langAcc, k, vv);
//						}
//					}
//
//					if (vc.size() > 0) {
//						altLabel.put(k, vc);
//					}
//				}
//			}
//
//			if (!altLabel.isEmpty()) {
//				map.putAll(flattenMultiLiteralMap("altLabel", altLabel));
//				map.put("altLabel_all", allAltLabels);
//			}
//		}
//
//		if (semantic.broader != null) {
//			List<String> broaderUris = new ArrayList<>();
//
//			Map<String, List<String>> broaderPrefLabelAcc = new HashMap<>();
//			Map<String, List<String>> broaderAltLabelAcc = new HashMap<>();
//			List<String> allBroaderPrefLabel = new ArrayList<>();
//			List<String> allBroaderAltLabel = new ArrayList<>();
//
//			for (SKOSTerm broader : semantic.broader) {
//				broaderUris.add(broader.getUri());
//
//				if (broader.prefLabel != null) {
//					for (Map.Entry<String, String> e : broader.prefLabel.entrySet()) {
//						String k = e.getKey();
//						String v = e.getValue();
//
//						if (!k.equals(Language.DEFAULT) && (v != null) && (v.length() > 0)) {
//							allBroaderPrefLabel.add(v);
//							addToLangAll(broaderPrefLabelAcc, k, v);
//							addToLangAll(langAcc, k, v);
//						}
//					}
//				}
//
//				if (broader.altLabel != null) {
//					for (Map.Entry<String, List<String>> e : broader.altLabel.entrySet()) {
//						String k = e.getKey();
//						List<String> v = e.getValue();
//
//						if (!k.equals(Language.DEFAULT) && (v != null) && (v.size() > 0)) {
//							for (String vv : e.getValue()) {
//								if ((vv != null) && (vv.length() > 0)) {
//									allBroaderAltLabel.add(vv);
//									addToLangAll(broaderAltLabelAcc, k, vv);
//									addToLangAll(langAcc, k, vv);
//								}
//							}
//						}
//					}
//				}
//			}
//
//			if (broaderUris.size() > 0) {
//				map.put("broaderUri", broaderUris);
//			}
//
//			if (broaderAltLabelAcc.size() > 0) {
//				map.putAll(flattenMultiLiteralMap("broaderPrefLabel", broaderPrefLabelAcc));
//			}
//
//			if (allBroaderPrefLabel.size() > 0) {
//				map.put("broaderPrefLabel_all", allBroaderPrefLabel);
//			}
//
//			if (broaderAltLabelAcc.size() > 0) {
//				map.putAll(flattenMultiLiteralMap("broaderAltLabel", broaderAltLabelAcc));
//			}
//
//			if (allBroaderAltLabel.size() > 0) {
//				map.put("broaderAltLabel_all", allBroaderAltLabel);
//			}
//		}
//
//		if (semantic.broaderTransitive != null) {
//			List<String> broaderUris = new ArrayList<>();
//
//			Map<String, List<String>> broaderPrefLabelAcc = new HashMap<>();
//			Map<String, List<String>> broaderAltLabelAcc = new HashMap<>();
//			List<String> allBroaderPrefLabel = new ArrayList<>();
//			List<String> allBroaderAltLabel = new ArrayList<>();
//
//			for (SKOSTerm broader : semantic.broaderTransitive) {
//				broaderUris.add(broader.getUri());
//
//				if (broader.prefLabel != null) {
//					for (Map.Entry<String, String> e : broader.prefLabel.entrySet()) {
//						String k = e.getKey();
//						String v = e.getValue();
//
//						if (!k.equals(Language.DEFAULT) && (v != null) && (v.length() > 0)) {
//							allBroaderPrefLabel.add(v);
//							addToLangAll(broaderPrefLabelAcc, k, v);
//							addToLangAll(langAcc, k, v);
//						}
//					}
//				}
//
//				if (broader.altLabel != null) {
//					for (Map.Entry<String, List<String>> e : broader.altLabel.entrySet()) {
//						String k = e.getKey();
//						List<String> v = e.getValue();
//
//						if (!k.equals(Language.DEFAULT) && (v != null) && (v.size() > 0)) {
//							for (String vv : e.getValue()) {
//								if ((vv != null) && (vv.length() > 0)) {
//									allBroaderAltLabel.add(vv);
//									addToLangAll(broaderAltLabelAcc, k, vv);
//									addToLangAll(langAcc, k, vv);
//								}
//							}
//						}
//					}
//				}
//			}
//
//			if (broaderUris.size() > 0) {
//				map.put("broaderTransitiveUri", broaderUris);
//			}
//
//			if (broaderAltLabelAcc.size() > 0) {
//				map.putAll(flattenMultiLiteralMap("broaderTransitivePrefLabel", broaderPrefLabelAcc));
//			}
//
//			if (allBroaderPrefLabel.size() > 0) {
//				map.put("broaderTransitivePrefLabel_all", allBroaderPrefLabel);
//			}
//
//			if (broaderAltLabelAcc.size() > 0) {
//				map.putAll(flattenMultiLiteralMap("broaderTransitiveAltLabel", broaderAltLabelAcc));
//			}
//
//			if (allBroaderAltLabel.size() > 0) {
//				map.put("broaderTransitiveAltLabel_all", allBroaderAltLabel);
//			}
//		}
//
//		if ((semantic.inCollections != null) && (semantic.inCollections.size() > 0)) {
//			map.put("inCollections", semantic.inCollections);
//		}
//
//		if ((semantic.inSchemes != null) && (semantic.inSchemes.size() > 0)) {
//			map.put("inSchemes", semantic.inSchemes);
//		}
//		
//		return map;
		
		
		
//		JsonNode m = mediaMapConverter(rr.getMedia());
//		JsonNode jn = withAccessConverter(rr.getAdministrative());


		/*
		 *
		 */

		ObjectMapper mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addSerializer(MultiLiteral.class, new Serializer.MUltiliteralSerializerForElastic());
		module.addSerializer(MultiLiteralOrResource.class, new Serializer.MUltiliteralSerializerForElastic());
		module.addSerializer(Literal.class, new Serializer.LiteralSerializerForElastic());
		mapper.registerModule(module);
		mapper.setSerializationInclusion(Include.NON_NULL);

		JsonNode json = mapper.valueToTree(this.semantic);

		((ObjectNode)json).remove("scopeNote");
		((ObjectNode)json).remove("narrower");
		((ObjectNode)json).remove("topConcepts");
		((ObjectNode)json).remove("members");
		
		return mapper.convertValue(json, Map.class);
	}

//	private Map<String, Object> flattenLiteralMap(String field, Map<String, String> values) {
//		Map<String, Object> res = new HashMap<>();
//		for (Map.Entry<String, String> entry : values.entrySet()) {
//			res.put(field + "." + entry.getKey(), entry.getValue());
//		}
//
//		return res;
//	}
//
//	private Map<String, Object> flattenMultiLiteralMap(String field, Map<String, List<String>> values) {
//		Map<String, Object> res = new HashMap<>();
//		for (Map.Entry<String, List<String>> entry : values.entrySet()) {
//			res.put(field + "." + entry.getKey(), entry.getValue());
//		}
//
//		return res;
//	}
//
//
//	private static void addToLangAll(Map<String, List<String>> map, String lang, String value) {
//		List<String> array = map.get(lang);
//
//		if (array == null) {
//			array = new ArrayList<>();
//			map.put(lang, array);
//		}
//
//		array.add(value);
//	}
	
}
