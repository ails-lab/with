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
import play.libs.F.Promise;

public class SimilarSearchTerm extends SimilarSearch {

	public SimilarSearchTerm() {
	}

	@Override
	public Promise<RecordsList> query(SimilarsQuery q) {
		RecordResource<?> r = getTheRecord(q);
		Query iq = new Query();
		iq.setPageAndSize(1, q.getSize());
		addSources(r, iq);
		String t = r.getDescriptiveData().getLabel().get(Language.DEFAULT).get(0);
		iq.addClause(new Filter(Fields.anywhere.fieldId(),t));
		iq.addClause(new Filter(Fields.resourceType.fieldId(),"CulturalObject"));
		// just to make sure its ok
		return SimilarSearch.executeQuery(iq).map((mapSR)->{
			RecordsList recordsList = new RecordsList(Fields.provenance_provider.fieldId(), 
					new Literal(Language.EN,"Related to the title"), 
					new Literal(Language.EN, "Items obtained with search term: "+t));
			buildResults(mapSR, recordsList);
			recordsList.setQuery(iq);
			return recordsList;
		});
	}
	
	

}
