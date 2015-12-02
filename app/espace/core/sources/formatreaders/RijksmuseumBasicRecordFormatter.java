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


package espace.core.sources.formatreaders;

import java.time.Year;

import org.apache.commons.codec.digest.DigestUtils;

import espace.core.ExternalBasicRecordReader;
import espace.core.JsonContextRecordFormatReader;
import espace.core.sources.NLASpaceSource;
import espace.core.sources.RijksmuseumSpaceSource;
import espace.core.utils.JsonContextRecord;
import model.ExternalBasicRecord;
import model.Provider;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import utils.ListUtils;

public class RijksmuseumBasicRecordFormatter extends ExternalBasicRecordReader<CulturalObject> {


	@Override
	public CulturalObject fillObjectFrom(JsonContextRecord rec) {
		CulturalObjectData model = new CulturalObjectData();
		object.setDescriptiveData(model);
//		object.setThumbnailUrl(rec.getStringValue("webImage.url"));
//		object.setTitle(rec.getStringValue("title"));
//		object.setDescription(rec.getStringValue("longTitle"));
//		object.setCreator(rec.getStringValue("principalOrFirstMaker"));
////		record.setContributors(rec.getStringArrayValue("contributor"));
//		// TODO: add years
//		object.setYears(ListUtils.transform(rec.getStringArrayValue("year"), (String y)->{return Year.parse(y);}));
//		// TODO: add rights
////		record.setItemRights(rec.getStringValue("rights"));
//		String id = rec.getStringValue("objectNumber");
//		Provider recordProvider = new Provider(RijksmuseumSpaceSource.LABEL, id, 
//				"https://www.rijksmuseum.nl/en/search/objecten?q=dance&p=1&ps=12&ii=0#/"
//						+ id + ",0");
//		object.addProvider(recordProvider);
		return object;
	}

}
