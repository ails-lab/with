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


package espace.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;

import utils.SortedList;

public class CommonFilterLogic implements Cloneable {

	private HashMap<String, ValueCount> counts;
	private final boolean global = true;

	public CommonFilterResponse data = new CommonFilterResponse();

	public CommonFilterLogic() {
		super();
		counts = new HashMap<String, ValueCount>();
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

	public static CommonFilterLogic typeFilter() {
		CommonFilterLogic r = new CommonFilterLogic();
		r.data.filterID = CommonFilters.TYPE_ID;
		r.data.filterName = CommonFilters.TYPE_NAME;
		return r;
	}

	public static CommonFilterLogic providerFilter() {
		CommonFilterLogic r = new CommonFilterLogic();
		r.data.filterID = CommonFilters.PROVIDER_ID;
		r.data.filterName = CommonFilters.PROVIDER_NAME;
		return r;
	}

	public static CommonFilterLogic dataproviderFilter() {
		CommonFilterLogic r = new CommonFilterLogic();
		r.data.filterID = CommonFilters.DATAPROVIDER_ID;
		r.data.filterName = CommonFilters.DATAPROVIDER_NAME;
		return r;
	}

	public static CommonFilterLogic comesFromFilter() {
		CommonFilterLogic r = new CommonFilterLogic();
		r.data.filterID = CommonFilters.COMESFROM_ID;
		r.data.filterName = CommonFilters.COMESFROM_NAME;
		return r;
	}

	public static CommonFilterLogic creatorFilter() {
		CommonFilterLogic r = new CommonFilterLogic();
		r.data.filterID = CommonFilters.CREATOR_ID;
		r.data.filterName = CommonFilters.CREATOR_NAME;
		return r;
	}

	@Override
	public String toString() {
		return "Filter [" + data.filterName + ", values=" + counts.values().size() + "]";
	}

	public CommonFilterResponse export() {
		data.suggestedValues = new SortedList<>(ValueCount.comparator());
		data.suggestedValues.addAll(counts.values());
		return data;
	}

	@Override
	protected CommonFilterLogic clone() {
		CommonFilterLogic res = new CommonFilterLogic();
		res.data.filterID = this.data.filterID;
		res.data.filterName = this.data.filterName;
		res.counts = (HashMap<String, ValueCount>) counts.clone();
		return res;
	}

	public Collection<ValueCount> values() {
		return counts.values();
	}

	public static CommonFilterLogic rightsFilter() {
		CommonFilterLogic r = new CommonFilterLogic();
		r.data.filterID = CommonFilters.RIGHTS_ID;
		r.data.filterName = CommonFilters.RIGHTS_NAME;
		return r;
	}

	public static CommonFilterLogic reusabilityFilter() {
		CommonFilterLogic r = new CommonFilterLogic();
		r.data.filterID = CommonFilters.REUSABILITY_ID;
		r.data.filterName = CommonFilters.REUSABILITY_NAME;
		return r;
	}

	public static CommonFilterLogic countryFilter() {
		CommonFilterLogic r = new CommonFilterLogic();
		r.data.filterID = CommonFilters.COUNTRY_ID;
		r.data.filterName = CommonFilters.COUNTRY_NAME;
		return r;
	}

	public static CommonFilterLogic yearFilter() {
		CommonFilterLogic r = new CommonFilterLogic();
		r.data.filterID = CommonFilters.YEAR_ID;
		r.data.filterName = CommonFilters.YEAR_NAME;
		return r;
	}
	/**
	 * TODO check this added for availability face of NLA
	 */
	public static CommonFilterLogic contributorFilter() {
		CommonFilterLogic r = new CommonFilterLogic();
		r.data.filterID = CommonFilters.CONTRIBUTOR_ID;
		r.data.filterName = CommonFilters.CONTRIBUTOR_NAME;
		return r;
	}

	public static CommonFilterLogic availabilityFilter() {
		CommonFilterLogic r = new CommonFilterLogic();
		r.data.filterID = CommonFilters.AVAILABILITY_ID;
		r.data.filterName = CommonFilters.AVAILABILITY_NAME;
		return r;
	}
}
