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


package sources.formatreaders;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.Provider.Sources;
import model.basicDataTypes.KeySingleValuePair.LiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import sources.core.JsonContextRecordFormatReader;
import sources.utils.JsonContextRecord;

public abstract class CulturalRecordFormatter extends JsonContextRecordFormatReader<CulturalObject> {

	public CulturalRecordFormatter() {
		super();
	}
	
	public CulturalObject readObjectFrom(JsonNode text){
		CulturalObject culturalObject = new CulturalObject();
		culturalObject.getAdministrative().getAccess().setPublic(true);
		object = culturalObject;
		fillObjectFrom(text);
		List<ProvenanceInfo> provenance = object.getProvenance();
		int index = provenance.size()-1;
		String resourceId = provenance.get(index).getResourceId();
		object.getAdministrative().setExternalId(resourceId);
		return object;
	}


}