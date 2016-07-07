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


package search;

import play.libs.F.Promise;
import sources.core.CommonQuery;
import sources.core.ISpaceSource;

public class ProxySource extends EmptySource {
	
	private Sources source;
	private ISpaceSource spaceSource;
	
	public ProxySource(Sources source, ISpaceSource spaceSource) {
		super();
		this.source = source;
		this.spaceSource = spaceSource;
	}
	
	@Override
	public Sources thisSource() {
		return source;
	}
	
	@Override
	public Promise<Response> execute(Query query) {
		// TODO Translate to a CommonQuery
		CommonQuery q = new CommonQuery(query);
		// TODO execute in spaceSource
		// TODO translate to a Response.
		return super.execute(query);
	}
	
	@Override
	public Promise<Object> completeRecord(Object incompleteRecord) {
		// TODO call the method to collecto objects and return the result
		return super.completeRecord(incompleteRecord);
	}
	
	@Override
	public Promise<Object> getById(String id) {
		// TODO see compreteRecord
		return super.getById(id);
	}

}
