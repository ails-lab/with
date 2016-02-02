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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.optimaize.langdetect.DetectedLanguage;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;

import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.basicDataTypes.ILiteral;
import model.basicDataTypes.Language;
import model.basicDataTypes.ProvenanceInfo.Sources;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import play.Logger;
import sources.FilterValuesMap;
import sources.core.JsonContextRecordFormatReader;
import sources.core.Utils;
import sources.utils.JsonContextRecord;
import sources.utils.StringUtils;
import utils.ListUtils;

public abstract class CulturalRecordFormatter extends JsonContextRecordFormatReader<CulturalObject> {

	private FilterValuesMap valuesMap;

	public CulturalRecordFormatter(FilterValuesMap valuesMap) {
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

	public CulturalObject readObjectFrom(JsonNode text) {
		object = new CulturalObject();
		object.getAdministrative().getAccess().setIsPublic(true);

		CulturalObjectData model = new CulturalObjectData();
		object.setDescriptiveData(model);
		model.setMetadataRights(new LiteralOrResource("http://creativecommons.org/publicdomain/zero/1.0/"));
		model.setRdfType("http://www.europeana.eu/schemas/edm/ProvidedCHO");

		fillObjectFrom(text);

		List<ProvenanceInfo> provenance = object.getProvenance();
		int index = provenance.size() - 1;
		String resourceId = provenance.get(index).getResourceId();
		object.getAdministrative().setExternalId(resourceId);

		return object;
	}

	public FilterValuesMap getValuesMap() {
		return valuesMap;
	}

	public void setValuesMap(FilterValuesMap valuesMap) {
		this.valuesMap = valuesMap;
	}

}