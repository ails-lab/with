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
import java.util.Arrays;
import java.util.List;

public class CommonFilter implements Cloneable {

	public String filterID;
	public List<String> values;

	@Override
	public String toString() {
		return "CommonFilter [filterID=" + filterID + ", values=" + values + "]";
	}

	public List<CommonFilter> splitValues(ISpaceSource src) {
		ArrayList<CommonFilter> res = new ArrayList<>();

		for (String v : values) {
			List<String> vvalues = src.translateToSpecific(filterID, v);
			for (String vv : vvalues) {

				try {
					CommonFilter c = (CommonFilter) super.clone();
					c.values = Arrays.asList(vv);
					res.add(c);
				} catch (CloneNotSupportedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return res;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

}
