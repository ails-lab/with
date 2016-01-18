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

import org.hibernate.validator.internal.constraintvalidators.URLValidator;

import sources.EuropeanaSpaceSource;
import sources.FilterValuesMap;
import sources.core.CommonFilters;
import sources.core.Utils;
import sources.utils.JsonContextRecord;
import sources.utils.JsonNodeUtils;
import sources.utils.StringUtils;
import utils.ListUtils;
import model.EmbeddedMediaObject;
import model.MediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.WithMediaRights;
import model.Provider.Sources;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;

public class EuropeanaRecordFormatter extends CulturalRecordFormatter {

	public EuropeanaRecordFormatter(FilterValuesMap map) {
		super(map);
		object = new CulturalObject();
	}

	@Override
	public CulturalObject fillObjectFrom(JsonContextRecord rec) {
		CulturalObjectData model = (CulturalObjectData) object.getDescriptiveData();
		model.setLabel(rec.getLiteralValue("dcTitleLangAware"));
		model.setDescription(rec.getLiteralValue("dcDescriptionLangAware"));
		model.setIsShownBy(rec.getStringValue("edmIsShownBy"));
		model.setIsShownAt(rec.getStringValue("edmIsShownAt"));
		List<String> years = rec.getStringArrayValue("year");
		model.setDates(StringUtils.getDates(years));
		model.setDccreator(Utils.asList(LiteralOrResource.build(rec.getStringValue("dcCreatorLangAware"))));
		model.setKeywords(rec.getLiteralOrResourceValue("dcSubjectLanAware"));
		object.addToProvenance(new ProvenanceInfo(rec.getStringValue("dataProvider")));
		object.addToProvenance(new ProvenanceInfo(rec.getStringValue("provider")));
		object.addToProvenance(
				new ProvenanceInfo(Sources.Europeana.toString(), rec.getStringValue("id"), rec.getStringValue("guid")));
		EmbeddedMediaObject medThumb = new EmbeddedMediaObject();
		medThumb.setUrl(model.getIsShownBy());
		object.addMedia(MediaVersion.Thumbnail, medThumb);
		// TODO: add rights!
		EmbeddedMediaObject med = new EmbeddedMediaObject();
		med.setUrl(model.getIsShownBy());
		// TODO: add withMediaRights, originalRights
		List<String> rights = rec.getStringArrayValue("rights");
		med.setOriginalRights(ListUtils.transform(rights, (String x) -> LiteralOrResource.build(x)).get(0));
		med.setWithRights(
				(WithMediaRights) getValuesMap().translateToCommon(CommonFilters.RIGHTS.name(), rights.get(0)).get(0));

		object.addMedia(MediaVersion.Original, med);
		return object;
		// TODO: add null checks
		// object.setThumbnailUrl(rec.getStringValue("edmPreview"));
		// object.setContributors(rec.getStringArrayValue("dcContributor"));
		// object.setItemRights(rec.getStringValue("rights"));
	}

}
