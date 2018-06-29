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

import java.util.HashMap;
import java.util.List;

import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.WithMediaRights;
import model.EmbeddedMediaObject.WithMediaType;
import model.basicDataTypes.Language;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.Resource;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import search.FiltersFields;
import search.Sources;
import sources.FilterValuesMap;
import sources.core.Utils;
import sources.utils.JsonContextRecord;
import sources.utils.ListMap;
import sources.utils.StringUtils;

public class DDBItemRecordFormatter extends CulturalRecordFormatter {
	
	public DDBItemRecordFormatter() {
		super(FilterValuesMap.getMap(Sources.DDB));
		object = new CulturalObject();
	}

	@Override
	public CulturalObject fillObjectFrom(JsonContextRecord rec) {
		CulturalObjectData model = (CulturalObjectData) object.getDescriptiveData();
		rec.enterContext("RDF");
		String stringValue = rec.getStringValue("WebResource.type");
		stringValue = "mediatype_"+stringValue.substring(stringValue.length()-3);
		List<Object> vals = getValuesMap().translateToCommon(FiltersFields.TYPE.getFilterId(), stringValue);
		WithMediaType type = (Utils.hasInfo(vals))? WithMediaType.getType(vals.get(0).toString()):WithMediaType.OTHER;
		
		// TODO read the language
		
		Language[] language = null;
		List<String> langs = rec.getStringArrayValue("ProvidedCHO.language","ProvidedCHO.language.@resource"
				,"LinguisticSystem.value");
		if (langs!=null){
			language = new Language[langs.size()];
			for (int i = 0; i < langs.size(); i++) {
				language[i] = Language.getLanguage(langs.get(i));
			}
		}
		if (!Utils.hasInfo(language)){
			language = getLanguagesFromText(rec.getStringValue("ProvidedCHO.title"),
											rec.getStringValue("ProvidedCHO.description"),
											rec.getStringValue("Concept[.*].prefLabel"));
		}
		rec.setLanguages(language);
		model.setDclanguage(StringUtils.getLiteralLanguages(language));
		
		
//		model.setDcidentifier(rec.getMultiLiteralOrResourceValue("dcIdentifier"));
//		model.setDccoverage(rec.getMultiLiteralOrResourceValue("dcCoverage"));
//		model.setDcrights(rec.getMultiLiteralOrResourceValue("dcRights"));
		model.setDctermsspatial(rec.getMultiLiteralOrResourceValue("Place.prefLabel","Place.@about"));
		model.setCoordinates(StringUtils.getPoint(rec.getStringValue("Place.lat"), (rec.getStringValue("Place.long"))));
		model.setDccreator(rec.getMultiLiteralOrResourceValue("ProvidedCHO.creator"));
//		model.setDccreated(rec.getWithDateArrayValue("dctermsCreated"));
//		model.setDcformat(rec.getMultiLiteralOrResourceValue("dcFormat"));
//		model.setDctermsmedium(rec.getMultiLiteralOrResourceValue("dctermsMedium"));
//		model.setIsRelatedTo(rec.getMultiLiteralOrResourceValue("edmIsRelatedTo"));
		model.setLabel(rec.getMultiLiteralValue("ProvidedCHO.title"));
		model.setDescription(rec.getMultiLiteralValue("ProvidedCHO.description"));
		model.setKeywords(rec.getMultiLiteralOrResourceValue("Concept[.*].prefLabel"));
		model.setDates(rec.getWithDateArrayValue("ProvidedCHO.issued","ProvidedCHO.date"));
		model.setDctype(rec.getMultiLiteralOrResourceValue("WebResource.type"));
//		model.setDccontributor(rec.getMultiLiteralOrResourceValue("dcContributor"));
		
		LiteralOrResource rights = rec.getLiteralOrResourceValue("WebResource.rights");
		String rightsString = rec.getStringValue("WebResource.rights");
		WithMediaRights withMediaRights = getWithMediaRights(rightsString);
		
		model.setIsShownAt(new Resource( rec.getStringValue("Aggregation.isShownAt")));
		model.setIsShownBy(new Resource( rec.getStringValue("Aggregation.isShownBy")));
		String uriAt = rec.getStringValue("Aggregation.dataProvider[1].@resource");
		ProvenanceInfo provInfo = new ProvenanceInfo(rec.getStringValue("Aggregation.dataProvider[0]"),uriAt,null);
		object.addToProvenance(provInfo);
		
		provInfo = new ProvenanceInfo(rec.getStringValue("Aggregation.provider"));
		object.addToProvenance(provInfo);
		
		
		String recID = rec.getStringValue("ProvidedCHO.identifier");
		String uri = rec.getStringValue("ProvidedCHO.@about");
		object.addToProvenance(
				new ProvenanceInfo(Sources.DDB.toString(),  uri,recID));

//		List<String> theViews = rec.getStringArrayValue("hasView");
		LiteralOrResource ro = rec.getLiteralOrResourceValue("edmObject");
		

//		model.getDates().addAll(rec.getWithDateArrayValue("year"));
		Resource isShownBy = model.getIsShownBy();
		String uri2 = isShownBy==null?null:isShownBy.toString();
		String uri3 = ro==null?null:ro.getURI();
		if (Utils.hasInfo(uri3)){
			EmbeddedMediaObject medThumb = new EmbeddedMediaObject();
			medThumb.setUrl(uri3);
			medThumb.setType(type);
			if (Utils.hasInfo(rights))
			medThumb.setOriginalRights(rights);
			medThumb.setWithRights(withMediaRights);
			object.addMedia(MediaVersion.Thumbnail, medThumb);
		}
		
		if (Utils.hasInfo(uri2)){
			EmbeddedMediaObject med = new EmbeddedMediaObject();
			med.setType(type);
			med.setUrl(uri2);
			if (Utils.hasInfo(rights))
			med.setOriginalRights(rights);
			med.setWithRights(withMediaRights);
			object.addMedia(MediaVersion.Original, med);
		}
		
//		if (Utils.hasInfo(theViews)){
//			for (String string : theViews) {
//				EmbeddedMediaObject med = new EmbeddedMediaObject();
//				med.setType(type);
//				med.setUrl(string);
//				med.setOriginalRights(rights);
//				med.setWithRights(withMediaRights);
//				object.addMediaView(MediaVersion.Original, med);
//			}
//		}
		
		// TODO fill the views
		return object;
	}

}
