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


package model.resources.collection;

import java.util.HashMap;
import java.util.Map;

import org.mongodb.morphia.annotations.Entity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.resources.collection.CollectionObject.CollectionDescriptiveData;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@Entity("CollectionObject")
public class Exhibition extends CollectionObject<Exhibition.ExhibitionDescriptiveData> {

	public Exhibition() {
		super();
		this.resourceType = WithResourceType.Exhibition;
	}

	public static class ExhibitionDescriptiveData extends CollectionDescriptiveData {

		private String intro;
		private HashMap<MediaVersion, EmbeddedMediaObject> backgroundImg;
		private String credits;

		public String getIntro() {
			return intro;
		}

		public void setIntro(String intro) {
			this.intro = intro;
		}

		public HashMap<MediaVersion, EmbeddedMediaObject> getBackgroundImg() {
			return backgroundImg;
		}

		public void setBackgroundImg(HashMap<MediaVersion, EmbeddedMediaObject> backgroundImg) {
			this.backgroundImg = backgroundImg;
		}

		public String getCredits() {
			return credits;
		}

		public void setCredits(String credits) {
			this.credits = credits;
		}
	}


	/*
	 * Elastic transformations
	 */

	/*
	 * Currently we are indexing only Resources that represent
	 * collected records
	 */
	public Map<String, Object> transform() {
		Map<String, Object> idx_map =  super.transform();

		idx_map.put("intro", this.getDescriptiveData().getIntro());
		idx_map.put("credits", this.getDescriptiveData().getCredits());
		return idx_map;
	}
}
