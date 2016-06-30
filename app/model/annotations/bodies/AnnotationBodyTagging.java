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


package model.annotations.bodies;

import java.util.ArrayList;

import annotators.Annotator;
import annotators.DBPediaAnnotator;
import annotators.DictionaryAnnotator;
import annotators.NERAnnotator;
import model.basicDataTypes.MultiLiteral;

public class AnnotationBodyTagging extends AnnotationBody {
	
	/**
	 * The uri of the tag.
	 */
	private String uri;
	
	/**
	 * The tag type. This value should be a uri either of a class of of a property.
	 */
	private String tagType;
	
	/**
	 * The pref label of the URI tag, that will be used for display purposes.
	 */
	private MultiLiteral label;
	
	
	/**
	 * The types of the uri. This value should be a uri of the class to which the tag belongs to.
	 * i.e. http://dbpedia.org/ontology/Building  etc
	 */
	private ArrayList<String> uriType;
	
	/**
	 * A value taken from an enumeration that includes all the vocabularies used in WITH and by the
	 * annotator generators.
	 */
	private Vocabulary uriVocabulary;
	
	/**
	 * This should be only populated when the tag of the annotation is not a URI but a text values.
	 * When this value exists the uri, uriLabel, uriType and uriVocabulary should be null.
	 */
	/*private MultiLiteral text;*/
	
//	public static enum Vocabulary {
//		DBPEDIA_ONTOLOGY, DBPEDIA_RESOURCE, MIMO, FASHION, GEMET, AAT, PARTAGE, PHOTO, WORDNET30, WORDNET31
//	}

	public static enum VocabularyType {
		THESAURUS,
		NER,
		CLASS
	}
	
	public static enum Vocabulary {
		AAT("aat", "Art & Architecture Thesaurus", VocabularyType.THESAURUS, DictionaryAnnotator.class),
		DBPEDIA_ONTOLOGY("dbpedia", "DBPedia Ontology", VocabularyType.THESAURUS, DictionaryAnnotator.class), 
		DBPEDIA_RESOURCE("dbr", "DBPedia Linking", VocabularyType.NER, DBPediaAnnotator.class), 
		GEMET("gemet", "GEMET Thesaurus", VocabularyType.THESAURUS, DictionaryAnnotator.class), 
		EUSCREENXL("euscreenxl", "EuscreenXL Thesaurus", VocabularyType.THESAURUS, DictionaryAnnotator.class),
		FASHION("fashion", "Fashion Thesaurus", VocabularyType.THESAURUS, DictionaryAnnotator.class),
		HORNBOSTEL_SACHS("hornbostelsachs", "Hornbostel Sachs Thesaurus", VocabularyType.THESAURUS, DictionaryAnnotator.class),
		MIMO("mimo", "MIMO Thesaurus", VocabularyType.THESAURUS, DictionaryAnnotator.class),
		NERD("nerd", "Named Entity Tagging", VocabularyType.NER, NERAnnotator.class),
		PHOTOGRAPHY("photography", "Photography Thesaurus", VocabularyType.THESAURUS, DictionaryAnnotator.class),
		PARTAGE_PLUS("partageplus", "Partage Plus Thesaurus", VocabularyType.THESAURUS, DictionaryAnnotator.class),
		WORDNET30("wordnet30", "Wordnet 3.0", null, null),
		WORDNET31("wordnet31", "Wordnet 3.1", null, null);
		
		private String name;
		private String label;
		private String[] alt;
		private VocabularyType type;
		private Class<? extends Annotator> annotator;
		
		Vocabulary(String name, String label, VocabularyType type, Class<? extends Annotator> annotator, String... alt) {
			this.name = name;
			this.setType(type);
			this.setLabel(label);
			this.setAnnotator(annotator);
			this.alt = alt;
		}
		
		public String getName() {
			return name;
		}
		
		public static Vocabulary getVocabulary(String name){
			for (Vocabulary voc : Vocabulary.values()) {
				if (voc.name.equals(name)) {
					return voc;
				} else if (voc.alt != null) {
					for (String s : voc.alt) {
						if (s.equals(name)) {
							return voc;
						}
					}
				}
			}
			
			return null;
		}

		public Class<? extends Annotator> getAnnotator() {
			return annotator;
		}

		public void setAnnotator(Class<? extends Annotator> annotator) {
			this.annotator = annotator;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public VocabularyType getType() {
			return type;
		}

		public void setType(VocabularyType type) {
			this.type = type;
		}

	}
	
	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getTagType() {
		return tagType;
	}

	public void setTagType(String tagType) {
		this.tagType = tagType;
	}

	public MultiLiteral getLabel() {
		return label;
	}

	public void setLabel(MultiLiteral label) {
		this.label = label;
	}

	public ArrayList<String> getUriType() {
		return uriType;
	}

	public void setUriType(ArrayList<String> uriType) {
		this.uriType = uriType;
	}

	public Vocabulary getUriVocabulary() {
		return uriVocabulary;
	}

	public void setUriVocabulary(Vocabulary uriVocabulary) {
		this.uriVocabulary = uriVocabulary;
	}

	/*public MultiLiteral getText() {
		return text;
	}

	public void setText(MultiLiteral text) {
		this.text = text;
	}*/

	
}
