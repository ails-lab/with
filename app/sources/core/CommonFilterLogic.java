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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import model.basicDataTypes.Language;
import model.basicDataTypes.ProvenanceInfo.Sources;
import play.Logger;
import play.Logger.ALogger;
import search.FiltersFields;
import utils.SortedList;

public class CommonFilterLogic implements Cloneable {
	public static final ALogger log = Logger.of( CommonFilterLogic.class);
	
	private HashMap<String, ValueCount> counts = new HashMap<String, ValueCount>();

	public CommonFilterResponse data = new CommonFilterResponse();

	public CommonFilterLogic(FiltersFields filter) {
		this.data.filterID = filter.getFilterId();
		this.data.filterName = filter.getFilterName().get(Language.DEFAULT).get(0);
	}
	
	public CommonFilterLogic(CommonFilterResponse filter) {
		this.data.filterID = filter.filterID;
		this.data.filterName = filter.filterName;
		for ( ValueCount v : filter.suggestedValues) {
			counts.put(v.value, v);
		}
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
		res.data.sources.addAll(data.sources);
		return res;
	}

	public Collection<ValueCount> values() {
		return counts.values();
	}
	
	public CommonFilterLogic addTo(List<CommonFilterLogic> list){
		list.add(this);
		return this;
	}

	public void addSourceFrom(CommonFilterLogic b) {
		addSourceFrom(b.data);
	}
	public void addSourceFrom(CommonFilterResponse b) {
		for (Sources s : b.sources) {
			data.addSource(s);	
		}
	}
}
