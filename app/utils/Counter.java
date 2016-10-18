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

public class Counter {

	private int count;
	
	public Counter(int i) {
		count = i;
	}
	
	public void increase() {
		count++;
	}

	public void increase(int c) {
		count += c;
	}

	public int increaseUse() {
		count++;
		return count - 1;
	}

	public void decrease() {
		count--;
	}
	
	public int getValue() {
		return count;
	}
	
	public int hashCode() {
		return count;
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof Counter)) {
			return false;
		}
		
		return count == ((Counter)obj).count;
	}
	
	public String toString() {
		return count + "";
	}
	
}	
