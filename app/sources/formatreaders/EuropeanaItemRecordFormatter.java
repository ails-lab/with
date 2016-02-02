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
import model.basicDataTypes.MultiLiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import sources.FilterValuesMap;
import sources.core.CommonFilters;
import sources.core.Utils;
import sources.utils.JsonContextRecord;
import sources.utils.StringUtils;

public class EuropeanaItemRecordFormatter extends CulturalRecordFormatter {

	public EuropeanaItemRecordFormatter(FilterValuesMap map) {
		super(map);
		object = new CulturalObject();
	}

	@Override
	public CulturalObject fillObjectFrom(JsonContextRecord rec) {
		CulturalObjectData model = (CulturalObjectData) object.getDescriptiveData();
		
		List<Object> vals = getValuesMap().translateToCommon(CommonFilters.TYPE.getId(), rec.getStringValue("type"));
		WithMediaType type = (WithMediaType) vals.get(0);
		
		rec.enterContext("proxies[0]");

		model.setDcidentifier(rec.getMultiLiteralOrResourceValue("dcIdentifier"));
		model.setDccoverage(rec.getMultiLiteralOrResourceValue("dcCoverage"));
		model.setDcrights(rec.getMultiLiteralOrResourceValue("dcRights"));
		model.setDcspatial(rec.getMultiLiteralOrResourceValue("dctermsSpatial"));
		model.setDccreator(rec.getMultiLiteralOrResourceValue("dcCreator"));
		model.setDccreated(rec.getWithDateArrayValue("dctermsCreated"));
		model.setDcformat(rec.getMultiLiteralOrResourceValue("dcFormat"));
		model.setDctermsmedium(rec.getMultiLiteralOrResourceValue("dctermsMedium"));
		model.setIsRelatedTo(rec.getMultiLiteralOrResourceValue("edmIsRelatedTo"));
		model.setLabel(rec.getMultiLiteralValue("dcTitle"));
		model.setDescription(rec.getMultiLiteralValue("dcDescription"));
		model.setKeywords(rec.getMultiLiteralOrResourceValue("dcSubject"));
		model.setDates(rec.getWithDateArrayValue("dcDate"));
		model.setDctype(rec.getMultiLiteralOrResourceValue("dcType"));
		model.setDccontributor(rec.getMultiLiteralOrResourceValue("dcContributor"));
		
		LiteralOrResource rights = rec.getLiteralOrResourceValue("dcRights");
		WithMediaRights withMediaRights = rights==null?null:(WithMediaRights)
		 getValuesMap().translateToCommon(CommonFilters.RIGHTS.getId(),
		 rights.getURI()).get(0);
		


		rec.exitContext();

		rec.enterContext("proxies[1]");

		model.getDates().addAll(rec.getWithDateArrayValue("dcDate"));

		rec.exitContext();
		rec.enterContext("aggregations[0]");

		model.setIsShownAt(rec.getLiteralOrResourceValue("edmIsShownAt"));
		model.setIsShownBy(rec.getLiteralOrResourceValue("edmIsShownBy"));
		String uriAt = model.getIsShownAt()==null?null:model.getIsShownAt().getURI();
		ProvenanceInfo provInfo = new ProvenanceInfo(rec.getStringValue("edmDataProvider"),uriAt,null);
		object.addToProvenance(provInfo);
		
		provInfo = new ProvenanceInfo(rec.getStringValue("edmProvider"));
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
