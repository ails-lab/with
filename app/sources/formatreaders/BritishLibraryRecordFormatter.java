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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.WithMediaRights;
import model.EmbeddedMediaObject.WithMediaType;
import model.basicDataTypes.ProvenanceInfo.Sources;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.MultiLiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import sources.BritishLibrarySpaceSource;
import sources.FilterValuesMap;
import sources.core.CommonFilters;
import sources.core.Utils;
import sources.utils.JsonContextRecord;
import sources.utils.StringUtils;

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
		matchInfo(rec.getStringValue("description._content"),rec);
		
		model.setCoordinates(StringUtils.getPoint(rec.getStringValue("latitude"), rec.getStringValue("longitude")));
		
		// model.setIsShownBy(rec.getStringValue("edmIsShownBy"));
		// model.setIsShownAt(rec.getStringValue("edmIsShownAt"));
		// model.setYear(Integer.parseInt(rec.getStringValue("year")));
		
//		model.setDccreator(rec.getMultiLiteralOrResourceValue("principalOrFirstMaker"));

//		object.addToProvenance(new ProvenanceInfo(rec.getStringValue("dataProvider")));
//		object.addToProvenance(
//				new ProvenanceInfo(rec.getStringValue("provider.name"), null, rec.getStringValue("provider.@id")));
		String id = rec.getStringValue("id");
		object.addToProvenance(new ProvenanceInfo(Sources.BritishLibrary.toString(),
				"https://www.flickr.com/photos/britishlibrary/" + id + "/", id));
				
		String rights = BritishLibrarySpaceSource.getLicence(rec.getStringValue("license"));
		WithMediaType type = (WithMediaType.getType(getValuesMap().translateToCommon(CommonFilters.TYPE.getId(), rec.getStringValue("media")).get(0).toString())) ;
		Object r = getValuesMap().translateToCommon(CommonFilters.RIGHTS.getId(),rights).get(0);
		WithMediaRights withRights = (Utils.hasInfo(rights))?null:(WithMediaRights) r;
		String uri3 = rec.getStringValue("url_s");
		String uri2 = rec.getStringValue("url_o");
		if (Utils.hasInfo(uri3)){
			EmbeddedMediaObject medThumb = new EmbeddedMediaObject();
			medThumb.setUrl(uri3);
			medThumb.setHeight(rec.getIntValue("height_s"));
			medThumb.setWidth(rec.getIntValue("width_s"));
			medThumb.setType(type);
			if (Utils.hasInfo(rights))
			medThumb.setOriginalRights(new LiteralOrResource(rights));
			medThumb.setWithRights(withRights);
			object.addMedia(MediaVersion.Thumbnail, medThumb);
		}
		if (Utils.hasInfo(uri2)){
			EmbeddedMediaObject med = new EmbeddedMediaObject();
			med.setUrl(uri2);
			med.setHeight(rec.getIntValue("height_o"));
			med.setWidth(rec.getIntValue("width_o"));
			if (Utils.hasInfo(rights))
			med.setOriginalRights(new LiteralOrResource(rights));
			med.setWithRights(withRights);
			med.setType(type);
			object.addMedia(MediaVersion.Original, med);
		}
		
		
		// med.setUrl(rec.getStringValue("edmIsShownBy"));
		return object;

		// object.setContributors(rec.getStringArrayValue("sourceResource.contributor"));
		// object.setYears(StringUtils.getYears(rec.getStringArrayValue("datetaken")));
		// // TODO: add rights
		// // object.setItemRights(rec.getStringValue("rights"));
		// object.setExternalId(object.getIsShownAt());
	}

	private void matchInfo(String stringValue, JsonContextRecord rec) {
		CulturalObjectData model = (CulturalObjectData) object.getDescriptiveData();
		String[] s = stringValue.split("<strong>");
		for (String string : s) {
			Pattern p = Pattern.compile("([^<]*)</strong>:\\s*(.+)");
			Matcher m = p.matcher(string);
			while (m.find()) {
				String field = m.group(1);
				String value = m.group(2);
				MultiLiteralOrResource l = new MultiLiteralOrResource();
				l.addSmartLiteral(value, rec.getLanguages());
				switch (field) {
				case "Author":
					model.setDccreator(l);
					break;
				case "Publisher":
					break;
				case "Place of Publishing":
					break;
				case "Date of Publishing":
					break;
				case "Contributor":
					model.setDccontributor(l);
					break;

				default:
					break;
				}
			}
		}
	}

	public static void main(String[] args) {
//		matchInfo("<strong>hola</strong>: me \n <strong>hola1</strong>: me1");
	}
}
