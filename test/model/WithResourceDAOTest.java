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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import model.basicDataTypes.Language;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.WithAccess;
import model.resources.CulturalObject;
import model.resources.RecordResource;
import model.resources.WithResource;
import model.resources.WithResource.WithAdmin;
import model.usersAndGroups.User;
import static org.fest.assertions.Assertions.assertThat;

import org.bson.types.ObjectId;
import org.junit.Test;

import play.libs.Json;
import utils.AccessManager.Action;
import db.DB;

public class WithResourceDAOTest {


	@Test
	public void checkWithCreatorInfo() {

		WithResource<DescriptiveData, WithAdmin> wr = DB.getWithResourceDAO().get(new ObjectId("570e4ae4388d812b9644e5f3"));

		User u = wr.getWithCreatorInfo();
		if(u != null)
			System.out.println(Json.toJson(u));
		else
			System.out.println("No creator");

	}

	@Test
	public void storeWithResource() {

		User u = DB.getUserDAO().getByUsername("eirini1");

		for (int i = 0; i < 1; i++) {
			CulturalObject withResource = new CulturalObject();
			withResource.getUsage().setLikes(i);
			withResource.addToProvenance(new ProvenanceInfo("provider0", "http://myUri", "12345"));
			withResource.addToProvenance(new ProvenanceInfo("provider1", "http://myUri", "12345"));
			withResource.addToProvenance(new ProvenanceInfo("ΜintTest", "http://myUri", "12345"));
			if(u == null) {
				System.out.println("No user found");
				return;
			}
			WithAccess access = new WithAccess();
			access.addToAcl(u.getDbId(), WithAccess.Access.READ);
			access.setIsPublic(true);
			withResource.getAdministrative().setAccess(access);
			withResource.getAdministrative().setCreated(new Date());
			withResource.getAdministrative().setLastModified(new Date());
			withResource.getAdministrative().setWithCreator(DB.getUserDAO().getByUsername("eirini2").getDbId());
			RecordResource.RecordDescriptiveData model = new RecordResource.RecordDescriptiveData();
			model.setLabel(new MultiLiteral(Language.EN, "TestWithResourceNewRights" + i));
			model.setDescription(new MultiLiteral(Language.EN, "Some description"));
			withResource.setDescriptiveData(model);
			int j=0;
			if (i==0) j=i; else j=i+1;
			withResource.addPositionToCollectedIn(new ObjectId("5656dd6ce4b0b19378e1cb80"));
			withResource.addPositionToCollectedIn(new ObjectId("5656dd6ce4b0b19378e1cb80"));
			withResource.addPositionToCollectedIn(new ObjectId("5656dd6ce4b0b19378e1cb81"));
			withResource.addPositionToCollectedIn(new ObjectId("5656dd6ce4b0b19378e1cb81"));
			assertThat(DB.getRecordResourceDAO().makePermanent(withResource)).isNotEqualTo(null);

			/*
			 * CollectionObject co = new CollectionObject();
			 * co.getAdministrative().setCreated(new Date());
			 * co.getAdministrative().setLastModified(new Date()); Literal label
			 * = new Literal(Language.EN, "TestWithResourceNew" + i);
			 * CollectionObject.CollectionDescriptiveData cdd = new
			 * CollectionDescriptiveData(); cdd.setLabel(label); Literal desc =
			 * new Literal(Language.EN, "This is a description");
			 * cdd.setDescription(desc); co.setDescriptiveData(cdd);
			 * co.addToProvenance(new ProvenanceInfo("ΜintTest", "http://myUri",
			 * "12345"));
			 * assertThat(DB.getCollectionObjectDAO().makePermanent(co)).
			 * isNotEqualTo(null);
			 */
			// CulturalObject o = (CulturalObject) resources.get(0);
			// System.out.println(o.getProvenance().get(0).getProvider() + " " +
			// o.getProvenance().get(0).getUri());
			// System.out.println(Json.toJson(o));
			// assertThat(o instanceof CulturalObject);
			/*
			 * assertThat(DB.getRecordResourceDAO().makePermanent(withResource))
			 * .isNotEqualTo(null);
			 * if(DB.getRecordResourceDAO().removeFromCollection(withResource.
			 * getDbId(), new ObjectId("5666bea55cf494714b7a71c6"), 3))
			 * System.out.println("Position removed correclty!");
			 * //DB.getRecordResourceDAO().shiftRecordsToLeft(new
			 * ObjectId("5656dd6ce4b0b19378e1cb80"), 2); //List<RecordResource>
			 * resources = DB.getRecordResourceDAO().getByLabel(Language.EN,
			 * "TestWithResourceZ" + i); //CulturalObject o = (CulturalObject)
			 * resources.get(0);
			 * //System.out.println(o.getProvenance().get(0).getProvider() + " "
			 * + o.getProvenance().get(0).getUri());
			 * //System.out.println(Json.toJson(o));
			 *
			 */
		}
		/*List<RecordResource> cos = DB.getRecordResourceDAO().getByCollectionBtwPositions(new ObjectId("5656dd6ce4b0b19378e1cb81"), 0, 2);
		for (RecordResource co: cos) {
			System.out.println(Json.toJson(co));
		}*/
		//DB.getRecordResourceDAO().shiftRecordsToLeft(new ObjectId("5656dd6ce4b0b19378e1cb81"), 1);
		CulturalObject co1 = (CulturalObject) DB.getWithResourceDAO().getByLabel(Language.EN, "TestWithResourceNewRights0").get(0);
		assertThat(DB.getWithResourceDAO().hasAccess(new ArrayList<>(Arrays.asList(u.getDbId())), Action.READ, co1.getDbId())).isEqualTo(true);
	}

}
