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


package tools.importers.vocabulary;

import java.util.HashSet;
import java.util.Set;

import model.basicDataTypes.Language;

public class SKOSImportConfiguration extends VocabularyImportConfiguration {
	String title;
	String prefix;
	String version;
	String URIfilterString;
	String mainScheme;
	Set<String> manualSchemes;
	Language defaultLanguage;
	
	public SKOSImportConfiguration(String folder, String title, String prefix, String version, String URIfilterString, String mainScheme, String[] schemes, Language defLanguage) {
		super(folder); 
		this.title = title;
		this.prefix = prefix;
		this.version = version;
		this.URIfilterString = URIfilterString;
		this.mainScheme = mainScheme;
		this.defaultLanguage = defLanguage;

		
		manualSchemes = null;
		if (schemes != null) {
			manualSchemes = new HashSet<>();
			for (String s : schemes) {
				manualSchemes.add(s);
			}
		}
	}
	
	public boolean keep(String cz) {
		if (URIfilterString != null && !cz.contains(URIfilterString)) {
			return false;
		} else {
			return true;
		}
	}
	
	public Set<String> getManualSchemes() {
		return manualSchemes;
	}
	
	public boolean isValidScheme(String uri, String type) {
		if (type.equals("http://www.w3.org/2004/02/skos/core#ConceptScheme")) {
			if (manualSchemes != null) {
				return manualSchemes.contains(uri);
			} else {
				return true;
			}
		} else {
			if (manualSchemes != null) {
				return manualSchemes.contains(uri);
			} else {
				return false;
			}
		}
	}
	
	
}