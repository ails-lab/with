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

import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.WithMediaRights;
import model.EmbeddedMediaObject.WithMediaType;
import model.basicDataTypes.Language;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import search.FiltersFields;
import search.Sources;
import sources.FilterValuesMap;
import sources.core.Utils;
import sources.utils.JsonContextRecord;
import sources.utils.StringUtils;

public class DDBRecordFormatter extends CulturalRecordFormatter {

	public DDBRecordFormatter() {
		super(FilterValuesMap.getMap(Sources.DDB));
		object = new CulturalObject();
	}

	@Override
	public CulturalObject fillObjectFrom(JsonContextRecord rec) {
		CulturalObjectData model = (CulturalObjectData) object.getDescriptiveData();
		Language[] language = null;
		language = getLanguagesFromText(rec.getStringValue("title"), 
				rec.getStringValue("subtitle"));
		rec.setLanguages(language);

		model.setDclanguage(StringUtils.getLiteralLanguages(language));
		model.setLabel(rec.getMultiLiteralValue("title"));
		model.setDescription(rec.getMultiLiteralValue("subtitle"));
		
		String id = rec.getStringValue("id");
		object.addToProvenance(new ProvenanceInfo(Sources.DDB.toString(),
				"https://www.deutsche-digitale-bibliothek.de/item/" + id, id));
		
		List<String> rights = rec.getStringArrayValue("license.@resource");
		String stringValue = rec.getStringValue("media");
		List<Object> translateToCommon = getValuesMap().translateToCommon(FiltersFields.TYPE.getFilterId(), stringValue);
		WithMediaType type = WithMediaType.getType(translateToCommon.get(0).toString());
		WithMediaRights withRights = (rights==null || rights.size()==0)?null:(WithMediaRights) getValuesMap().translateToCommon(FiltersFields.RIGHTS.getFilterId(), rights.get(0)).get(0);
		String uri3 = "https://www.deutsche-digitale-bibliothek.de/" + rec.getStringValue("thumbnail");
		
		if (Utils.hasInfo(uri3)){
			EmbeddedMediaObject medThumb = new EmbeddedMediaObject();
			medThumb.setUrl(uri3);
			medThumb.setType(type);
			if (Utils.hasInfo(rights))
			medThumb.setOriginalRights(new LiteralOrResource(rights.get(0)));
			medThumb.setWithRights(withRights);
			object.addMedia(MediaVersion.Thumbnail, medThumb);
		}
		

		return object;
	}

}
