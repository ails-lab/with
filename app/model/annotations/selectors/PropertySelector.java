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


package model.annotations.selectors;

import model.annotations.Annotation;

import org.mongodb.morphia.query.Query;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class PropertySelector extends SelectorType {
	
	private String property;

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	} 
	
	@Override
    public Object clone() throws CloneNotSupportedException {
		PropertySelector c = (PropertySelector)super.clone();
		c.property = property;
		
		return c;
    }
	
	@Override
	public void addToQuery(Query<Annotation> q) {
		q.field("target.selector.property").equal(property);
	}

}
