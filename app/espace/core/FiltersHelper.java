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

import java.util.Collection;

public class FiltersHelper {

	public static Collection<CommonFilterResponse> merge(Collection<CommonFilterResponse> mergedfilters,
			Collection<CommonFilterResponse> newfilters) {
		if (newfilters != null) {
			for (CommonFilterResponse commonFilterResponse : newfilters) {
				boolean merged = false;
				for (CommonFilterResponse old : mergedfilters) {
					if (old.filterID.equals(commonFilterResponse.filterID)) {
						merged = true;
						// Do the merge
						merge(old, commonFilterResponse);
						break;
					}
				}
				if (!merged) {
					mergedfilters.add(commonFilterResponse.clone());
				}
			}
		}
		return mergedfilters;
	}

	public static CommonFilterResponse merge(CommonFilterResponse a, CommonFilterResponse b) {
		a.addValue(b.suggestedValues);
		return a;
	}

}
