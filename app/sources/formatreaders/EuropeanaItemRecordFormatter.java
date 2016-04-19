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
import model.basicDataTypes.ProvenanceInfo.Sources;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import sources.FilterValuesMap;
import sources.core.CommonFilters;
import sources.core.Utils;
import sources.utils.JsonContextRecord;
import sources.utils.StringUtils;

public class EuropeanaItemRecordFormatter extends CulturalRecordFormatter {

	public EuropeanaItemRecordFormatter() {
		super(FilterValuesMap.getEuropeanaMap());
		object = new CulturalObject();
	}

	@Override
	public CulturalObject fillObjectFrom(JsonContextRecord rec) {
		CulturalObjectData model = (CulturalObjectData) object.getDescriptiveData();
		
		String stringValue = rec.getStringValue("type");
		List<Object> vals = getValuesMap().translateToCommon(CommonFilters.TYPE.getId(), stringValue);
		WithMediaType type = (WithMediaType.getType(vals.get(0).toString())) ;
		

//		String id = rec.getStringValue("objectNumber");
		
		Language[] language = null;
		List<String> langs = rec.getStringArrayValue("proxies[0].dcLanguage","europeanaAggregation.edmLanguage","language");
		if (Utils.hasInfo(langs)){
			language = new Language[langs.size()];
			for (int i = 0; i < langs.size(); i++) {
				language[i] = Language.getLanguage(langs.get(i));
			}
		}
		if (!Utils.hasInfo(language)){
			language = getLanguagesFromText(rec.getStringValue("dcTitle"),
											rec.getStringValue("titles"),
											rec.getStringValue("dcSubject"));
		}

		rec.setLanguages(language);
		

		model.setCountry(rec.getMultiLiteralOrResourceValue("proxies[0].country","europeanaAggregation.edmCountry"));
		model.setCoordinates(StringUtils.getPoint(rec.getStringValue("places[.*].latitude"), rec.getStringValue("places[.*].longitude")));

		model.setKeywords(rec.getMultiLiteralOrResourceValue("proxies[0].dcSubject","proxies[1].dcSubject"));
		model.setDates(rec.getWithDateArrayValue("year"));

		model.setDctermsspatial(rec.getMultiLiteralOrResourceValue("proxies[0].dctermsSpatial","places[.*].about","places[.*].about","places[.*].prefLabel"));
		rec.enterContext("proxies[0]");
		
		model.setAltLabels(rec.getMultiLiteralValue("dctermsAlternative"));
		model.setDclanguage(StringUtils.getLiteralLanguages(language));
		model.setDcidentifier(rec.getMultiLiteralOrResourceValue("dcIdentifier"));
		model.setDccoverage(rec.getMultiLiteralOrResourceValue("dcCoverage"));
		model.setDcrights(rec.getMultiLiteralOrResourceValue("dcRights"));
		model.setDccreator(rec.getMultiLiteralOrResourceValue("dcCreator"));
		model.setDccreated(rec.getWithDateArrayValue("dctermsCreated"));
		model.setDcformat(rec.getMultiLiteralOrResourceValue("dcFormat"));
		model.setDctermsmedium(rec.getMultiLiteralOrResourceValue("dctermsMedium"));
		model.setIsRelatedTo(rec.getMultiLiteralOrResourceValue("edmIsRelatedTo"));
		model.setLabel(rec.getMultiLiteralValue("dcTitle","title"));
		model.setDescription(rec.getMultiLiteralValue("dcDescription"));
		model.getDates().addAll(rec.getWithDateArrayValue("dcDate"));
		model.setDctype(rec.getMultiLiteralOrResourceValue("dcType"));
		model.setDccontributor(rec.getMultiLiteralOrResourceValue("dcContributor"));

		rec.exitContext();
		rec.enterContext("proxies[1]");
		
		model.getDates().addAll(rec.getWithDateArrayValue("dcDate"));

		rec.exitContext();
		rec.enterContext("aggregations[0]");
		LiteralOrResource rightsLiteral = rec.getLiteralOrResourceValue("edmRights");
		LiteralOrResource rights = rightsLiteral;
		String rightsString = rec.getStringValue("edmRights");
		WithMediaRights withMediaRights = (!Utils.hasInfo(rightsString))?null:
			(WithMediaRights.getRights(getValuesMap().translateToCommon(CommonFilters.RIGHTS.getId(),
					rightsString).get(0).toString()));
		
		model.setIsShownAt(rec.getLiteralOrResourceValue("edmIsShownAt"));
		model.setIsShownBy(rec.getLiteralOrResourceValue("edmIsShownBy"));
		String uriAt = model.getIsShownAt()==null?null:model.getIsShownAt().getURI();
		ProvenanceInfo provInfo = new ProvenanceInfo(rec.getStringValue("edmDataProvider.def[0]"),uriAt,null);
		object.addToProvenance(provInfo);
		
		provInfo = new ProvenanceInfo(rec.getStringValue("edmProvider.def[0]"));
		object.addToProvenance(provInfo);
		
		
		String recID = rec.getStringValue("about");
		String uri = "http://www.europeana.eu/portal/record"+recID+".html";
		object.addToProvenance(
				new ProvenanceInfo(Sources.Europeana.toString(),  uri,recID));

		List<String> theViews = rec.getStringArrayValue("hasView");
		LiteralOrResource ro = rec.getLiteralOrResourceValue("edmObject");
		
		rec.exitContext();

		model.getDates().addAll(rec.getWithDateArrayValue("year"));
		LiteralOrResource isShownBy = model.getIsShownBy();
		String uri2 = isShownBy==null?null:isShownBy.getURI();
		String uri3 = ro==null?null:ro.getURI();
		if (Utils.hasInfo(uri3)){
			EmbeddedMediaObject medThumb = new EmbeddedMediaObject();
			medThumb.setUrl(uri3);
			medThumb.setType(type);
			medThumb.setOriginalRights(rights);
			medThumb.setWithRights(withMediaRights);
			object.addMedia(MediaVersion.Thumbnail, medThumb);
		}
		
		if (Utils.hasInfo(uri2)){
			EmbeddedMediaObject med = new EmbeddedMediaObject();
			med.setType(type);
			med.setUrl(uri2);
			med.setOriginalRights(rights);
			med.setWithRights(withMediaRights);
			object.addMedia(MediaVersion.Original, med);
		}
		
		if (Utils.hasInfo(theViews)){
			for (String string : theViews) {
				EmbeddedMediaObject med = new EmbeddedMediaObject();
				med.setType(type);
				med.setUrl(string);
				med.setOriginalRights(rights);
				med.setWithRights(withMediaRights);
				object.addMediaView(MediaVersion.Original, med);
			}
		}
		
		// TODO fill the views
		return object;
	}

}
