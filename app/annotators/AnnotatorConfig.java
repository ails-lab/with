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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import vocabularies.Vocabulary;
import annotators.Annotator.AnnotatorDescriptor;
import annotators.Annotator.AnnotatorDescriptor.AnnotatorType;
import annotators.DBPediaAnnotator.Descriptor;

import com.fasterxml.jackson.databind.JsonNode;

public class AnnotatorConfig {
	private AnnotatorDescriptor ad;
	private Map<String, Object> props;
	
	public AnnotatorConfig(AnnotatorDescriptor ad, Map<String, Object> props) {
		this.setAnnotatorDescriptor(ad);
		this.setProps(props);
	}

	public AnnotatorDescriptor getAnnotatorDesctriptor() {
		return ad;
	}

	public void setAnnotatorDescriptor(AnnotatorDescriptor ad) {
		this.ad = ad;
	}

	public Map<String, Object> getProps() {
		return props;
	}

	public void setProps(Map<String, Object> props) {
		this.props = props;
	}
	
	public String toString() {
		return ad.getName() + " -- " + (props != null ? props.toString() : "null");
	}
	
	public static List<AnnotatorConfig> createAnnotationConfigs(JsonNode json) {
		List<AnnotatorConfig> annConfigs = new ArrayList<>();

		Map<AnnotatorDescriptor, Set<Vocabulary>> annotatorMap = new HashMap<>();

		for (Iterator<JsonNode> iter = json.elements(); iter.hasNext();) {
			String entry = iter.next().textValue();
			String[] v = entry.split("/");
			
			AnnotatorDescriptor ad;
			try {
				ad = (AnnotatorDescriptor)Class.forName("annotators." + v[0]).getField("descriptor").get(null);
			
				Map<String, Object> props = new HashMap<>();
				try {
					AnnotatorType type = ad.getType();
					
					if (type == AnnotatorType.LOOKUP || type == AnnotatorType.NER) {
						props.put(Annotator.TEXT_ANNOTATOR, true);
						props.put(Annotator.TEXT_FIELDS, Annotator.textfields);
					} else {
						props.put(Annotator.IMAGE_ANNOTATOR, true);
					}

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				if (v.length == 1) {
					annConfigs.add(new AnnotatorConfig(ad, props));
				} else if (v.length == 2) {
					Vocabulary voc = Vocabulary.getVocabulary(v[1]);
					
					Set<Vocabulary> vocSet = annotatorMap.get(ad);
					if (vocSet == null) {
						vocSet = new HashSet<Vocabulary>();
						annotatorMap.put(ad, vocSet);
						
						props.put(LookupAnnotator.VOCABULARIES, vocSet);
	
						annConfigs.add(new AnnotatorConfig(ad, props));
					}
					
					vocSet.add(voc);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		
		return annConfigs;
	}
	
}