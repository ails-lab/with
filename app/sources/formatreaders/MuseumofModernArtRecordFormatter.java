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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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

public class MuseumofModernArtRecordFormatter extends CulturalRecordFormatter {

	public MuseumofModernArtRecordFormatter() {
		super(FilterValuesMap.getMap(Sources.Europeana));
		object = new CulturalObject();
	}

	@Override
	public CulturalObject fillObjectFrom(JsonContextRecord rec) {
		CulturalObjectData model = (CulturalObjectData) object.getDescriptiveData();
		
		String stringValue = rec.getStringValue("Classification");
		List<Object> vals = getValuesMap().translateToCommon(CommonFilters.TYPE.getId(), stringValue);
		WithMediaType type = (WithMediaType.getType(vals.get(0).toString())) ;
		

//		String id = rec.getStringValue("objectNumber");
		
		Language[] language = null;
		if (!Utils.hasInfo(language)){
			language = getLanguagesFromText(rec.getStringValue("Title"),
											rec.getStringValue("CreditLine"));
		}

		rec.setLanguages(language);
		

		model.setCountry(rec.getMultiLiteralOrResourceValue("Nationality"));
		
//		model.setKeywords(rec.getMultiLiteralOrResourceValue("proxies[0].dcSubject","proxies[1].dcSubject"));
		model.setDates(rec.getWithDateArrayValue("Date"));

		model.setDclanguage(StringUtils.getLiteralLanguages(language));
		model.setDccreator(rec.getMultiLiteralOrResourceValue("Artist"));
		model.setDctermsmedium(rec.getMultiLiteralOrResourceValue("Medium"));
		model.setLabel(rec.getMultiLiteralValue("Title"));
		model.setDescription(rec.getMultiLiteralValue("CreditLine"));

		model.setIsShownAt(rec.getLiteralOrResourceValue("URL"));

		String uriAt = model.getIsShownAt()==null?null:model.getIsShownAt().getURI();
		ProvenanceInfo provInfo = new ProvenanceInfo("MuseumofModernArt",uriAt,rec.getStringValue("ObjectID"));
		object.addToProvenance(provInfo);
		
		LiteralOrResource ro = rec.getLiteralOrResourceValue("ThumbnailURL");
		String uriThumbnail = ro==null?null:ro.getURI();
		if (Utils.hasInfo(uriThumbnail)){
			EmbeddedMediaObject medThumb = new EmbeddedMediaObject();
			medThumb.setUrl(uriThumbnail);
			medThumb.setType(type);
//			medThumb.setWidth(rec.get));
//			medThumb.setOriginalRights(rights);
//			medThumb.setWithRights(withMediaRights);
			object.addMedia(MediaVersion.Thumbnail, medThumb);
		}

		
		// TODO fill the views
		return object;
	}

}
