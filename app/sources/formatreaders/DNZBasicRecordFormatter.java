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
import model.Provider.Sources;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import sources.FilterValuesMap;
import sources.core.CommonFilters;
import sources.core.Utils;
import sources.utils.JsonContextRecord;

public class DNZBasicRecordFormatter extends CulturalRecordFormatter {

	public DNZBasicRecordFormatter(FilterValuesMap map) {
		super(map);
		object = new CulturalObject();
	}

	@Override
	public CulturalObject fillObjectFrom(JsonContextRecord rec) {
		CulturalObjectData model = (CulturalObjectData) object.getDescriptiveData();
		// TODO read the language
		
		model.setLabel(rec.getMultiLiteralValue("title"));
		model.setDescription(rec.getMultiLiteralValue("description"));
		model.setIsShownBy(rec.getLiteralOrResourceValue("object_url"));
		model.setIsShownAt(rec.getLiteralOrResourceValue("landing_url"));
		model.setDates(rec.getWithDateArrayValue("date"));
		model.setDccreator(rec.getMultiLiteralOrResourceValue("creator"));
		model.setDctype(rec.getMultiLiteralOrResourceValue("dctype"));
//		object.addToProvenance(new ProvenanceInfo(rec.getStringValue("dataProvider"), model.getIsShownAt().getURI(),null));
//		object.addToProvenance(
//				new ProvenanceInfo(rec.getStringValue("provider.name"), null, rec.getStringValue("provider.@id")));
		String id = rec.getStringValue("id");
		object.addToProvenance(
				new ProvenanceInfo(Sources.DigitalNZ.toString(), "http://www.digitalnz.org/objects/" + id, id));
		
		
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
		
		
		
		String uri3 = rec.getStringValue("thumbnail_url");
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
		
		uri3 = rec.getStringValue("large_thumbnail_url");
		if (Utils.hasInfo(uri3)){
			EmbeddedMediaObject medThumb = new EmbeddedMediaObject();
			medThumb.setUrl(uri3);
			medThumb.setType(type);
			medThumb.setParentID(uri2);
			if (Utils.hasInfo(rights))
			medThumb.setOriginalRights(new LiteralOrResource(rights.get(0)));
			medThumb.setWithRights(withRights);
			object.addMediaView(MediaVersion.Thumbnail, medThumb);
		}
		
		
		return object;
	}

}
