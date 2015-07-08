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


package espace.core.sources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;

public class FashionSearch {

	private String term;
	private List<Filter> filters = new ArrayList<Filter>();
	private Integer count;
	private Integer offset;
	private Map<String, Object> additionalProperties = new HashMap<String, Object>();

	/**
	 *
	 * @return The term
	 */
	public String getTerm() {
		return term;
	}

	/**
	 *
	 * @param term
	 *            The term
	 */
	public void setTerm(String term) {
		this.term = term;
	}

	/**
	 *
	 * @return The filters
	 */
	public List<Filter> getFilters() {
		return filters;
	}

	/**
	 *
	 * @param filters
	 *            The filters
	 */
	public void setFilters(List<Filter> filters) {
		this.filters = filters;
	}

	/**
	 *
	 * @return The count
	 */
	public Integer getCount() {
		return count;
	}

	/**
	 *
	 * @param count
	 *            The count
	 */
	public void setCount(Integer count) {
		this.count = count;
	}

	/**
	 *
	 * @return The offset
	 */
	public Integer getOffset() {
		return offset;
	}

	/**
	 *
	 * @param offset
	 *            The offset
	 */
	public void setOffset(Integer offset) {
		this.offset = offset;
	}

	public Map<String, Object> getAdditionalProperties() {
		return this.additionalProperties;
	}

	public void setAdditionalProperty(String name, Object value) {
		this.additionalProperties.put(name, value);
	}

}