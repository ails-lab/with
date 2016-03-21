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
import java.util.List;

import model.resources.WithResource;

public class ItemsGrouping {
	
	private List<WithResource<?, ?>> culturalCHO;
	
	

	public ItemsGrouping() {
		super();
		culturalCHO = new ArrayList<>();
	}

	public List<WithResource<?, ?>> getCulturalCHO() {
		return culturalCHO;
	}

	public void setCulturalCHO(List<WithResource<?, ?>> culturalHO) {
		this.culturalCHO = culturalHO;
	}

	public void addAll(ItemsGrouping items) {
		culturalCHO.addAll(items.getCulturalCHO());
	}

	public int getItemsCount() {
		return culturalCHO.size();
	}

}
