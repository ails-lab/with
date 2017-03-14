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
import model.basicDataTypes.MultiLiteralOrResource;
import model.resources.CulturalObject.CulturalObjectData;
import model.resources.RecordResource;
import play.libs.F.Promise;
import play.mvc.Result;

public class SimilarCreatorSearch extends SimilarSearch {

	public SimilarCreatorSearch() {
	}

	@Override
	public Promise<RecordsList> query(SimilarsQuery q) {
		RecordResource<?> r = getTheRecord(q);
//		prov = ((RecordResource.RecordDescriptiveData)r.getDescriptiveData()).;
		String prov = r.getProvenance().get(0).getProvider();
		Query iq = new Query();
		iq.setPageAndSize(1, q.getSize());
		addSources(r, iq);
		MultiLiteralOrResource dccreator = ((CulturalObjectData)r.getDescriptiveData()).getDccreator();
		RecordsList recordsList = new RecordsList(Fields.descriptiveData_dccreator.fieldId(), 
				new Literal(Language.EN,"Same Creator"));
		if (dccreator!=null){
			String creator = dccreator.get(Language.DEFAULT).get(0);
			iq.addClause(new Filter(Fields.descriptiveData_dccreator.fieldId(), creator));
			iq.addClause(new Filter(Fields.resourceType.fieldId(),"CulturalObject"));
			// just to make sure its ok
			recordsList.setDescription(new Literal(Language.EN, "Items with creator "+creator));
			return SimilarSearch.executeQuery(iq).map((mapSR)->{
				buildResults(mapSR, recordsList);
				recordsList.setQuery(iq);
				return recordsList;
			});
		} else {
			recordsList.setDescription(new Literal(Language.EN, "No creator Found"));
			return Promise.pure(recordsList);
		}
	}
	
	

}
