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
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import espace.core.CommonFilterLogic;
import espace.core.CommonFilterResponse;

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
	 * tells if a collection does not contain elements of another collection.
	 * 
	 * @param a
	 * @param b
	 * @return true iff no element in b is contained in a.
	 */
	public static boolean containsNone(Collection a, Collection b) {
		return !containsAny(a, b);
	}

	public static <T> boolean allof(Collection<T> c, Function<T, Boolean> condition) {
		return !anyof(c, (T t) -> {
			return !condition.apply(t);
		});
	}

	public static <T> boolean anyof(Collection<T> c, Function<T, Boolean> condition) {
		for (T t : c) {
			if (condition.apply(t))
				return true;
		}
		return false;
	}

	public static <T, R> List<R> transform(Collection<T> objects, Function<T, R> function) {
		List<R> res = new ArrayList<>();
		if (objects!=null){
			for (T t : objects) {
				res.add(function.apply(t));
			}
		}
		return res;
	}

}
