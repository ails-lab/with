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


package vocabularies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.typesafe.config.ConfigValue;

import db.DB;

public class Vocabulary {

	public static enum VocabularyType {
		CUSTOM_THESAURUS,
		THESAURUS,
		REFERENCE,
		DATA
	}
	
	static {
		loadVocabularies();
	}

	private String name;
	private String label;
	private VocabularyType type;
	private String version;
	
	private static List<Vocabulary> vocabularies;
	
	public static List<Vocabulary> getVocabularies() {
		return vocabularies;
	}
	
	public static void loadVocabularies() {
		vocabularies = new ArrayList<>();
		
		for (Entry<String, ConfigValue> voc : DB.getConf().getConfig("vocabularies").root().entrySet()) {
			String name = voc.getKey();
			VocabularyType type = null;
			String label = "";
			String version = ""; 

			for (Map.Entry<String, String> entry : ((Map<String,String>)voc.getValue().unwrapped()).entrySet()) {
				String key = entry.getKey();
				String val = entry.getValue();
				
				if (key.equals("type")) {
					type = getVocabularyType(val);
				} else if (key.equals("label")) {
					label = val;
				} else if (key.equals("version")) {
					version = val;
				}
			};
			
			vocabularies.add(new Vocabulary(name, label, type, version));
			
			Collections.sort(vocabularies, new Comparator<Vocabulary>() {

				@Override
				public int compare(Vocabulary arg0, Vocabulary arg1) {
					return arg0.name.compareTo(arg1.name);
				}
				
			});
			
		}
	}
	

	
	public Vocabulary(String name, String label, VocabularyType type, String version) {
		this.name = name;
		this.setType(type);
		this.setLabel(label);
		this.setVersion(version);
	}
	
	public String getName() {
		return name;
	}
	
	public static Vocabulary getVocabulary(String name){
		for (Vocabulary voc : vocabularies) {
			if (voc.name.equalsIgnoreCase(name)) {
				return voc;
			} 
		}
		
		return null;
	}
	
	public static VocabularyType getVocabularyType(String name){
		for (VocabularyType type : VocabularyType.values()) {
			if (type.name().equalsIgnoreCase(name)) {
				return type;
			} 
		}
		
		return null;
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

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
	
	public String toString() {
		return name;
	}
}
