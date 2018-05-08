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

import model.basicDataTypes.Language;

import org.semanticweb.owlapi.model.OWLClass;

public class OWLImportConfiguration extends VocabularyImportConfiguration {
	String title;
	String prefix;
	String version;
	String labelProperty;
	private String URIfilterString;
	String mainScheme;
	public Language defaultLanguage;
	public String top;
	
	public OWLImportConfiguration(String folder, String title, String prefix, String version, String labelProperty, String URIfilterString, String mainScheme, Language defLanguage, String top) {
		super(folder); 
		this.title = title;
		this.prefix = prefix;
		this.version = version;
		this.labelProperty = labelProperty;
		this.URIfilterString = URIfilterString;
		this.mainScheme = mainScheme;
		this.defaultLanguage = defLanguage;
		this.top = top;
	}
	
	public boolean keep(OWLClass cz) {
		if (URIfilterString != null && !cz.getIRI().toString().contains(URIfilterString)) {
			return false;
		} else {
			return true;
		}
	}
	
	public boolean isTop(OWLClass cz) {
		if (top != null && cz.getIRI().toString().equals(top)) {
			return true;
		} else {
			return false;
		}
	}
}