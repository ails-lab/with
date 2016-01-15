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

public class OldMultiLiteral extends HashMap<String, ArrayList<String>> {

	
		public OldMultiLiteral() {
		}

		public OldMultiLiteral(String label) {
			this.put(Language.UNKNOWN.toString(), new ArrayList<String>(Arrays.asList(label)));
		}

		public OldMultiLiteral(Language lang, String label) {
			if (this.containsKey(lang.toString()))
				this.get(lang.toString()).add(label);
			else
				this.put(Language.UNKNOWN.toString(), new ArrayList<String>(Arrays.asList(label)));
			if (lang.equals(Language.EN) && !this.containsKey(Language.DEF.toString()))
				this.put(Language.DEF.toString(), new ArrayList<String>(Arrays.asList(label)));
		}

		// keys are language 2 letter codes,
		// "unknown" for unknown language
		public void setMultiLiteral(Language lang, String label) {
			if (this.containsKey(lang.toString()))
				this.get(lang.toString()).add(label);
			else
				this.put(Language.UNKNOWN.toString(), new ArrayList<String>(Arrays.asList(label)));
		}
		/**
		 * Don't request the "unknown" language, request "any" if you don't care
			 * @param lang
		 * @return
		 */
		public ArrayList<String> getMultiLiteral(Language lang) {
			/*if(Language.ANY.equals(lang)) {
				return this.get(this.keySet().toArray()[0]);
			}
			else*/
				return get(lang.toString());
		}
}
