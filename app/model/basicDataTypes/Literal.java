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
import java.util.Locale;

import org.mongodb.morphia.annotations.Embedded;

@Embedded
public class Literal extends HashMap<String, String> {

	public static enum Language {
		ENGLISH("en"),SPANISH("es"), FRENCH("fr"),
		UNKNOWN("unk"), DEFAULT("def");
		private String code;

		private Language(String code) {
			this.code = code;
		}
		public String getCode(){
			return code;
		}
	}

	public Literal() {
	}

	public Literal(String label) {
		this.put(Language.UNKNOWN.toString(), label);
	}

	public Literal(Language lang, String label) {
		this.put(lang.toString(), label);
		if (lang.equals(Language.ENGLISH))
			this.put(Language.DEFAULT.toString(), label);
	}

	public Literal(String lang, String label) {
		this.put(lang, label);
	}

	// keys are language 2 letter codes,
	// "unknown" for unknown language
	// special request for any is "any"
	public void setLiteral(Language lang, String label) {
		put(lang.toString(), label);
	}
	/**
	 * Don't request the "unknown" language, request "any" if you don't care
		 * @param lang
	 * @return
	 */
	public String getLiteral(Language lang) {
		/*if(Language.ANY.equals(lang)) {
			return this.get(this.keySet().toArray()[0]);
		}
		else*/
			return get(lang.toString());
	}
}
