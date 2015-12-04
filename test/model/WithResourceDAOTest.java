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


package model;
import java.util.Date;
import java.util.List;

import model.DescriptiveData;
import model.basicDataTypes.Literal;
import model.basicDataTypes.Literal.Language;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.WithAccess;
import model.resources.CulturalObject;
import model.resources.RecordResource;
import model.resources.WithResource;
import static org.fest.assertions.Assertions.assertThat;

import org.bson.types.ObjectId;
import org.junit.Test;

import play.libs.Json;

import com.fasterxml.jackson.annotation.JsonCreator;

import db.DB;

public class WithResourceDAOTest {
	
	@Test
	public void storeWithResource() {

		/*for (int i = 0; i < 5; i++) {
			RecordResource withResource = new RecordResource();
			withResource.setUsage(new WithResource.Usage());
			withResource.getUsage().setLikes(i);
			withResource.addToProvenance(new ProvenanceInfo("provider0"));
			WithAccess access = new WithAccess();
			//access.put(new ObjectId(), WithAccess.Access.READ);
			access.setPublic(true);
			withResource.getAdministrative().setAccess(access);
			withResource.getAdministrative().setCreated(new Date());
			withResource.getAdministrative().setLastModified(new Date());
			DescriptiveData model = new RecordResource.RecordDescriptiveData(new Literal(Language.EN, "TestWithResource" + i));
			model.setDescription(new Literal(Language.EN, "Some description"));
			withResource.setModel(model);
			assertThat(DB.getRecordResourceDAO().makePermanent(withResource)).isNotEqualTo(null);
		}*/
		
		for (int i = 0; i < 1; i++) {
			CulturalObject withResource = new CulturalObject();
			withResource.getUsage().setLikes(i);
			withResource.addToProvenance(new ProvenanceInfo("provider0", "http://myUri", "12345"));
			WithAccess access = new WithAccess();
			access.put(new ObjectId(), WithAccess.Access.READ);
			access.setPublic(true);
			withResource.getAdministrative().setAccess(access);
			withResource.getAdministrative().setCreated(new Date());
			withResource.getAdministrative().setLastModified(new Date());
			RecordResource.RecordDescriptiveData model = new RecordResource.RecordDescriptiveData();
			model.setLabel(new Literal(Language.EN, "TestWithResourceZ" + i));
			model.setDescription(new Literal(Language.EN, "Some description"));
			withResource.setDescriptiveData(model);
			withResource.addPositionToCollectedIn(new ObjectId("5656dd6ce4b0b19378e1cb80"), 0);
			withResource.addPositionToCollectedIn(new ObjectId("5656dd6ce4b0b19378e1cb80"), 1);
			assertThat(DB.getRecordResourceDAO().makePermanent(withResource)).isNotEqualTo(null);
			List<RecordResource> resources = DB.getRecordResourceDAO().getByLabel(Language.EN, "TestWithResourceZ" + i);
			CulturalObject o = (CulturalObject) resources.get(0);
			//System.out.println(o.getProvenance().get(0).getProvider() + " " + o.getProvenance().get(0).getUri());
			//System.out.println(Json.toJson(o));
			assertThat(resources.get(1) instanceof CulturalObject);
		}
		List<RecordResource> resources = DB.getRecordResourceDAO().getCollectedResources(new ObjectId("5656dd6ce4b0b19378e1cb80"));
		for (RecordResource c: resources) {
			((CulturalObject) c).getCollectedIn().get(new ObjectId("5656dd6ce4b0b19378e1cb80"));
		}
	}

}
