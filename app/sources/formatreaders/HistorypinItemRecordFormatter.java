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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;

import jdk.nashorn.internal.runtime.regexp.RegExp;
import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.WithMediaRights;
import model.EmbeddedMediaObject.WithMediaType;
import model.basicDataTypes.Language;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.MultiLiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import search.FiltersFields;
import search.Sources;
import sources.FilterValuesMap;
import sources.core.Utils;
import sources.utils.JsonContextRecord;
import sources.utils.StringUtils;

public class HistorypinItemRecordFormatter extends CulturalRecordFormatter {

	public HistorypinItemRecordFormatter() {
		super(FilterValuesMap.getMap(Sources.Historypin));
		object = new CulturalObject();
	}

	@Override
	public CulturalObject fillObjectFrom(JsonContextRecord rec) {
		CulturalObjectData model = (CulturalObjectData) object.getDescriptiveData();
		

		Language[] language = null;
//		List<String> langs = rec.getStringArrayValue(false,"language","dcLanguage");
//		if (Utils.hasInfo(langs)){
//			language = new Language[langs.size()];
//			for (int i = 0; i < langs.size(); i++) {
//				language[i] = Language.getLanguage(langs.get(i));
//			}
////			System.out.println(Arrays.toString(language));
//		}
		if (!Utils.hasInfo(language)){
			language = getLanguagesFromText(rec.getStringValue("description"),
											rec.getStringValue("caption"),
											rec.getStringValue("dcSubject"));
		}
		
		model.setDctermsspatial(rec.getMultiLiteralOrResourceValue("geo_tags"));
//		model.setCountry(rec.getMultiLiteralOrResourceValue("country"));;
		model.setCoordinates(StringUtils.getPoint(rec.getStringValue("location.lat"),rec.getStringValue("location.lng")));
		model.setDclanguage(StringUtils.getLiteralLanguages(language));
		model.setLabel(rec.getMultiLiteralValue("caption"));
		model.setDescription(rec.getMultiLiteralValue("description"));
//		model.setAltLabels(rec.getMultiLiteralValue("altLabel"));
		model.setIsShownBy(rec.getLiteralOrResourceValue("media_url"));
//		model.setIsShownAt(rec.getLiteralOrResourceValue("media_url"));
		model.setDates(rec.getWithDateArrayValue("date"));
		model.setDccreator(rec.getMultiLiteralOrResourceValue("user_name"));
		model.getDccreator().addLiteral("http://www.historypin.org/en/channels/view/"+rec.getStringValue("user_id"));
		model.setDccontributor(rec.getMultiLiteralOrResourceValue("dcContributor"));
		model.setKeywords(rec.getMultiLiteralOrResourceValue("tags[.*]text"));
//		object.addToProvenance(new ProvenanceInfo(rec.getStringValue("dataProvider"), model.getIsShownAt()==null?null:model.getIsShownAt().getURI(),null));
//		object.addToProvenance(new ProvenanceInfo(rec.getStringValue("provider")));
		String recID = rec.getStringValue("id");
		String uri = "http://www.historypin.org/en/explore/pin/"+recID;
		object.addToProvenance(
				new ProvenanceInfo(Sources.Historypin.toString(), uri, recID));
		List<String> rights = rec.getStringArrayValue("license");
		List<Object> translateToCommon = getValuesMap().translateToCommon(FiltersFields.TYPE.getId(), rec.getStringValue("type"));
		WithMediaType type = (WithMediaType.getType(translateToCommon.get(0).toString())) ;
		WithMediaRights withRights = (!Utils.hasInfo(rights))?null:WithMediaRights.getRights(
				getValuesMap().translateToCommon(FiltersFields.RIGHTS.getId(), rights.get(0)).get(0).toString());
		System.out.println(getValuesMap().translateToCommon(FiltersFields.RIGHTS.getId(), rights.get(0)).get(0).toString());
		
		String uri3 = "http://www.historypin.org"+rec.getStringValue("display.content");
		String uri2 = model.getIsShownBy()==null?null:model.getIsShownBy().getURI();
		if (Utils.hasInfo(uri3)){
			EmbeddedMediaObject medThumb = new EmbeddedMediaObject();
			uri3 = Utils.decodeURL(uri3);
			medThumb.setUrl(uri3);
			medThumb.setType(type);
			if (Utils.hasInfo(rights))
			medThumb.setOriginalRights(new LiteralOrResource(rights.get(0)));
			medThumb.setWithRights(withRights);
			object.addMedia(MediaVersion.Thumbnail, medThumb);
		}
		if (Utils.hasInfo(uri2)){
			EmbeddedMediaObject med = new EmbeddedMediaObject();
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
