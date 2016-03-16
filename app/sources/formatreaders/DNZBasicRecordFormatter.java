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

import akka.dispatch.Filter;
import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.WithMediaRights;
import model.EmbeddedMediaObject.WithMediaType;
import model.basicDataTypes.Language;
import model.basicDataTypes.ProvenanceInfo.Sources;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import play.Logger;
import sources.FilterValuesMap;
import sources.core.CommonFilters;
import sources.core.Utils;
import sources.utils.JsonContextRecord;
import sources.utils.StringUtils;

public class DNZBasicRecordFormatter extends CulturalRecordFormatter {

	public DNZBasicRecordFormatter() {
		super(FilterValuesMap.getDNZMap());
		object = new CulturalObject();
	}

	@Override
	public CulturalObject fillObjectFrom(JsonContextRecord rec) {
		
		String id = rec.getStringValue("id");
		CulturalObjectData model = (CulturalObjectData) object.getDescriptiveData();
		// TODO read the language
		Language[] language = null;
		if (rec.getValue("language")!=null){
			JsonNode langs = rec.getValue("language");
			language = new Language[langs.size()];
			for (int i = 0; i < langs.size(); i++) {
				language[i] = Language.getLanguage(langs.get(i).asText());
			}
			Logger.info("["+id+"] Item Languages " + Arrays.toString(language));
		}
		if (!Utils.hasInfo(language)){
			language = getLanguagesFromText(rec.getStringValue("title"),
											rec.getStringValue("description"),
											rec.getStringValue("additional_description"),
											rec.getStringValue("fulltext"));
		}
		rec.setLanguages(language);
		model.setDclanguage(StringUtils.getLiteralLanguages(language));
		model.setLabel(rec.getMultiLiteralValue("title"));
		model.setDescription(rec.getMultiLiteralValue("description","additional_description"));
		model.setAltLabels(rec.getMultiLiteralValue("alternative_title"));
		model.setDcidentifier(rec.getMultiLiteralOrResourceValue("dc_identifier"));
		model.setIsShownBy(rec.getLiteralOrResourceValue("object_url","thumbnail_url", "large_thumbnail_url"));
		model.setIsShownAt(rec.getLiteralOrResourceValue("landing_url"));
		model.setDates(rec.getWithDateArrayValue("date"));
		model.setDccreator(rec.getMultiLiteralOrResourceValue("creator"));
		model.setDccontributor(rec.getMultiLiteralOrResourceValue("contributor"));
		model.setDctype(rec.getMultiLiteralOrResourceValue("dctype"));
		model.setKeywords(rec.getMultiLiteralOrResourceValue("category"));
		object.addToProvenance(new ProvenanceInfo(rec.getStringValue("collection[0]"), rec.getStringValue("landing_url"),null));
		object.addToProvenance(
				new ProvenanceInfo(rec.getStringValue("content_partner[0]")));
		object.addToProvenance(
				new ProvenanceInfo(Sources.DigitalNZ.toString(), "http://www.digitalnz.org/objects/" + id, id));
		
		
		//TODO EmbeddedMediaObject.originalRights <- rights_url
		List<String> rights = rec.getStringArrayValue("usage");
		String stringValue = rec.getStringValue("category");
		List<Object> translateToCommon = getValuesMap().translateToCommon(CommonFilters.TYPE.getId(), stringValue);
		WithMediaType type = null;
		for (Object object : translateToCommon) {
			WithMediaType type2 = WithMediaType.getType(object.toString());
			if (type2!=null){
				type = type2;
				if (!type.equals(WithMediaType.OTHER))
				break;
			}
		}
		WithMediaRights withRights = (rights==null || rights.size()==0)?null:(WithMediaRights) getValuesMap().translateToCommon(CommonFilters.RIGHTS.getId(), rights.get(0)).get(0);
		
		
		
		String uri3 = rec.getStringValue("thumbnail_url","object_url");//TODO MediaVersion.Thumbnail <- thumbnail_url or object_url
		String uri2 = rec.getStringValue("large_thumbnail_url");
		if (Utils.hasInfo(uri3)){
			EmbeddedMediaObject medThumb = new EmbeddedMediaObject();
			medThumb.setUrl(uri3);
			medThumb.setType(type);
			if (Utils.hasInfo(rights))
			medThumb.setOriginalRights(new LiteralOrResource(rights.get(0)).fillDEF());
			medThumb.setWithRights(withRights);
			object.addMedia(MediaVersion.Thumbnail, medThumb);
		}
		if (Utils.hasInfo(uri2)){  //TODO  MediaVersion.Original <- large_thumbnail_url
			EmbeddedMediaObject med = new EmbeddedMediaObject();
			med.setUrl(uri2);
			if (Utils.hasInfo(rights))
			med.setOriginalRights(new LiteralOrResource(rights.get(0)).fillDEF());
			med.setWithRights(withRights);
			med.setType(type);
			object.addMedia(MediaVersion.Original, med);
		}
		
		uri3 = rec.getStringValue("large_thumbnail_url");
		if (Utils.hasInfo(uri3)){
			EmbeddedMediaObject medThumb = new EmbeddedMediaObject();
			medThumb.setUrl(uri3);
			medThumb.setType(type);
			if (Utils.hasInfo(rights))
			medThumb.setOriginalRights(new LiteralOrResource(rights.get(0)).fillDEF());
			medThumb.setWithRights(withRights);
			object.addMediaView(MediaVersion.Thumbnail, medThumb);
		}
		
		
		return object;
	}

}
