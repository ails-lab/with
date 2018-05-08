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
import model.basicDataTypes.MultiLiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.Resource;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import search.FiltersFields;
import search.Sources;
import sources.FilterValuesMap;
import sources.core.Utils;
import sources.utils.JsonContextRecord;
import sources.utils.StringUtils;

public class RijksmuseumItemRecordFormatter extends CulturalRecordFormatter {

	public RijksmuseumItemRecordFormatter() {
		super(FilterValuesMap.getMap(Sources.Rijksmuseum));
		
		object = new CulturalObject();
	}

	@Override
	public CulturalObject fillObjectFrom(JsonContextRecord rec) {
		CulturalObjectData model = (CulturalObjectData) object.getDescriptiveData();
		rec.enterContext("artObject");

		String id = rec.getStringValue("objectNumber");
		
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
											rec.getStringValue("titles"),
											rec.getStringValue("longTitle"),
											rec.getStringValue("description"));
		}

		rec.setLanguages(language);

		model.setDclanguage(StringUtils.getLiteralLanguages(language));

		
		// List<Object> vals =
		// getValuesMap().translateToCommon(CommonFilters.TYPE.getId(),
		// rec.getStringValue("ProvidedCHO.type"));
		// WithMediaType type = WithMediaType.getType(vals.get(0).toString());

		// model.setDcidentifier(rec.getMultiLiteralOrResourceValue("dcIdentifier"));
		// model.setDccoverage(rec.getMultiLiteralOrResourceValue("dcCoverage"));
		// model.setDcrights(rec.getMultiLiteralOrResourceValue("dcRights"));
//		model.setDctermsspatial(rec.getMultiLiteralOrResourceValue("location","classification.places"));
		model.setCountry(new MultiLiteralOrResource(Language.EN,"Netherlands"));
		model.setCity(new MultiLiteralOrResource(Language.EN,"Amsterdam"));
		model.setCoordinates(StringUtils.getPoint("52.36", "4.885278"));
		// model.setDccreated(rec.getWithDateArrayValue("dctermsCreated"));
		// model.setDcformat(rec.getMultiLiteralOrResourceValue("dcFormat"));
		// model.setDctermsmedium(rec.getMultiLiteralOrResourceValue("dctermsMedium"));
		// model.setIsRelatedTo(rec.getMultiLiteralOrResourceValue("edmIsRelatedTo"));
		model.setDccreator(rec.getMultiLiteralOrResourceValue("principalMaker"));
		model.setDccontributor(rec.getMultiLiteralOrResourceValue("makers[.*].name"));
		model.setKeywords(rec.getMultiLiteralOrResourceValue("classification.iconClassDescription","materials","techniques","objectTypes"));
		model.setLabel(rec.getMultiLiteralValue("title", "titles","longTitle"));
		model.setDescription(rec.getMultiLiteralValue("description","subTitle","scLabelLine"));
		model.setDates(StringUtils.getDates(rec.getStringArrayValue("dating.year")));
		List<String> types = rec.getStringArrayValue("objectTypes");
		WithMediaType type = null;
		for (String string : types) {
			type = WithMediaType.getType(getValuesMap().translateToCommon(FiltersFields.TYPE.getFilterId(), string).get(0).toString());
			if (type.isKnown()){
				break;
			}
		}
		// model.setDctype(rec.getMultiLiteralOrResourceValue("WebResource.type"));
		// model.setDccontributor(rec.getMultiLiteralOrResourceValue("dcContributor"));

//		LiteralOrResource rights = rec.getLiteralOrResourceValue("WebResource.rights");
//		WithMediaRights withMediaRights = rights == null ? null
//				: (WithMediaRights) getValuesMap().translateToCommon(CommonFilters.RIGHTS.getId(), rights.getURI())
//						.get(0);

//		model.getDates().addAll(rec.getWithDateArrayValue("dcDate"));

//		model.setIsShownAt(rec.getLiteralOrResourceValue("Aggregation.isShownAt"));
//		model.setIsShownBy(rec.getLiteralOrResourceValue("Aggregation.isShownBy"));

		object.addToProvenance(new ProvenanceInfo(Sources.Rijksmuseum.toString(), 
				"https://www.rijksmuseum.nl/en/collection/" + id, id));
		
		
		// List<String> theViews = rec.getStringArrayValue("hasView");

		// model.getDates().addAll(rec.getWithDateArrayValue("year"));
		Resource isShownBy = model.getIsShownBy();
		String uri2 = isShownBy == null ? null : isShownBy.toString();
		String uri3 = rec.getStringValue("webImage.url");
		if (Utils.hasInfo(uri3)) {
			EmbeddedMediaObject medThumb = new EmbeddedMediaObject();
			medThumb.setUrl(uri3);
			medThumb.setWidth(rec.getIntValue("webImage.width"));
			medThumb.setHeight(rec.getIntValue("webImage.height"));
			medThumb.setType(type);
			medThumb.setOriginalRights(new LiteralOrResource("http://creativecommons.org/publicdomain/zero/1.0/deed.en"));
			medThumb.setWithRights(WithMediaRights.Public);
			object.addMedia(MediaVersion.Original, medThumb);
		}

		return object;
	}

}
