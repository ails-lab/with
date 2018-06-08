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


package search;

import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.codec.binary.Hex;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import play.libs.Json;
import search.Response.ValueCount;
import search.Response.ValueCounts;
import utils.Serializer;
import utils.Serializer.ValueCountSerializer;

public class FiltersCache {
	
	public static final ObjectId ID = new ObjectId(new byte[] {0,1,2,3,4,5,6,7,8,9,10,11});
	private static final int DAY = 100*60*60*24;
	@Embedded
	private ValueCounts accumulatedValues;
	private long creationTime;
	
	@Id
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId dbId;

	public ObjectId getDbId() {
		return dbId;
	}

	public void setDbId(ObjectId dbId) {
		this.dbId = dbId;
	}

	
	public FiltersCache() {
		setDbId(ID);
	}

	public FiltersCache(ValueCounts accumulatedValues, long creationTime) {
		setDbId(ID);
		parseAccumulatedValues(accumulatedValues);
		this.creationTime = creationTime;
	}

	public boolean isUpToDate(int days) {
		return System.currentTimeMillis()-creationTime < days*DAY;
	}
	

	public ValueCounts getAccumulatedValues() {
		return accumulatedValues;
	}

	public void setAccumulatedValues(ValueCounts accumulatedValues) {
		this.accumulatedValues = accumulatedValues;
	}

	public void parseAccumulatedValues(ValueCounts accumulatedValues) {
		ValueCounts t = new ValueCounts();
		for (Entry<String, List<ValueCount>> e : accumulatedValues.entrySet()) {
			t.put(getMykey(e.getKey()), e.getValue());
		}
		this.accumulatedValues = t;
	}
	
	public ValueCounts exportAccumulatedValues() {
		ValueCounts res = new ValueCounts();
		ValueCounts acc = accumulatedValues;
		for (Entry<String, List<ValueCount>> e : acc.entrySet()) {
			res.put(getDotkey(e.getKey()), e.getValue());
		}
		return res;
	}

	private String getMykey(String key) {
		return Fields.forFieldId(key).name();
	}
	
	private String getDotkey(String key) {
		return Fields.forFieldId(key).fieldId();
	}

	public long getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(long creationTime) {
		this.creationTime = creationTime;
	}
	
	
	
	
}
