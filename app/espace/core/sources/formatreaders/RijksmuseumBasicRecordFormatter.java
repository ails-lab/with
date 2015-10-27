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

import espace.core.JsonContextRecordFormatReader;
import espace.core.sources.NLASpaceSource;
import espace.core.sources.RijksmuseumSpaceSource;
import espace.core.utils.JsonContextRecord;
import model.ExternalBasicRecord;
import model.Provider;
import utils.ListUtils;

public class RijksmuseumBasicRecordFormatter extends JsonContextRecordFormatReader<ExternalBasicRecord> {

	@Override
	public ExternalBasicRecord buildObjectFrom() {
		return new ExternalBasicRecord();
	}

	@Override
	public ExternalBasicRecord fillObjectFrom(JsonContextRecord rec, ExternalBasicRecord record) {
		//TODO: add type
		//TODO: add null checks
		record.setThumbnailUrl(rec.getStringValue("webImage.url"));
		record.setIsShownBy(rec.getStringValue("headerImage.url"));
		record.setTitle(rec.getStringValue("title"));
		record.setDescription(rec.getStringValue("longTitle"));
		record.setCreator(rec.getStringValue("principalOrFirstMaker"));
//		record.setContributors(rec.getStringArrayValue("contributor"));
		// TODO: add years
		record.setYear(ListUtils.transform(rec.getStringArrayValue("year"), (String y)->{return Year.parse(y);}));
		record.setIsShownAt(record.getIsShownBy());
		// TODO: add rights
//		record.setItemRights(rec.getStringValue("rights"));
		record.setExternalId(record.getIsShownAt());
		if (record.getExternalId() == null || record.getExternalId() == "")
			record.setExternalId(record.getIsShownBy());
		record.setExternalId(DigestUtils.md5Hex(record.getExternalId()));
		String id = rec.getStringValue("objectNumber");
		Provider recordProvider = new Provider(RijksmuseumSpaceSource.LABEL, id, 
				"https://www.rijksmuseum.nl/en/search/objecten?q=dance&p=1&ps=12&ii=0#/"
						+ id + ",0");
		record.addProvider(recordProvider);
		return record;
	}

}
