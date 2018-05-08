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


package search;

public class Filter implements Cloneable {

	/**
	 * FieldIdswill be the path expressions into the json represeantation of the resource you are querying.
	 * Some rules about shortening may apply for labels and other very deep fieldIds. Arrays are probably not part of this
	 * fieldId meaning the filter applies to any element of the array and is true if any value is found in the array.
	 */
	public String fieldId;

	/**
	 * A literal value we are looking for. Possibly we might allow for some wildcarding here in the future. Some sources may
	 * interpret the values differently. More specs, when we know more.
	 */
	public String value;

	/**
	 * In case the fields supports range queries, the inclusive boundaries go here. Range queries should
	 * not work on tokenized fields (so they assume exact, whether you specify it or not)
	 */
	public String from, to;
	
	/**
	 * Optionally, at some point a Filter should allow to be true/valid if the value is not part of the field.
	 */
	// public boolean not;

	/**
	 * If the semantics of this filter is to look for an exact match of the field, this should be set to true.
	 * This is usually the case if the user can select something in the UI.
	 */
	public boolean exact = false;

	/**
	 * If you need to be specific about the language you are searching, you can specify it here, the deafult is, that
	 * language is ignored. If a source doesn't support language specific search, it may choose to ignore this.
	 */

	public String lang = "";

	public Filter( String fieldId, String value ) {
		this.fieldId = fieldId;
		this.value = value;
	}

	public Filter( String fieldId, String value, boolean exact) {
		this.fieldId = fieldId;
		this.value = value;
		this.exact = exact;
	}

	public Filter() {

	}
	
	public Object clone() {
		try {
			Object obj =  super.clone();
			return obj;
		} catch( Exception e ) {
			// shouldn't really happen
		}
		return null;
	}
}

/*
    FieldIds: in the API we use the canonical fieldids, the JSON path into the database representation of the object.
    Some exceptions apply to arrays and labels

    The search.Fields enum contains all Fields that are allowed.
    TODO: use Fields enum in the code to agree on spelling??
 */
