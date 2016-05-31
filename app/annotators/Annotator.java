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


package annotators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import model.basicDataTypes.Language;
import model.resources.RecordResource;

public abstract class Annotator {

	public static String LANGUAGE = "lang";
	
	public abstract String getName();
	
	public abstract String getService();
	
	public abstract List<Annotation> annotate(String text, Map<String, Object> properties) throws Exception;
	
	public void annotate(RecordResource rr) {	}
	
	public static List<Annotator> getAnnotators(Language lang) {
		List<Annotator> res = new ArrayList<>();
		
		Annotator ann;
		ann = DBPediaSpotlightAnnotator.getAnnotator(lang);
		if (ann != null) {
			res.add(ann);
		}
		
		ann = DictionaryAnnotator.getAnnotator(lang, true);
		if (ann != null) {
			res.add(ann);
		}

		
		return res;
	}
}
