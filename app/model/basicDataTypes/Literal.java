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

public class Literal extends HashMap<Literal.Language, String> {
	
	public static enum Language {
		EN, UNKNOWN, ANY;
	}
	
	public Literal(String label) {
		this.put(Language.UNKNOWN, label);
	}
	
	public Literal(Language lang, String label) {
		this.put(lang, label);
	}
	
	// keys are language 2 letter codes, 
	// "unknown" for unknown language
	// special request for any is "any"
	public void setLiteral(Language val, String lang) {
		this.put(val, lang);
	}
	/**
	 * Don't request the "unknown" language, request "any" if you don't care
		 * @param lang
	 * @return
	 */
	public String getLiteral(String lang) {
		if(Language.ANY.equals(lang)) {
			return this.get(this.keySet().toArray()[0]);
		}
		else
			return get(lang);
	}		
}
