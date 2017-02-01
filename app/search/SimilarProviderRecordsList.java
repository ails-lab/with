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

import model.basicDataTypes.Language;
import model.basicDataTypes.Literal;
import model.resources.RecordResource;

public class SimilarProviderRecordsList extends SimilarRecordsList {

	public SimilarProviderRecordsList(SimilarsQuery q) {
		super("same.provider",new Literal(Language.EN, "Same Provider"));
		query(q);
	}

	@Override
	public void query(SimilarsQuery q) {
		RecordResource<?> r = getTheRecord(q);
//		prov = ((RecordResource.RecordDescriptiveData)r.getDescriptiveData()).;
		String prov = r.getProvenance().get(r.getProvenance().size()-1).getProvider();
		Query iq = new Query();
		iq.addClause(new Filter(Fields.provenance_provider.fieldId(), prov));
	}

}
