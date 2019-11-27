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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import model.DescriptiveData;
import model.EmbeddedMediaObject.MediaVersion;
import model.annotations.Annotation;
import model.annotations.ContextData;
import model.annotations.ContextData.ContextDataBody;
import model.basicDataTypes.MultiLiteralOrResource;
import model.resources.WithAdmin;
import model.resources.WithResource;
import model.resources.WithResourceType;
import model.resources.collection.Exhibition.ExhibitionDescriptiveData;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;

import play.libs.Json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import controllers.WithController.Profile;
import db.DB;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@Entity("CollectionObject")

@Indexes({
	@Index(fields = @Field(value = "resourceType", type = IndexType.ASC), options = @IndexOptions()),
	@Index(fields = @Field(value = "administrative.withCreator", type = IndexType.ASC), options = @IndexOptions()),
	@Index(fields = @Field(value = "administrative.externalId", type = IndexType.ASC), options = @IndexOptions()),
	@Index(fields = @Field(value = "administrative.lastModified", type = IndexType.DESC), options = @IndexOptions()),
	@Index(fields = @Field(value = "provenance.provider", type = IndexType.ASC), options = @IndexOptions()),
	@Index(fields = @Field(value = "provenance.resourceId", type = IndexType.ASC), options = @IndexOptions()),
	@Index(fields = @Field(value = "descriptiveData.label", type = IndexType.ASC), options = @IndexOptions()),
	@Index(fields = @Field(value = "collectedIn", type = IndexType.ASC), options = @IndexOptions()) })
public class CollectionObject<T extends CollectionObject.CollectionDescriptiveData>
	extends WithResource<T, CollectionObject.CollectionAdmin> {

	public CollectionObject() {
		super();
		this.administrative = new CollectionAdmin();
		this.resourceType = WithResourceType.valueOf(this.getClass()
				.getSimpleName());
		this.collectedResources = new ArrayList<ContextData<ContextDataBody>>();
	}

	@Embedded
	private List<ContextData<ContextDataBody>> collectedResources;
	private long annotationCount = 0;

	public List<ContextData<ContextDataBody>> getCollectedResources() {
		return collectedResources;
	}

	public void setCollectedResources(
			List<ContextData<ContextDataBody>> collectedResources) {
		this.collectedResources = collectedResources;
	}

	public long getAnnotationCount() {
		return annotationCount;
	}

	public void fillAnnotationCount() {
		this.annotationCount = DB.getRecordResourceDAO()
				.countAnnotations(this.getDbId());
	}

	@Embedded
	public static class CollectionAdmin extends WithAdmin {

		protected int entryCount = 0;
		//protected CollectionType collectionType = CollectionType.SimpleCollection;

		public int getEntryCount() {
			return entryCount;
		}

		public void setEntryCount(int entryCount) {
			this.entryCount = entryCount;
		}

		public void incEntryCount() {
			this.entryCount++;
		}

	}


	public static class CollectionDescriptiveData extends DescriptiveData {
		//TODO: change these to camelCase!
		// start day or possible start days
		private MultiLiteralOrResource dccreator;
		// for whom the resource is intended or useful
		private MultiLiteralOrResource dctermsaudience;
		// additional views of the timespan?
		private MultiLiteralOrResource dclanguage;

		// TODO: add link to external collection
		public MultiLiteralOrResource getDccreator() {
			return dccreator;
		}

		public void setDccreator(MultiLiteralOrResource dccreator) {
			this.dccreator = dccreator;
		}

		public MultiLiteralOrResource getDctermsaudience() {
			return dctermsaudience;
		}

		public void setDctermsaudience(
				MultiLiteralOrResource dctermsaudience) {
			this.dctermsaudience = dctermsaudience;
		}

		public MultiLiteralOrResource getDclanguage() {
			return dclanguage;
		}

		public void setDclanguage(MultiLiteralOrResource dclanguage) {
			this.dclanguage = dclanguage;
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
		/*Map<String, Object> idx_map =  this.transformWR();

		idx_map.put("dccreator", ((CollectionDescriptiveData)this.getDescriptiveData()).getDccreator());
		idx_map.put("dctermsaudience", ((CollectionDescriptiveData)this.getDescriptiveData()).getDctermsaudience());
		idx_map.put("dclanguage", ((CollectionDescriptiveData)this.getDescriptiveData()).getDclanguage());

		ArrayNode cd = (ArrayNode) Json.toJson(this.getCollectedResources());
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);
		List<Object> cd_map = mapper.convertValue(cd, List.class);
		idx_map.put("collectedResources", cd_map);
		return idx_map;*/
		return transformWR();
	}

	public CollectionObject getCollectionProfile(String profileString) {
		Profile profile = Profile.valueOf(profileString);
		if (profile == null)
			profile = Profile.BASIC;
		if (profile.equals(Profile.FULL))
			return this;
		else if (profile.equals(Profile.BASIC) || profile.equals(Profile.MEDIUM)) {
			if (this.getResourceType().equals(WithResourceType.SimpleCollection)) {
				SimpleCollection output = new SimpleCollection();
				addCommonCollectionFields(output);
				return output;
			}
			else if (getResourceType().equals(WithResourceType.Exhibition)) {
				Exhibition output = new Exhibition();
				addCommonCollectionFields(output);
				ExhibitionDescriptiveData edd = (ExhibitionDescriptiveData) getDescriptiveData();
//				if ((edd.getBackgroundImg() != null) && edd.getBackgroundImg().containsKey(MediaVersion.Original))
//					edd.getBackgroundImg().remove(MediaVersion.Original);
				output.getDescriptiveData().setBackgroundImg(edd.getBackgroundImg());
				output.getDescriptiveData().setCredits(edd.getCredits());
				output.getDescriptiveData().setIntro(edd.getIntro());
				return output;
			}
			else return this;
		}
		else return this;
	}

	private void addCommonCollectionFields(CollectionObject output) {
		output.setAdministrative(getAdministrative());
		output.setDbId(getDbId());
		output.getDescriptiveData().setLabel(getDescriptiveData().getLabel());
		output.getDescriptiveData().setDescription(getDescriptiveData().getDescription());
		output.setProvenance(getProvenance());
		output.setMedia(getMedia());
		output.setResourceType(getResourceType());
		output.fillAnnotationCount();
		//output.setUsage(input.getUsage());
	}

}
