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


package utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import sources.core.CommonFilterLogic;
import sources.core.CommonFilterResponse;

public class ListUtils {

	/**
	 * tells if a collection contains any of the elements in another collection.
	 * 
	 * @param a
	 * @param b
	 * @return true iff any element of collection b is in collection a.
	 */
	public static boolean containsAny(Collection a, Collection b) {
		for (Object object : b) {
			if (a.contains(object))
				return true;
		}
		return false;
	}
	
	/**
	 * tells if a collection contains all of the elements in another collection.
	 * 
	 * @param a
	 * @param b
	 * @return true iff all elements of collection b are in collection a.
	 */
	public static boolean containsAll(Collection a, Collection b) {
		for (Object object : b) {
			if (!a.contains(object))
				return false;
		}
		return true;
	}

	/**
	 * tells if a collection does not contain elements of another collection.
	 * 
	 * @param a
	 * @param b
	 * @return true iff no element in b is contained in a.
	 */
	public static boolean containsNone(Collection a, Collection b) {
		return !containsAny(a, b);
	}

	/**
	 * tells if all the elements of a collection fulfill a condition.
	 * @param c
	 * @param condition
	 * @return
	 */
	public static <T> boolean allof(Collection<T> c, Function<T, Boolean> condition) {
		return !anyof(c, (T t) -> {
			return !condition.apply(t);
		});
	}

	/**
	 * tells if at least one element of a collection fulfills a condition
	 * @param c
	 * @param condition
	 * @return
	 */
	public static <T> boolean anyof(Collection<T> c, Function<T, Boolean> condition) {
		for (T t : c) {
			if (condition.apply(t))
				return true;
		}
		return false;
	}
	
	/**
	 * tells if at least one element of a collection fulfills a condition
	 * @param c
	 * @param condition
	 * @return
	 */
	public static <T> boolean anyof(T[] c, Function<T, Boolean> condition) {
		return anyof(Arrays.asList(c), condition);
	}
	
	/**
	 * tells if all the elements of a collection fulfill a condition.
	 * @param c
	 * @param condition
	 * @return
	 */
	public static <T> boolean allof(T[] c, Function<T, Boolean> condition) {
		return allof(Arrays.asList(c), condition);
	}


	/**
	 * transforms the elements of a collection of type T to a collection of elements of type R using
	 * a functions that does the transformation.
	 * @param objects
	 * @param function
	 * @return
	 */
	public static <T, R> List<R> transform(Collection<T> objects, Function<T, R> function) {
		List<R> res = new ArrayList<>();
		if (objects!=null){
			for (T t : objects) {
				res.add(function.apply(t));
			}
		}
		return res;
	}
	
	/**
	 * gets the list associated to a key in a map of Key-List structure. If the key is not present it is
	 * added to the map with an empty list associated. 
	 * @param key
	 * @param map
	 * @return
	 */
	public static <K,T> List<T> getOrSet(K key, HashMap<K,List<T>>  map){
		List<T> result = map.get(key);
		if (result==null){
			map.put(key, result = new ArrayList<>());
		}
		return result;	
	}

	/**
	 * gets the last element of a list.
	 * @param list
	 * @return
	 */
	public static<T> T last(List<T> list) {
		return list.get(list.size()-1);
	}

}
