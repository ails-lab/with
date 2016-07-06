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

public class Filter {
	
	/**
	 * Fieldnames will be the path expressions into the json represeantation of the resource you are querying.
	 * Some rules about shortening may apply for labels nad other very deep fieldnames. Arrays are probably not part of this 
	 * fieldname, meaning the filter applies to any element of the array and is true if any value is found in the array.
	 */
	public String fieldname;
	
	/**
	 * A literal value we are looking for. Possibly we might allow for some wildcarding here in the future. Some sources may 
	 * interpret the values differently. More specs, when we know more. 
	 */
	public String value;
	
	/**
	 * Optionally, at some point a Filter should allow to be true/valid if the value is not part of the field.
	 */
	// public boolean not;

	public Filter( String fieldname, String value ) {
		this.fieldname = fieldname;
		this.value = value;
	}
}

/*
    Fieldnames: in the API we use the canonical fieldnames, the JSON path into the database representation of the object.
    Some exceptions apply to arrays and labels
    
    
    
  
 */
