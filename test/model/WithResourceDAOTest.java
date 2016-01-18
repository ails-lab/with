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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.Quality;
import model.EmbeddedMediaObject.WithMediaRights;
import model.EmbeddedMediaObject.WithMediaType;
import model.ExampleDataModels.LiteralOrResource.ResourceType;
import model.basicDataTypes.Language;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.WithAccess;
import model.resources.CollectionObject;
import model.resources.CulturalObject;
import model.resources.RecordResource;
import model.resources.CollectionObject.CollectionDescriptiveData;
import model.usersAndGroups.User;
import static org.fest.assertions.Assertions.assertThat;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.junit.Test;
import play.libs.Json;

import com.google.common.net.MediaType;

import com.google.common.net.MediaType;

import play.libs.Json;
import db.DB;

public class WithResourceDAOTest {

	@Test
	public void storeWithResource() {

		for (int i = 0; i < 1; i++) {
			CulturalObject withResource = new CulturalObject();
			withResource.getUsage().setLikes(i);
			withResource.addToProvenance(new ProvenanceInfo("provider0", "http://myUri", "12345"));
			withResource.addToProvenance(new ProvenanceInfo("provider1", "http://myUri", "12345"));
			withResource.addToProvenance(new ProvenanceInfo("ΜintTest", "http://myUri", "12345"));
			User u = DB.getUserDAO().getByUsername("eirini1");
			if(u == null) {
				System.out.println("No user found");
				return;
			}
			System.out.println(u.getUsername());
			WithAccess access = new WithAccess();
			access.addAccess(u.getDbId(), WithAccess.Access.READ);
			access.setIsPublic(true);
			withResource.getAdministrative().setAccess(access);
			withResource.getAdministrative().setCreated(new Date());
			withResource.getAdministrative().setLastModified(new Date());
			RecordResource.RecordDescriptiveData model = new RecordResource.RecordDescriptiveData();
			model.setLabel(new MultiLiteral(Language.EN, "TestWithResourceNewRights" + i));
			model.setDescription(new MultiLiteral(Language.EN, "Some description"));
			withResource.setDescriptiveData(model);
			int j=0;
			if (i==0) j=i; else j=i+1;
			withResource.addPositionToCollectedIn(new ObjectId("5656dd6ce4b0b19378e1cb80"), 0+j);
			withResource.addPositionToCollectedIn(new ObjectId("5656dd6ce4b0b19378e1cb80"), 1+j);
			withResource.addPositionToCollectedIn(new ObjectId("5656dd6ce4b0b19378e1cb81"), 0+j);
			withResource.addPositionToCollectedIn(new ObjectId("5656dd6ce4b0b19378e1cb81"), 1+j);
			System.out.println(Json.toJson(withResource));
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
		System.out.println(DB.getWithResourceDAO().getByLabel(Language.EN, "TestWithResourceNewRights0").size());
	}

}
