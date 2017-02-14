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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import controllers.WithController;
import model.basicDataTypes.Language;
import model.basicDataTypes.Literal;
import model.resources.CulturalObject.CulturalObjectData;
import model.resources.RecordResource;
import play.libs.Json;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.mvc.Result;
import rationals.properties.Similar;
import scalaz.std.java.util.map;
import search.Response.SingleResponse;
import sources.core.ParallelAPICall;
import utils.ChainedSearchResult;

public class SimilarCreatorSearch extends SimilarSearch {

	public SimilarCreatorSearch() {
	}

	@Override
	public Promise<RecordsList> query(SimilarsQuery q) {
		RecordResource<?> r = getTheRecord(q);
//		prov = ((RecordResource.RecordDescriptiveData)r.getDescriptiveData()).;
		String prov = r.getProvenance().get(0).getProvider();
		Query iq = new Query();
		iq.setPageAndSize(1, 10);
		iq.addSource(Sources.WITHin);
		String creator = ((CulturalObjectData)r.getDescriptiveData()).getDccreator().get(Language.DEFAULT).get(0);
		iq.addClause(new Filter(Fields.descriptiveData_dccreator.fieldId(), creator));
		iq.addClause(new Filter(Fields.resourceType.fieldId(),"CulturalObject"));
		// just to make sure its ok
		return SimilarSearch.executeQuery(iq).map((mapSR)->{
			RecordsList recordsList = new RecordsList(Fields.descriptiveData_dccreator.fieldId(), 
					new Literal(Language.EN,"Same Creator"));
			SingleResponse loc = mapSR.get(Sources.WITHin);
			if (loc.count>0){
				recordsList.addRecords(loc.items);
			} else {
//				one of the rest?
			}
			return recordsList;
		});
	}
	
	

}
