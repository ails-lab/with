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

import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.basicDataTypes.ProvenanceInfo.Sources;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import sources.FilterValuesMap;
import sources.utils.JsonContextRecord;

public class BritishLibraryRecordFormatter extends CulturalRecordFormatter {

	public BritishLibraryRecordFormatter(FilterValuesMap map) {
		super(map);
		object = new CulturalObject();
	}

	@Override
	public CulturalObject fillObjectFrom(JsonContextRecord rec) {
		CulturalObjectData model = (CulturalObjectData) object.getDescriptiveData();
		model.setLabel(rec.getMultiLiteralValue("title"));
		model.setDescription(rec.getMultiLiteralValue("description._content"));
		// model.setIsShownBy(rec.getStringValue("edmIsShownBy"));
		// model.setIsShownAt(rec.getStringValue("edmIsShownAt"));
		// model.setYear(Integer.parseInt(rec.getStringValue("year")));
		model.setDccreator(rec.getMultiLiteralOrResourceValue(rec.getStringValue("principalOrFirstMaker")));

		object.addToProvenance(new ProvenanceInfo(rec.getStringValue("dataProvider")));
		object.addToProvenance(
				new ProvenanceInfo(rec.getStringValue("provider.name"), null, rec.getStringValue("provider.@id")));
		String id = rec.getStringValue("id");
		object.addToProvenance(new ProvenanceInfo(Sources.BritishLibrary.toString(),
				"https://www.flickr.com/photos/britishlibrary/" + id + "/", id));
		EmbeddedMediaObject medThumb = new EmbeddedMediaObject();
		// TODO: add both thumbnail embMedia and full size embedded media!
		medThumb.setUrl(rec.getStringValue("url_s"));
		object.addMedia(MediaVersion.Thumbnail, medThumb);
		EmbeddedMediaObject med = new EmbeddedMediaObject();
		// TODO: add rights
		med.setHeight(Integer.parseInt(rec.getStringValue("height_s")));
		med.setWidth(Integer.parseInt(rec.getStringValue("width_s")));
		object.addMedia(MediaVersion.Original, med);
		// med.setUrl(rec.getStringValue("edmIsShownBy"));
		return object;

		// object.setContributors(rec.getStringArrayValue("sourceResource.contributor"));
		// object.setYears(StringUtils.getYears(rec.getStringArrayValue("datetaken")));
		// // TODO: add rights
		// // object.setItemRights(rec.getStringValue("rights"));
		// object.setExternalId(object.getIsShownAt());
	}

}
