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


package sources.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListMap<K, T> extends HashMap<K, List<T>> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public ListMap() {
		super();
	}

	public ListMap(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	public ListMap(int initialCapacity) {
		super(initialCapacity);
	}

	public ListMap(Map<? extends K, ? extends List<T>> m) {
		super(m);
	}

	@Override
	public List<T> put(K key, List<T> value) {
		List<T> list = getOrSet(key);
		list.addAll(value);
		return list;
	}
	
	public List<T> put(K key, T... value) {
		return this.put(key, Arrays.asList(value));
	}
	
	/**
	 * gets the list associated to a key in a map of Key-List structure. If the key is not present it is
	 * added to the map with an empty list associated. 
	 * @param key
	 * @param map
	 * @return
	 */
	public List<T> getOrSet(K key){
		List<T> result = get(key);
		if (result==null){
			super.put(key, result = new ArrayList<>());
		}
		return result;	
	}
}
