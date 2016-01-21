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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.mongodb.morphia.annotations.Converters;
import utils.Deserializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import db.converters.MultiLiteralConverter;

@Converters(MultiLiteralConverter.class)
public class MultiLiteral extends HashMap<String, List<String>> {

	public MultiLiteral() {
	}

	public MultiLiteral(String label) {
		addLiteral(label);
	}

	public MultiLiteral(Language lang, String label) {
		addLiteral(lang, label);
	}

	public void addLiteral(Language lang, String value) {
		add(lang.toString(), value);
//		if (lang.equals(Language.EN) && !this.containsKey(Language.DEF.toString()))
//			add(Language.DEF.toString(), value);
	}

	public void addLiteral(String value) {
		addLiteral(Language.UNKNOWN, value);
	}

	public List<String> get(Language lang) {
		/*if(Language.ANY.equals(lang)) {
			return this.get(this.keySet().toArray()[0]);

		}
		else*/
			return get(lang.toString());
	}
	
	public void add(String key, String value) {
		if (this.containsKey(key))
			this.get(key).add(value);
		else
			this.put(key, new ArrayList<String>(Arrays.asList(value)));
	}

	public void fillDEF(){
		String defLang = Language.EN.toString();
		if (!containsKey(defLang)){
			int max = 0;
			for (String k : this.keySet()) {
				if (!k.equals(Language.DEF.toString())){
					int m = get(k).size();
					if (max < m){
						max = m;
						defLang = k;
					}
				}
			}
		}
		for (String d : get(defLang)) {
			add(Language.DEF.toString(), d);
		}
	}
}
