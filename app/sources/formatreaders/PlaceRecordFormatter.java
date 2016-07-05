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

import model.basicDataTypes.Language;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.resources.AgentObject;
import model.resources.PlaceObject;
import model.resources.RecordResource;
import model.resources.PlaceObject.PlaceData;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import play.Logger;
import play.Logger.ALogger;
import sources.FilterValuesMap;
import sources.core.JsonContextRecordFormatReader;
import sources.core.Utils;
import sources.utils.JsonContextRecord;
import sources.utils.StringUtils;

public abstract class PlaceRecordFormatter extends JsonContextRecordFormatReader<PlaceObject> {
	public static final ALogger log = Logger.of( PlaceRecordFormatter.class );

	private FilterValuesMap valuesMap;

	public PlaceRecordFormatter(FilterValuesMap valuesMap) {
		super();
		this.valuesMap = valuesMap;
	}
	
	protected Language[] getLanguagesFromText(String... text) {
		String full = "";
		for (String string : text) {
			if (Utils.hasInfo(string)){
				full+=string;
			}
		}
		List<Language> res =StringUtils.getLanguages(full);
        Logger.info("["+full+"] Item Detected Languages " + res);
		return res.toArray(new Language[]{});
	}
	
	public PlaceObject readObjectFrom(JsonContextRecord text) {
		object = new PlaceObject();
		object.getAdministrative().getAccess().setIsPublic(true);

		PlaceData model = new PlaceData();
		object.setDescriptiveData(model);
		model.setMetadataRights(new LiteralOrResource("http://creativecommons.org/publicdomain/zero/1.0/"));
		model.setRdfType("http://www.europeana.eu/schemas/edm/ProvidedCHO");

		try {
			fillObjectFrom(text);
		} catch (Exception e) {
			log.error("Error Importing object from source",e );
		}
		List<ProvenanceInfo> provenance = object.getProvenance();
		int index = provenance.size() - 1;
		String resourceId = provenance.get(index).getResourceId();
		object.getAdministrative().setExternalId(resourceId);

		return object;
	}
	
	private PlaceObject internalOverwriteObjectFrom(JsonContextRecord text) {
		
		try {
			fillObjectFrom(text);
		} catch (Exception e) {
			log.error("Error Importing object from source",e);
		}

		return object;
	}

	public PlaceObject readObjectFrom(JsonNode text) {
		return readObjectFrom(new JsonContextRecord(text));
	}
	
	public PlaceObject overwriteObjectFrom(RecordResource object, JsonNode text) {
		if (object==null){
			return readObjectFrom(text);
		} else
		this.object = (PlaceObject)object;
		return internalOverwriteObjectFrom(new JsonContextRecord(text));
	}

	public FilterValuesMap getValuesMap() {
		return valuesMap;
	}

	public void setValuesMap(FilterValuesMap valuesMap) {
		this.valuesMap = valuesMap;
	}

}