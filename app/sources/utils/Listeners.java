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
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class Listeners<T> {
	protected List<T> listeners;

	public Listeners() {
		super();
		listeners = new ArrayList<>();
	}

	public List<T> getListeners() {
		return listeners;
	}

	public void addListener(T listener) {
		if (listener == null)
			throw new RuntimeException("listener cannot be null");
		listeners.add(listener);
	}
	
	public void removeListener(T listener) {
		listeners.remove(listener);
	}
	
	public void clearListeners(){
		listeners.clear();
	}
	public static class ObjectListeners<B> extends Listeners<Consumer<B>> implements Consumer<B>{

		@Override
		public void accept(B t) {
			for (Consumer<B> consumer : listeners) {
				consumer.accept(t);
			}
		}
		
	}
	
}
