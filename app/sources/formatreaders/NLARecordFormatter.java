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

import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.WithMediaRights;
import model.EmbeddedMediaObject.WithMediaType;
import model.basicDataTypes.Language;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.ProvenanceInfo.Sources;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import play.Logger;
import sources.FilterValuesMap;
import sources.core.CommonFilters;
import sources.core.Utils;
import sources.utils.JsonContextRecord;
import sources.utils.StringUtils;

public class NLARecordFormatter extends CulturalRecordFormatter {

	public NLARecordFormatter() {
		super(FilterValuesMap.getNLAMap());
		object = new CulturalObject();
	}

	@Override
	public CulturalObject fillObjectFrom(JsonContextRecord rec) {
		String id = rec.getStringValue("id");	
		Language[] language = null;
		List<String> langs = rec.getStringArrayValue("language");
		if (Utils.hasInfo(langs)){
			language = new Language[langs.size()];
			for (int i = 0; i < langs.size(); i++) {
				language[i] = Language.getLanguage(langs.get(i));
			}
		}
		if (!Utils.hasInfo(language)){
			language = getLanguagesFromText(rec.getStringValue("title"),
											rec.getStringValue("abstract"),
											rec.getStringValue("subject"));
		}
		rec.setLanguages(language);
		
		CulturalObjectData model = (CulturalObjectData) object.getDescriptiveData();
		model.setDclanguage(StringUtils.getLiteralLanguages(language));
		model.setLabel(rec.getMultiLiteralValue("title"));
		model.setDescription(rec.getMultiLiteralValue("abstract"));
		model.setDccontributor(rec.getMultiLiteralOrResourceValue("contributor"));
		model.setDates(rec.getWithDateArrayValue("issued"));
		model.getDates().addAll(rec.getWithDateArrayValue("year"));
		model.getDates().addAll(rec.getWithDateArrayValue("date"));
		model.setKeywords(rec.getMultiLiteralOrResourceValue("subject"));
		model.setDcdate(rec.getWithDateArrayValue("date"));
		model.setIsShownBy(rec.getLiteralOrResourceValue("identifier[type=url,linktype=fulltext|restricted|unknown].value"));
		
		
		object.addToProvenance(new ProvenanceInfo(Sources.NLA.toString(), rec.getStringValue("troveUrl"), id));
		
		List<String> rights = rec.getStringArrayValue("rights");
		String stringValue = rec.getStringValue("type");
		List<Object> translateToCommon = getValuesMap().translateToCommon(CommonFilters.TYPE.getId(), stringValue);
		WithMediaType type = (Utils.hasInfo(translateToCommon))?WithMediaType.getType(translateToCommon.get(0).toString()):WithMediaType.OTHER;
		WithMediaRights withRights = (rights==null || rights.size()==0)?null:(WithMediaRights) getValuesMap().translateToCommon(CommonFilters.RIGHTS.getId(), rights.get(0)).get(0);
		String uri3 = rec.getStringValue("identifier[type=url,linktype=thumbnail].value");
		String uri2 = model.getIsShownBy()==null?null:model.getIsShownBy().getURI();
		
		if (Utils.hasInfo(uri3)){
			EmbeddedMediaObject medThumb = new EmbeddedMediaObject();
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
		// object.setCreator(rec.getStringValue("contributor"));
		// // object.setContributors(rec.getStringArrayValue("contributor"));
		// // TODO: add years
		// object.setYears(StringUtils.getYears(rec.getStringArrayValue("issued")));
		// // TODO: add rights
		// // object.setItemRights(rec.getStringValue("sourceResource.rights"));

	}

	

}
