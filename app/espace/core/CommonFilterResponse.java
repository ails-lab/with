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
import java.util.List;

public class CommonFilterResponse implements Cloneable {

	public String filterName;
	public String filterID;
	public List<String> suggestedValues;

	public void addValue(String value) {
		if (value != null && !suggestedValues.contains(value)) {
			System.out.println(filterName + " Added " + value);
			suggestedValues.add(value);
		}
	}

	public void addValue(Collection<String> values) {
		for (String string : values) {
			addValue(string);
		}
	}

	public static CommonFilterResponse typeFilter() {
		CommonFilterResponse r = new CommonFilterResponse();
		r.filterID = CommonFilters.TYPE_ID;
		r.filterName = CommonFilters.TYPE_NAME;
		r.suggestedValues = new ArrayList<String>();
		return r;
	}

	@Override
	public String toString() {
		return "CommonFilterResponse [filterName=" + filterName + ", filterID=" + filterID + ", suggestedValues="
				+ suggestedValues + "]";
	}

	@Override
	protected CommonFilterResponse clone() {
		CommonFilterResponse res = new CommonFilterResponse();
		res.filterID = this.filterID;
		res.filterName = this.filterName;
		res.suggestedValues = new ArrayList<String>();
		res.suggestedValues.addAll(suggestedValues);
		return res;
	}
}
