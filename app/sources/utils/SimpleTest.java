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


package sources.utils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import model.resources.CulturalObject;
import play.libs.Json;
import sources.formatreaders.CulturalRecordFormatter;
import sources.formatreaders.EuropeanaItemRecordFormatter;

public class SimpleTest {
	public static void main(String[] args) throws IOException {
		String text = FileUtils.readFileToString(new File("record.json"));
		CulturalRecordFormatter rec = new EuropeanaItemRecordFormatter();
		CulturalObject obj = rec.readObjectFrom(new JsonContextRecord(text));
		System.out.println(Json.toJson(obj));
	}
}
