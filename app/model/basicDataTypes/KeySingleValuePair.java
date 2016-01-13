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

import java.util.HashMap;

import org.mongodb.morphia.annotations.Embedded;

import model.basicDataTypes.KeySingleValuePair.LiteralOrResource;
import model.basicDataTypes.OldLiteralOrResource.OldResource;
import sources.core.Utils;

@Embedded
public class KeySingleValuePair<K,V> extends HashMap<K, V> {

	public KeySingleValuePair() {
		super();
	}
	
	public void add(K key, V value) {
		put(key, value);
	}
	
	public static class LiteralOrResource extends KeySingleValuePair<ILiteralOrResource, String>{

		public LiteralOrResource() {
			super();
		}

		public LiteralOrResource(ResourceType resourceType, String uri) {
			add(resourceType, uri);
		}

		public LiteralOrResource(String label) {
			add(Language.UNKNOWN,label);
		}

		
		public static LiteralOrResource build(String string) {
			if (Utils.isValidURL(string)){
				return new LiteralOrResource(ResourceType.uri, string);
			}
			return new LiteralOrResource(string);
		}
		
	}
	
	public static class Literal extends KeySingleValuePair<Language, String>{
		
	}
	
	public static class Resource extends KeySingleValuePair<ResourceType, String> {

	}
    
}
