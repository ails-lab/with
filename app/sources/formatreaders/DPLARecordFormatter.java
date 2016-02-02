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

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.javafx.animation.TickCalculation;

import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.WithMediaRights;
import model.EmbeddedMediaObject.WithMediaType;
import model.basicDataTypes.ProvenanceInfo.Sources;
import model.basicDataTypes.Language;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import sources.FilterValuesMap;
import sources.core.CommonFilters;
import sources.core.Utils;
import sources.utils.JsonContextRecord;
import utils.ListUtils;

public class DPLARecordFormatter extends CulturalRecordFormatter {

	public DPLARecordFormatter(FilterValuesMap map) {
		super(map);
		object = new CulturalObject();
	}

	@Override
	public CulturalObject fillObjectFrom(JsonContextRecord rec) {
		CulturalObjectData model = (CulturalObjectData) object.getDescriptiveData();
		Language[] language = null;
		if (rec.getValue("sourceResource.language")!=null){
			JsonNode langs = rec.getValue("sourceResource.language");
			language = new Language[langs.size()];
			for (int i = 0; i < langs.size(); i++) {
				language[i] = Language.getLanguage(langs.get(i).path("iso639_3").asText());
			}
			System.out.println(Arrays.toString(language));
		}
		rec.setLanguages(language);
		
		model.setDcspatial(rec.getMultiLiteralOrResourceValue("originalRecord.spatial"));
		
		model.setLabel(rec.getMultiLiteralValue("sourceResource.title"));
		model.setDescription(rec.getMultiLiteralValue("sourceResource.description"));
		model.setIsShownBy(rec.getLiteralOrResourceValue("hasView.@id"));
		model.setIsShownAt(rec.getLiteralOrResourceValue("isShownAt"));
		model.setDates(rec.getWithDateArrayValue("sourceResource.date.begin"));
		model.setDccontributor(rec.getMultiLiteralOrResourceValue("sourceResource.contributor"));
		model.setDccreator(rec.getMultiLiteralOrResourceValue("sourceResource.creator"));
		model.setKeywords(rec.getMultiLiteralOrResourceValue("sourceResource.subject"));
		String uriAt = model.getIsShownAt()==null?null:model.getIsShownAt().getURI();
		object.addToProvenance(new ProvenanceInfo(rec.getStringValue("dataProvider"), uriAt,null));
		object.addToProvenance(new ProvenanceInfo(rec.getStringValue("provider.name"), rec.getStringValue("provider.@id"), null));
		String recID = rec.getStringValue("id");
		String uri = "http://dp.la/item/"+recID;
		object.addToProvenance(
				new ProvenanceInfo(Sources.DPLA.toString(), uri, recID));
		List<String> rights = rec.getStringArrayValue("rights");
		String stringValue = rec.getStringValue("sourceResource.type");
		List<Object> translateToCommon = getValuesMap().translateToCommon(CommonFilters.TYPE.getId(), stringValue);
		WithMediaType type = translateToCommon==null?null:(WithMediaType) translateToCommon.get(0);
		WithMediaRights withRights = (rights==null || rights.size()==0)?null:(WithMediaRights) getValuesMap().translateToCommon(CommonFilters.RIGHTS.getId(), rights.get(0)).get(0);
		String uri3 = rec.getStringValue("object");
		String uri2 = model.getIsShownBy()==null?null:model.getIsShownBy().getURI();
		if (Utils.hasInfo(uri3)){
			EmbeddedMediaObject medThumb = new EmbeddedMediaObject();
			medThumb.setUrl(uri3);
			medThumb.setType(type);
			medThumb.setParentID(uri2);
			if (Utils.hasInfo(rights))
			medThumb.setOriginalRights(new LiteralOrResource(rights.get(0)));
			medThumb.setWithRights(withRights);
			object.addMedia(MediaVersion.Thumbnail, medThumb);
		}
		if (Utils.hasInfo(uri2)){
			EmbeddedMediaObject med = new EmbeddedMediaObject();
			med.setParentID("self");
			med.setUrl(uri2);
			if (Utils.hasInfo(rights))
			med.setOriginalRights(new LiteralOrResource(rights.get(0)));
			med.setWithRights(withRights);
			med.setType(type);
			object.addMedia(MediaVersion.Original, med);
		}
		return object;
		
	}

}
