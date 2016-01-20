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
import model.EmbeddedMediaObject.WithMediaType;
import model.Provider.Sources;
import model.basicDataTypes.MultiLiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import sources.FilterValuesMap;
import sources.core.CommonFilters;
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
		EmbeddedMediaObject med = new EmbeddedMediaObject();
		
		List<Object> vals = getValuesMap().translateToCommon(CommonFilters.TYPE.name(), rec.getStringValue("type"));
		if (vals!=null)
		med.setType((WithMediaType) vals.get(0));


		rec.enterContext("proxies[0]");

		model.setLabel(rec.getLiteralValue("dcTitle"));
		model.setDescription(rec.getLiteralValue("dcDescription"));
		model.setKeywords(rec.getLiteralOrResourceValue("dcSubject"));
		List<String> years = rec.getStringArrayValue("dcDate.def");
		model.setDates(StringUtils.getDates(years));
		// model.setDctype(Utils.asList(rec.getLiteralValue("deType")));

		MultiLiteralOrResource rights = rec.getLiteralOrResourceValue("dcRights");
		// med.setOriginalRights(ListUtils.transform(rights, (String x) ->
		// LiteralOrResource.build(x)).get(0));
		// med.setWithRights(
		// (WithMediaRights)
		// getValuesMap().translateToCommon(CommonFilters.RIGHTS.name(),
		// rights.get(0)).get(0));

		object.addMedia(MediaVersion.Original, med);

		rec.exitContext();

		rec.enterContext("proxies[1]");

		years = rec.getStringArrayValue("dcDate.def");
		model.getDates().addAll(StringUtils.getDates(years));

		rec.exitContext();
		rec.enterContext("aggregations[0]");

		model.setIsShownAt(rec.getStringValue("edmIsShownAt"));
		model.setIsShownBy(rec.getStringValue("edmIsShownBy"));
		ProvenanceInfo provInfo = new ProvenanceInfo(rec.getStringValue("edmDataProvider"));
		object.addToProvenance(provInfo);
		object.addToProvenance(
				new ProvenanceInfo(Sources.Europeana.toString(),  model.getIsShownAt(),rec.getStringValue("about")));

		model.setDccreator(rec.getLiteralOrResourceValue("dcCreator"));

		rec.exitContext();

		// System.out.println(years+"--->"+model.getDates());
		// model.setKeywords(rec.getLiteralValue("dcSubjectLanAware"));

		EmbeddedMediaObject medThumb = new EmbeddedMediaObject();
		medThumb.setUrl(model.getIsShownBy());
		object.addMedia(MediaVersion.Thumbnail, medThumb);
		// TODO: add rights!
		med.setUrl(model.getIsShownBy());

		object.addMedia(MediaVersion.Original, med);
		return object;
		// TODO: add null checks
		// object.setThumbnailUrl(rec.getStringValue("edmPreview"));
		// object.setContributors(rec.getStringArrayValue("dcContributor"));
		// object.setItemRights(rec.getStringValue("rights"));
	}

}
