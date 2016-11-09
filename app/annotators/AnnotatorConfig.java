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

import vocabularies.Vocabulary;

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

		Map<Class<? extends Annotator>, Set<Vocabulary>> annotatorMap = new HashMap<>();

		for (Iterator<JsonNode> iter = json.elements(); iter.hasNext();) {
			String entry = iter.next().textValue();
			String[] v = entry.split("/");
			
			Class<? extends Annotator> annotator;
			try {
				annotator = (Class<? extends Annotator>)Class.forName("annotators." + v[0]);
			
				if (v.length == 1) {
					annConfigs.add(new AnnotatorConfig(annotator, null));
				} else if (v.length == 2) {
					Vocabulary voc = Vocabulary.getVocabulary(v[1]);
					
					Set<Vocabulary> vocSet = annotatorMap.get(annotator);
					if (vocSet == null) {
						vocSet = new HashSet<Vocabulary>();
						annotatorMap.put(annotator, vocSet);
						
						Map<String, Object> props = new HashMap<>();
						props.put(LookupAnnotator.VOCABULARIES, vocSet);
	
						annConfigs.add(new AnnotatorConfig(annotator, props));
					}
					
					vocSet.add(voc);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

		}
		
		return annConfigs;
	}
	
}