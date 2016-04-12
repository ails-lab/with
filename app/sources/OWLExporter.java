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


package sources;

import java.util.List;

import model.basicDataTypes.Language;
import model.basicDataTypes.MultiLiteral;
import model.resources.CulturalObject;
import model.resources.RecordResource.RecordDescriptiveData;
import sources.core.Utils;

public class OWLExporter {
	
	public OWLExporter(){
		
	}
	
	public void exportClassAssertion(String className, String instance){
		
	}
	
	public void exportRoleAssertion(String roleName, String instanceA, String instanceB) {

	}
	
	public void exportDataPropertyAssertion(String propertyName, String instance, Object data) {
		
	}
	
	public void flush(){
		// TODO save the ontology
	}
	
	public static class CulturalItemOWLExporter extends OWLExporter{
		public void exportItem(CulturalObject item){
			String instance = item.getDbId().toString();
			RecordDescriptiveData descriptiveData = item.getDescriptiveData();
			String label = toText(descriptiveData.getLabel());
		}

		private String toText(MultiLiteral literal) {
			if (Utils.hasInfo(literal)){
				List<String> list = literal.get(Language.DEFAULT);
				return list.toString();
			} else
				return null;
		}
	}

}
