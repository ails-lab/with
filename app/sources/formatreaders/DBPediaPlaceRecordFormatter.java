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

import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.WithMediaRights;
import model.basicDataTypes.Language;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.MultiLiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.ProvenanceInfo.Sources;
import model.basicDataTypes.WithDate;
import model.resources.PlaceObject;
import model.resources.PlaceObject.PlaceData;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import play.Logger;
import sources.FilterValuesMap;
import sources.core.Utils;
import sources.utils.JsonContextRecord;
import sources.utils.StringUtils;

public class DBPediaPlaceRecordFormatter extends PlaceRecordFormatter {

	public DBPediaPlaceRecordFormatter(FilterValuesMap map) {
		super(map);
		object = new PlaceObject();
	}

	@Override

	public PlaceObject fillObjectFrom(JsonContextRecord rec) {
//		Language[] language = new Language[] {Language.EN };
//		
//		language = getLanguagesFromText(rec.getStringValue("title"), 
//				rec.getStringValue("longTitle"));
//		rec.setLanguages(language);

		PlaceData model = (PlaceData) object.getDescriptiveData();
		
//		model.setDclanguage(StringUtils.getLiteralLanguages(language));
		
		model.setLabel(rec.getMultiLiteralValue("label"));
		model.setDescription(rec.getMultiLiteralValue("abstract"));
//		model.setIsShownBy(rec.getLiteralOrResourceValue("edmIsShownBy"));
//		model.setIsShownAt(rec.getLiteralOrResourceValue("edmIsShownAt"));
		// model.setYear(Integer.parseInt(rec.getStringValue("year")));
//		model.setDccreator(rec.getMultiLiteralOrResourceValue("principalOrFirstMaker"));
		
		List<String> country =  rec.getStringArrayValue("country");
		if (country != null && country.size() > 0) {
			MultiLiteralOrResource ct = new MultiLiteralOrResource();
			ct.addURI(country);
			model.setNation(ct);
		}
		
		String id = rec.getStringValue("uri");
		object.addToProvenance(new ProvenanceInfo(Sources.DBPedia.toString(), id , id));
		
		String uri3 = rec.getStringValue("depiction");
		if (Utils.hasInfo(uri3)){
			EmbeddedMediaObject medThumb = new EmbeddedMediaObject();
			medThumb.setUrl(uri3);
//			medThumb.setWidth(rec.getIntValue("headerImage.width"));
//			medThumb.setHeight(rec.getIntValue("headerImage.height"));
//			medThumb.setType(type);
//			if (Utils.hasInfo(rights))
			medThumb.setOriginalRights(new LiteralOrResource("http://creativecommons.org/publicdomain/zero/1.0/deed.en"));
			medThumb.setWithRights(WithMediaRights.Public);
			
			object.addMedia(MediaVersion.Thumbnail, medThumb);
		}
		
		
		return object;
	}

}
