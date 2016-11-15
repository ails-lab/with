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


package model.annotations.bodies;

import java.util.ArrayList;

import db.DB;
import model.basicDataTypes.Literal;
import model.basicDataTypes.MultiLiteral;
import model.resources.ThesaurusObject;

public class AnnotationBodyTagging extends AnnotationBody {
	
	/**
	 * The uri of the tag.
	 */
	private String uri;
	
	/**
	 * The tag type. This value should be a uri either of a class of of a property.
	 */
	private String tagType;
	
	/**
	 * The pref label of the URI tag, that will be used for display purposes.
	 */
	private MultiLiteral label;
	
	
	/**
	 * The types of the uri. This value should be a uri of the class to which the tag belongs to.
	 * i.e. http://dbpedia.org/ontology/Building  etc
	 */
	private ArrayList<String> uriType;
	
	/**
	 * A value taken from an enumeration that includes all the vocabularies used in WITH and by the
	 * annotator generators.
	 */
	private String uriVocabulary;
	
	/**
	 * This should be only populated when the tag of the annotation is not a URI but a text values.
	 * When this value exists the uri, uriLabel, uriType and uriVocabulary should be null.
	 */
	/*private MultiLiteral text;*/
	
	
	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getTagType() {
		return tagType;
	}

	public void setTagType(String tagType) {
		this.tagType = tagType;
	}

	public MultiLiteral getLabel() {
		return label;
	}

	public void setLabel(MultiLiteral label) {
		this.label = label;
	}

	public ArrayList<String> getUriType() {
		return uriType;
	}

	public void setUriType(ArrayList<String> uriType) {
		this.uriType = uriType;
	}

	public String getUriVocabulary() {
		return uriVocabulary;
	}

	public void setUriVocabulary(String uriVocabulary) {
		this.uriVocabulary = uriVocabulary;
	}

	/*public MultiLiteral getText() {
		return text;
	}

	public void setText(MultiLiteral text) {
		this.text = text;
	}*/

	public void adjustLabel() {
		ThesaurusObject to = DB.getThesaurusDAO().getByUri(uri);
		if (to != null) {
			MultiLiteral ml = new MultiLiteral(to.getSemantic().getPrefLabel());
			ml.fillDEF();
			
			setLabel(ml);
			
			setUriVocabulary(to.getSemantic().getVocabulary().getName());
		}
		
	}
	
}
