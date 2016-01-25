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


package sources.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;

import utils.SortedList;

public class CommonFilterLogic implements Cloneable {

	private HashMap<String, ValueCount> counts = new HashMap<String, ValueCount>();

	public CommonFilterResponse data = new CommonFilterResponse();

	public CommonFilterLogic(CommonFilters filter) {
		this.data.filterID = filter.getId();
		this.data.filterName = filter.getText();
	}
	
	public CommonFilterLogic(String filter) {
		this.data.filterID = filter;
		this.data.filterName = filter;
	}
	
	public CommonFilterLogic(String filter, String text) {
		this.data.filterID = filter;
		this.data.filterName = text;
	}
	
	public void addValue(String value, int count) {
		if (value != null) {
			// System.out.println(filterName + " Added " + value);
			getOrSet(value).add(count);
		}
	}

	public void addValueCounts(Collection<ValueCount> value) {
		if (value != null) {
			for (ValueCount valueCount : value) {
				getOrSet(valueCount.value).add(valueCount.count);
			}
		}
	}

	private ValueCount getOrSet(String value) {
		if (!counts.containsKey(value)) {
			counts.put(value, new ValueCount(value, 0));
		}
		return counts.get(value);
	}

	public void addValue(Collection<String> values, int count) {
		for (String string : values) {
			addValue(string, count);
		}
	}


	@Override
	public String toString() {
		return "Filter [" + data.filterID + ", values=" + counts.values().size() + "]";
	}

	public CommonFilterResponse export() {
		data.suggestedValues = new SortedList<>(ValueCount.comparator());
		data.suggestedValues.addAll(counts.values());
		return data;
	}

	@Override
	protected CommonFilterLogic clone() {
		CommonFilterLogic res = new CommonFilterLogic(this.data.filterID);
		res.data.filterName = data.filterName;
		res.counts = (HashMap<String, ValueCount>) counts.clone();
		return res;
	}

	public Collection<ValueCount> values() {
		return counts.values();
	}
}
