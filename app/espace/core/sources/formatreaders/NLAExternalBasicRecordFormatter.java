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

import org.apache.commons.codec.digest.DigestUtils;

import espace.core.JsonContextRecordFormatReader;
import espace.core.sources.NLASpaceSource;
import espace.core.utils.JsonContextRecord;
import espace.core.utils.StringUtils;
import model.ExternalBasicRecord;
import model.Provider;

public class NLAExternalBasicRecordFormatter extends JsonContextRecordFormatReader<ExternalBasicRecord> {

	@Override
	public ExternalBasicRecord buildObjectFrom() {
		return new ExternalBasicRecord();
	}

	@Override
	public ExternalBasicRecord fillObjectFrom(JsonContextRecord rec, ExternalBasicRecord record) {
		// TODO: add type
		// TODO: add null checks
		record.setThumbnailUrl(rec.getStringValue("identifier[type=url,linktype=thumbnail].value"));
		record.setIsShownBy(null);
		record.setTitle(rec.getStringValue("title"));
		record.setDescription(rec.getStringValue("abstract"));
		record.setCreator(rec.getStringValue("contributor"));
		// record.setContributors(rec.getStringArrayValue("contributor"));
		// TODO: add years
		record.setYear(StringUtils.getYears(rec.getStringArrayValue("issued")));
		record.setIsShownAt(rec.getStringValue("identifier[type=url,linktype=fulltext|restricted|unknown].value"));
		// TODO: add rights
		// record.setItemRights(rec.getStringValue("sourceResource.rights"));
		record.setExternalId(record.getIsShownAt());
		if (record.getExternalId() == null || record.getExternalId() == "")
			record.setExternalId(record.getIsShownBy());
		record.setExternalId(DigestUtils.md5Hex(record.getExternalId()));
		String id = rec.getStringValue("id");
		Provider recordProvider = new Provider(NLASpaceSource.LABEL, id, rec.getStringValue("troveUrl"));
		record.addProvider(recordProvider);

		return record;
	}

}
