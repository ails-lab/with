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


package model.basicDataTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.mongodb.morphia.annotations.Embedded;

@Embedded
public class KeyValuesPair<K> extends HashMap<String, List<String>> {

	public KeyValuesPair() {
		super();
	}

	public void add(K key, String value) {
		List<String> list = get(key);
		if (list == null) {
			put(key.toString(), list = new ArrayList<>());
		}
		list.add(value);
	}

}
