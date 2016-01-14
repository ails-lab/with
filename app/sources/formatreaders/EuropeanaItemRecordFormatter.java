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
import sources.core.Utils;
import sources.utils.JsonContextRecord;
import sources.utils.JsonNodeUtils;
import sources.utils.StringUtils;
import model.EmbeddedMediaObject;
import model.MediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.Provider.Sources;
import model.basicDataTypes.KeySingleValuePair.LiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;

public class EuropeanaItemRecordFormatter extends CulturalRecordFormatter {

	public EuropeanaItemRecordFormatter() {
		object = new CulturalObject();
	}

	@Override
	public CulturalObject fillObjectFrom(JsonContextRecord rec) {
		CulturalObjectData model = new CulturalObjectData();
		object.setDescriptiveData(model);
		
		rec.enterContext("proxies[0]");
		
		model.setLabel(rec.getLiteralValue("dcTitle"));
		model.setDescription(rec.getLiteralValue("dcDescription"));
//		model.setKeywords(rec.getLiteralValue("dcSubject"));
		List<String> years = rec.getStringArrayValue("dcDate.def");
		model.setDates(StringUtils.getDates(years));
//		model.setDctype(Utils.asList(rec.getLiteralValue("deType")));
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
				new ProvenanceInfo(Sources.Europeana.toString(), rec.getStringValue("about"), model.getIsShownAt()));
		
		
		rec.exitContext();
		model.setMetadataRights(LiteralOrResource.build("http://creativecommons.org/publicdomain/zero/1.0/"));
		model.setRdfType("http://www.europeana.eu/schemas/edm/ProvidedCHO");
		
//		System.out.println(years+"--->"+model.getDates());
		model.setDccreator(Utils.asList(LiteralOrResource.build(rec.getStringValue("dcCreatorLangAware"))));
//		model.setKeywords(rec.getLiteralValue("dcSubjectLanAware"));
		
		
		
		EmbeddedMediaObject medThumb = new EmbeddedMediaObject();
		medThumb.setUrl(rec.getStringValue("edmIsShownBy"));
		object.addMedia(MediaVersion.Thumbnail, medThumb);
		// TODO: add rights!
		EmbeddedMediaObject med = new EmbeddedMediaObject();
		med.setUrl(rec.getStringValue("edmIsShownBy"));
		// TODO: add withMediaRights, originalRights
		List<String> rights = rec.getStringArrayValue("rights");
		// med.setOriginalRights(originalRights);
		//
		// med.setWithRights(withRights);
		object.addMedia(MediaVersion.Original, med);
		return object;
		// TODO: add null checks
		// object.setThumbnailUrl(rec.getStringValue("edmPreview"));
		// object.setContributors(rec.getStringArrayValue("dcContributor"));
		// object.setItemRights(rec.getStringValue("rights"));
	}

}
