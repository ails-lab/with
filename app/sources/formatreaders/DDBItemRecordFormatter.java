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
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.ProvenanceInfo.Sources;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import sources.FilterValuesMap;
import sources.core.CommonFilters;
import sources.core.Utils;
import sources.utils.JsonContextRecord;

public class DDBItemRecordFormatter extends CulturalRecordFormatter {

	public DDBItemRecordFormatter() {
		super(FilterValuesMap.getDDBMap());
		object = new CulturalObject();
	}

	@Override
	public CulturalObject fillObjectFrom(JsonContextRecord rec) {
		CulturalObjectData model = (CulturalObjectData) object.getDescriptiveData();
		rec.enterContext("RDF");
		List<Object> vals = getValuesMap().translateToCommon(CommonFilters.TYPE.getId(), rec.getStringValue("ProvidedCHO.type"));
		WithMediaType type = WithMediaType.getType(vals.get(0).toString());
		
		// TODO read the language
		
//		model.setDcidentifier(rec.getMultiLiteralOrResourceValue("dcIdentifier"));
//		model.setDccoverage(rec.getMultiLiteralOrResourceValue("dcCoverage"));
//		model.setDcrights(rec.getMultiLiteralOrResourceValue("dcRights"));
//		model.setDcspatial(rec.getMultiLiteralOrResourceValue("dctermsSpatial"));
		model.setDccreator(rec.getMultiLiteralOrResourceValue("ProvidedCHO.creator"));
//		model.setDccreated(rec.getWithDateArrayValue("dctermsCreated"));
//		model.setDcformat(rec.getMultiLiteralOrResourceValue("dcFormat"));
//		model.setDctermsmedium(rec.getMultiLiteralOrResourceValue("dctermsMedium"));
//		model.setIsRelatedTo(rec.getMultiLiteralOrResourceValue("edmIsRelatedTo"));
		model.setLabel(rec.getMultiLiteralValue("ProvidedCHO.title"));
//		model.setDescription(rec.getMultiLiteralValue("dcDescription"));
		model.setKeywords(rec.getMultiLiteralOrResourceValue("Concept"));
		model.setDates(rec.getWithDateArrayValue("ProvidedCHO.issued"));
		model.setDctype(rec.getMultiLiteralOrResourceValue("WebResource.type"));
//		model.setDccontributor(rec.getMultiLiteralOrResourceValue("dcContributor"));
		
		LiteralOrResource rights = rec.getLiteralOrResourceValue("WebResource.rights");
		WithMediaRights withMediaRights = rights==null?null:(WithMediaRights)
		 getValuesMap().translateToCommon(CommonFilters.RIGHTS.getId(),
		 rights.getURI()).get(0);
		




		model.getDates().addAll(rec.getWithDateArrayValue("dcDate"));


		model.setIsShownAt(rec.getLiteralOrResourceValue("Aggregation.isShownAt"));
		model.setIsShownBy(rec.getLiteralOrResourceValue("Aggregation.isShownBy"));
		String uriAt = rec.getStringValue("Aggregation.dataProvider[1]@resource");
		ProvenanceInfo provInfo = new ProvenanceInfo(rec.getStringValue("Aggregation.dataProvider[0]"),uriAt,null);
		object.addToProvenance(provInfo);
		
		provInfo = new ProvenanceInfo(rec.getStringValue("Aggregation.provider"));
		object.addToProvenance(provInfo);
		
		
		String recID = rec.getStringValue("ProvidedCHO.identifier[1]");
		String uri = rec.getStringValue("ProvidedCHO.@about");
		object.addToProvenance(
				new ProvenanceInfo(Sources.DDB.toString(),  uri,recID));

//		List<String> theViews = rec.getStringArrayValue("hasView");
		LiteralOrResource ro = rec.getLiteralOrResourceValue("edmObject");
		

//		model.getDates().addAll(rec.getWithDateArrayValue("year"));
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
