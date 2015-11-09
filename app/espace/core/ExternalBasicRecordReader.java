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


package espace.core;

import model.ExternalBasicRecord;

import org.apache.commons.codec.digest.DigestUtils;

import com.fasterxml.jackson.databind.JsonNode;

import espace.core.utils.JsonContextRecord;

public abstract class ExternalBasicRecordReader extends JsonContextRecordFormatReader<ExternalBasicRecord> {
	
	//public ExternalBasicRecord record = new ExternalBasicRecord();
	
	public ExternalBasicRecordReader() {
		this.object = new ExternalBasicRecord();
	}	

	@Override
	public ExternalBasicRecord fillObjectFrom(JsonContextRecord rec) {
		fillInExternalId(rec);
		if (object.externalIdNotNull()) {
			setExternalId();
			fillInValidRecord(rec);
			return object;
		}
		else 
			return null;
	}
	
	private void setExternalId() {
		String isShownAt = object.getIsShownAt();
		if (isShownAt != null && !isShownAt.isEmpty())
			object.setExternalId(DigestUtils.md5Hex(isShownAt));
		else {
			object.setExternalId(DigestUtils.md5Hex(object.getIsShownBy()));
		}
			
	}
	
	/*public ExternalBasicRecord fillObjectFrom(String text) {
		JsonContextRecord rec = new JsonContextRecord(text);
		return formRecord(rec);
	}
	
	public ExternalBasicRecord fillObjectFrom(JsonContextRecord record,
			ExternalBasicRecord object) {
		return formRecord(record);
	}*/
	
	
	public abstract void fillInExternalId(JsonContextRecord rec);
	
	public abstract void fillInValidRecord(JsonContextRecord rec);


}
