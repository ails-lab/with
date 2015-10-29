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
import espace.core.sources.EuropeanaSpaceSource;
import espace.core.utils.JsonContextRecord;
import model.ExternalBasicRecord;
import model.Provider;
import utils.ListUtils;

public class EuropeanaExternalBasicRecordFormatter extends JsonContextRecordFormatReader<ExternalBasicRecord> {

	@Override
	public ExternalBasicRecord buildObjectFrom() {
		return new ExternalBasicRecord();
	}

	@Override
	public ExternalBasicRecord fillObjectFrom(JsonContextRecord rec, ExternalBasicRecord record) {
		record.addProvider(new Provider(rec.getStringValue("dataProvider")));
		record.addProvider(new Provider(rec.getStringValue("provider")));
		//TODO: add null checks
		record.setThumbnailUrl(rec.getStringValue("edmPreview"));
		record.setIsShownBy(rec.getStringValue("edmIsShownBy"));
		record.setTitle(rec.getStringValue("title"));
		record.setDescription(rec.getStringValue("dcDescription"));
		record.setCreator(rec.getStringValue("dcCreator"));
		record.setContributors(rec.getStringArrayValue("dcContributor"));
		record.setYear(ListUtils.transform(rec.getStringArrayValue("year"), (String y)->{return Year.parse(y);}));
		record.setIsShownAt(rec.getStringValue("edmIsShownAt"));
//		record.setItemRights(rec.getStringValue("rights"));
		record.setExternalId(record.getIsShownAt());
		if (record.getExternalId() == null || record.getExternalId() == "")
			record.setExternalId(record.getIsShownBy());
		record.setExternalId(DigestUtils.md5Hex(record.getExternalId()));
		Provider recordProvider = new Provider(EuropeanaSpaceSource.LABEL, rec.getStringValue("id"), rec.getStringValue("guid"));
		record.addProvider(recordProvider);
		return record;
	}

}
