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
import java.util.Comparator;

import com.google.common.collect.Lists;

public class SortedList<T> extends ArrayList<T> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6818337153028793324L;

	Comparator<T> comparator;

	public SortedList(Comparator<T> comparator) {
		super();
		this.comparator = comparator;
	}

	@Override
	public boolean add(T e) {
		if (isEmpty())
			return super.add(e);
		int pos = binarySearch(e);
		if (pos >= size())
			super.add(e);
		else
			add(pos, e);
		return true;

	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		for (T t : c) {
			add(t);
		}
		return true;
	}

	private int binarySearch(T e) {
		int a = 0, b = size() - 1;
		int mid = (a + b) / 2;
		while (a < b) {
			int cmp = comparator.compare(e, get(mid));
			if (cmp == 0)
				return mid + 1;
			if (cmp < 0) {
				b = mid - 1;
			} else {
				a = mid + 1;
			}
			mid = (a + b) / 2;
		}
		if (comparator.compare(e, get(mid)) >= 0)
			mid++;
		// System.out.println("put " + e + " at " + mid + " of " + this);
		return mid;
	}

	public static void main(String[] args) {
		SortedList<Integer> l = new SortedList<>(new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				return (int) Math.signum(o2 - o1);
			}
		});
		l.add(2);
		l.add(3);
		l.add(5);
		l.add(1);
		l.add(4);
		l.add(4);
		System.out.println(l);
	}
}
