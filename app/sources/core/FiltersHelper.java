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

public class FiltersHelper {

	public static Collection<CommonFilterLogic> merge(Collection<CommonFilterLogic> mergedfilters,
			Collection<CommonFilterLogic> newfilters) {
		if (newfilters != null) {
			for (CommonFilterLogic commonFilterResponse : newfilters) {
				boolean merged = false;
				for (CommonFilterLogic old : mergedfilters) {
					if (old.data.filterID.equals(commonFilterResponse.data.filterID)) {
						merged = true;
						// Do the merge
						merge(old, commonFilterResponse);
						break;
					}
				}
				if (!merged) {
					CommonFilterLogic clone = commonFilterResponse.clone();
					mergedfilters.add(clone);
				}
			}
		}
		return mergedfilters;
	}
	
	public static Collection<CommonFilterLogic> mergeAux(Collection<CommonFilterLogic> mergedfilters,
			Collection<CommonFilterResponse> newfilters) {
		if (newfilters != null) {
			for (CommonFilterResponse commonFilterResponse : newfilters) {
				boolean merged = false;
				for (CommonFilterLogic old : mergedfilters) {
					if (old.data.filterID.equals(commonFilterResponse.filterID)) {
						merged = true;
						// Do the merge
						merge(old, commonFilterResponse);
						break;
					}
				}
				if (!merged) {
					CommonFilterLogic clone = new CommonFilterLogic(commonFilterResponse);
					mergedfilters.add(clone);
				}
			}
		}
		return mergedfilters;
	}

	public static CommonFilterLogic merge(CommonFilterLogic a, CommonFilterLogic b) {
		a.addValueCounts(b.values());
		return a;
	}

	public static CommonFilterLogic merge(CommonFilterLogic a, CommonFilterResponse b) {
		a.addValueCounts(b.suggestedValues);
		return a;
	}
}
