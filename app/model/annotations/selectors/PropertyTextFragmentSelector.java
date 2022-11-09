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

import org.mongodb.morphia.query.Query;

import com.fasterxml.jackson.annotation.JsonInclude;

import model.annotations.Annotation;
import model.basicDataTypes.Language;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class PropertyTextFragmentSelector extends PropertySelector {
	
	private String origValue;
	private Language origLang;
	
	private int start;
	private int end;
	
	public int getStart() {
		return start;
	}
	
	public void setStart(int start) {
		this.start = start;
	}
	
	public int getEnd() {
		return end;
	}
	
	public void setEnd(int end) {
		this.end = end;
	}

	public String getOrigValue() {
		return origValue;
	}

	public void setOrigValue(String origValue) {
		this.origValue = origValue;
	}

	public Language getOrigLang() {
		return origLang;
	}

	public void setOrigLang(Language origLang) {
		this.origLang = origLang;
	}
	
	@Override
    public Object clone() throws CloneNotSupportedException {
		PropertyTextFragmentSelector c = (PropertyTextFragmentSelector)super.clone();
		c.origLang = origLang;
		c.origValue = origValue;

		return c;
    }

	@Override
	public java.lang.String toString() {
		return "PropertyTextFragmentSelector{" +
				"origValue='" + origValue + '\'' +
				", origLang=" + origLang +
				", start=" + start +
				", end=" + end +
				'}';
	}

	@Override
	public void addToQuery(Query<Annotation> q) {
		super.addToQuery(q);
		
		q.field("target.selector.start").equal(start);
		q.field("target.selector.end").equal(end);
		
		if (origValue != null) {
			q.field("target.selector.origValue").equal(origValue);
		}
		
		if (origLang != null) {
			q.field("target.selector.origLang").equal(origLang);
		}
	}



}
