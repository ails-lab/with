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


package annotators;

public class Lexicon {

	public static enum VocabularyType {
		THESAURUS,
		RESOURCE
	}
	
	public static enum Vocabulary {
		AAT("aat", "Art & Architecture Thesaurus", VocabularyType.THESAURUS, DictionaryAnnotator.class),
		DBPEDIA_ONTOLOGY("dbo", "DBPedia Ontology", VocabularyType.THESAURUS, DictionaryAnnotator.class), 
		DBPEDIA_RESOURCE("dbr", "DBPedia Resources", VocabularyType.RESOURCE, DBPediaAnnotator.class), 
		DBPEDIA_RESOURCE2("dbx", "DBPedia Resources", VocabularyType.RESOURCE, DBPediaAnnotator.class),
		GEMET("gemet", "GEMET Thesaurus", VocabularyType.THESAURUS, DictionaryAnnotator.class), 
		EUSCREENXL("euscreenxl", "EuscreenXL Thesaurus", VocabularyType.THESAURUS, DictionaryAnnotator.class),
		FASHION("fashion", "Fashion Thesaurus", VocabularyType.THESAURUS, DictionaryAnnotator.class),
		HORNBOSTEL_SACHS("hornbostelsachs", "Hornbostel Sachs Thesaurus", VocabularyType.THESAURUS, DictionaryAnnotator.class),
		MIMO("mimo", "Musical Instrument Museums Online Thesaurus", VocabularyType.THESAURUS, DictionaryAnnotator.class),
		NERD("nerd", "Named Entity Recognition and Disambiguation Ontology", VocabularyType.THESAURUS, NLPAnnotator.class),
		PHOTOGRAPHY("photo", "Photography Thesaurus", VocabularyType.THESAURUS, DictionaryAnnotator.class),
		PARTAGE_PLUS("partageplus", "Partage Plus Thesaurus", VocabularyType.THESAURUS, DictionaryAnnotator.class);
//		WORDNET30("wordnet30", "Wordnet 3.0", null),
//		WORDNET31("wordnet31", "Wordnet 3.1", null);

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

}
