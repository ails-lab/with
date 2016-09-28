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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import annotators.Vocabulary;

import com.fasterxml.jackson.databind.JsonNode;

public class AnnotatorConfig {
	private Class<? extends Annotator> annotator;
	private Map<String, Object> props;
	
	public AnnotatorConfig(Class<? extends Annotator> annotator, Map<String, Object> props) {
		this.setAnnotatorClass(annotator);
		this.setProps(props);
	}

	public Class<? extends Annotator> getAnnotatorClass() {
		return annotator;
	}

	public void setAnnotatorClass(Class<? extends Annotator> annotator) {
		this.annotator = annotator;
	}

	public Map<String, Object> getProps() {
		return props;
	}

	public void setProps(Map<String, Object> props) {
		this.props = props;
	}
	
	public String toString() {
		return annotator.getName() + " -- " + (props != null ? props.toString() : "null");
	}
	
	public static List<AnnotatorConfig> createAnnotationConfigs(JsonNode json) {
		List<AnnotatorConfig> annConfigs = new ArrayList<>();
		
		JsonNode vocs = json.get("vocabulary");
		if (vocs != null) {
			Map<Class<? extends Annotator>, Set<Vocabulary>> annotatorMap = new HashMap<>();

			for (Iterator<JsonNode> iter = vocs.elements(); iter.hasNext();) {
				Vocabulary voc = Vocabulary.getVocabulary(iter.next().asText());
				Class<? extends Annotator> annClass = voc.getAnnotator();
				
				Set<Vocabulary> vocSet = annotatorMap.get(annClass);
				if (vocSet == null) {
					vocSet = new HashSet<Vocabulary>();
					annotatorMap.put(annClass, vocSet);
				}
				
				vocSet.add(voc);
			}
			
			for (Map.Entry<Class<? extends Annotator>, Set<Vocabulary>> entry : annotatorMap.entrySet()) {
				Map<String, Object> props = new HashMap<>();
				props.put(DictionaryAnnotator.VOCABULARIES, entry.getValue());
				
				annConfigs.add(new AnnotatorConfig(entry.getKey(), props));
			}
		}
		
		JsonNode ners = json.get("ner");
		if (ners != null) {
			Set<Class<? extends Annotator>> annotatorMap = new HashSet<>();
			
			for (Iterator<JsonNode> iter = ners.elements(); iter.hasNext();) {
				Vocabulary voc = Vocabulary.getVocabulary(iter.next().asText());
				Class<? extends Annotator> annClass = voc.getAnnotator();
				
				annotatorMap.add(annClass);
			}
			
			for (Class<? extends Annotator> entry : annotatorMap) {
				annConfigs.add(new AnnotatorConfig(entry, null));
			}
		}
		
		return annConfigs;
	}
	
}