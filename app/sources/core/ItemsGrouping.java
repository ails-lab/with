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
	
	private List<WithResource<?>> culturalHO;
	
	

	public ItemsGrouping() {
		super();
		culturalHO = new ArrayList<>();
	}

	public List<WithResource<?>> getCulturalHO() {
		return culturalHO;
	}

	public void setCulturalHO(List<WithResource<?>> culturalHO) {
		this.culturalHO = culturalHO;
	}

	public void addAll(ItemsGrouping items) {
		culturalHO.addAll(items.getCulturalHO());
	}

}
