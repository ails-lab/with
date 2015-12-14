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
import model.basicDataTypes.Literal;
import model.basicDataTypes.Literal.Language;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.WithAccess;
import model.resources.CulturalObject;
import model.resources.RecordResource;
import static org.fest.assertions.Assertions.assertThat;

import org.bson.types.ObjectId;
import org.junit.Test;

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

		for (int i = 0; i < 2; i++) {
			CulturalObject withResource = new CulturalObject();
			withResource.getUsage().setLikes(i);
			withResource.addToProvenance(new ProvenanceInfo("provider0", "http://myUri", "12345"));
			withResource.addToProvenance(new ProvenanceInfo("provider1", "http://myUri", "12345"));
			withResource.addToProvenance(new ProvenanceInfo("Μint", "http://myUri", "12345"));
/*
		for (int i = 0; i < 1; i++) {
			CulturalObject withResource = new CulturalObject();
			withResource.getUsage().setLikes(i);
			withResource.addToProvenance(new ProvenanceInfo("Mint", "http://myUri", "12345"));
			withResource.addToProvenance(new ProvenanceInfo("WITH", "http://myUri", "12345"));
			withResource.addToProvenance(new ProvenanceInfo("dpla", "http://myUri", "12345"));
*/
			WithAccess access = new WithAccess();
			access.put(new ObjectId(), WithAccess.Access.READ);
			access.setPublic(true);
			withResource.getAdministrative().setAccess(access);
			withResource.getAdministrative().setCreated(new Date());
			withResource.getAdministrative().setLastModified(new Date());
			RecordResource.RecordDescriptiveData model = new RecordResource.RecordDescriptiveData();
			model.setLabel(new Literal(Language.EN, "TestWithResourceN" + i));
			model.setDescription(new Literal(Language.EN, "Some description"));
			withResource.setDescriptiveData(model);

			int j=0;
			if (i==0) j=i; else j=i+1;
			withResource.addPositionToCollectedIn(new ObjectId("5656dd6ce4b0b19378e1cb80"), 0+j);
			withResource.addPositionToCollectedIn(new ObjectId("5656dd6ce4b0b19378e1cb80"), 1+j);
			withResource.addPositionToCollectedIn(new ObjectId("5656dd6ce4b0b19378e1cb81"), 0+j);
			withResource.addPositionToCollectedIn(new ObjectId("5656dd6ce4b0b19378e1cb81"), 1+j);
			assertThat(DB.getRecordResourceDAO().makePermanent(withResource)).isNotEqualTo(null);
			//CulturalObject o = (CulturalObject) resources.get(0);
			//System.out.println(o.getProvenance().get(0).getProvider() + " " + o.getProvenance().get(0).getUri());
			//System.out.println(Json.toJson(o));
			//assertThat(o instanceof CulturalObject);
/*
			withResource.addPositionToCollectedIn(new ObjectId("5666bea55cf494714b7a71c6"), 0);
			withResource.addPositionToCollectedIn(new ObjectId("5666bea55cf494714b7a71c6"), 1);
			withResource.addPositionToCollectedIn(new ObjectId("5666bea55cf494714b7a71c6"), 2);
			withResource.addPositionToCollectedIn(new ObjectId("5666bea55cf494714b7a71c6"), 3);
			assertThat(DB.getRecordResourceDAO().makePermanent(withResource)).isNotEqualTo(null);
			if(DB.getRecordResourceDAO().removeFromCollection(withResource.getDbId(), new ObjectId("5666bea55cf494714b7a71c6"), 3))
				System.out.println("Position removed correclty!");
			//DB.getRecordResourceDAO().shiftRecordsToLeft(new ObjectId("5656dd6ce4b0b19378e1cb80"), 2);
			//List<RecordResource> resources = DB.getRecordResourceDAO().getByLabel(Language.EN, "TestWithResourceZ" + i);
			//CulturalObject o = (CulturalObject) resources.get(0);
			//System.out.println(o.getProvenance().get(0).getProvider() + " " + o.getProvenance().get(0).getUri());
			//System.out.println(Json.toJson(o));
			//assertThat(o instanceof CulturalObject);

*/
		}
		/*List<RecordResource> cos = DB.getRecordResourceDAO().getByCollectionBtwPositions(new ObjectId("5656dd6ce4b0b19378e1cb81"), 0, 2);
		for (RecordResource co: cos) {
			System.out.println(Json.toJson(co));
		}*/
		DB.getRecordResourceDAO().shiftRecordsToLeft(new ObjectId("5656dd6ce4b0b19378e1cb81"), 1);
	}

}
