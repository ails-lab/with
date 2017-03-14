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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mongodb.morphia.annotations.Converters;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import db.converters.MultiLiteralConverter;
import utils.Deserializer.MultiLiteralDesiarilizer;

@Converters(MultiLiteralConverter.class)
@JsonDeserialize(using = MultiLiteralDesiarilizer.class)
public class MultiLiteral extends HashMap<String, List<String>> implements
		ILiteral {

	public MultiLiteral() {
	}

	public MultiLiteral(String label) {
		addLiteral(label);
	}

	public MultiLiteral(Language lang, String label) {
		addLiteral(lang, label);
	}

	public MultiLiteral(Literal lit) {
		for (Map.Entry<String, String> entry : lit.entrySet()) {
			add(entry.getKey(), entry.getValue());
		}
	}
	
	@Override
	public void addLiteral(Language lang, String value) {
		add(lang.getDefaultCode(), value);
	}

	public void addMultiLiteral(Language lang, List<String> values) {
		for (String value : values) {
			addLiteral(lang, value);
		}
	}

	public List<String> get(Language lang) {
		/*
		 * if(Language.ANY.equals(lang)) { return
		 * this.get(this.keySet().toArray()[0]);
		 * 
		 * } else
		 */
		return get(lang.getDefaultCode());
	}

	public void add(String key, String value) {
		if (this.containsKey(key)){
			List<String> list = this.get(key);
			if (!list.contains(value))
				list.add(value);
		}
		else
			this.put(key, new ArrayList<String>(Arrays.asList(value)));
	}

	public List<String> remove(Language lang) {
		return remove(lang.getDefaultCode());
	}
	
	public MultiLiteral fillDEF() {
		return fillDEF(false);
	}

	public MultiLiteral fillDEF(boolean forced) {
		if (forced || !containsKey(Language.DEFAULT.getDefaultCode())) {
			remove(Language.DEFAULT.getDefaultCode());
			String defLang = null;
			if (containsKey(Language.EN.getDefaultCode())) {
				defLang = Language.EN.getDefaultCode();
			}
			if (!containsKey(defLang)) {
				int max = 0;
				for (String k : this.keySet()) {
					if (Language.isLanguage(k)
							&& !k.equals(Language.DEFAULT.getDefaultCode())) {
						int m = get(k).size();
						if (max < m) {
							max = m;
							defLang = k;
						}
					}
				}
			}

			if (defLang != null)
				for (String d : get(defLang)) {
					add(Language.DEFAULT.getDefaultCode(), d);
				}
		}
		return this;
	}
	
	public MultiLiteral merge(MultiLiteral other) {
		this.putAll(other);
		return this;
	}
	
	public boolean contains(Language lang) {
		return containsKey(lang.getDefaultCode());
	}
	
	public Set<Language> getLanguages() {
		Set<Language> res = new HashSet<>();
		for (String s : keySet()) {
			res.add(Language.getLanguageByCode(s));
		}
		
		return res;
	}
}
