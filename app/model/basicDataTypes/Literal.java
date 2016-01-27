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

import java.util.HashMap;

import utils.Deserializer.LiteralDesiarilizer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = LiteralDesiarilizer.class)
public class Literal extends HashMap<String, String> {

	public Literal() {
	}

	public Literal(String label) {
		this.put(Language.UNKNOWN.getDefaultCode(), label);
	}

	public Literal(Language lang, String label) {
		this.put(lang.getDefaultCode(), label);
	}

	public void addLiteral(Language lang, String value) {
		this.put(lang.getDefaultCode(), value);
	}

	public void addLiteral(String value) {
		addLiteral(Language.UNKNOWN, value);
	}

	/**
	 * Don't request the "unknown" language, request "any" if you don't care
	 * 
	 * @param lang
	 * @return
	 */
	public String getLiteral(Language lang) {
		return get(lang.toString());
	}

	public void fillDEF() {
		if (containsKey(Language.DEFAULT.getDefaultCode())) {
			return;
		}
		if (containsKey(Language.EN.getDefaultCode())) {
			put(Language.DEFAULT.getDefaultCode(), getLiteral(Language.EN));
			return;
		}
		for (String lang : this.keySet()) {
			if (Language.contains(lang)) {
				put(Language.DEFAULT.getDefaultCode(), get(lang));
				return;
			}
		}
	}
}
