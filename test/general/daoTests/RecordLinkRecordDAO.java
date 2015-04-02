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


package general.daoTests;

import static org.fest.assertions.Assertions.assertThat;
import model.Media;
import model.RecordLink;

import org.bson.types.ObjectId;
import org.junit.Test;
import org.mongodb.morphia.Key;

import db.DB;

public class RecordLinkRecordDAO {

	@Test
	public void testCRUD() {

		// create and store
		RecordLink recordLink = new RecordLink();
		recordLink.setDescription("Testing CRUD for RecordLink");
		recordLink.setRights("CC");
		recordLink.setSource(null);
		recordLink.setSourceId("item_42");
		recordLink.setSourceUrl("http://eur");
		recordLink.setThumbnailUrl("http://www.ntua.gr/ntua-01.jpg");
		recordLink.setTitle("Test recordLink!");
		recordLink.setType("The blue-black or white-gold dress");

		MediaDAOTest mediaDAO = new MediaDAOTest();
		Media image = null;
		try {
			image = mediaDAO.testMediaStorage();
		} catch(Exception e) {
			System.out.println("Cannot save image (media object) to database!");
		}
		recordLink.setThumbnail(image);

		Key<RecordLink> recId = DB.getRecordLinkDAO().makePermanent(recordLink);
		assertThat(recId).isNotNull()
			.overridingErrorMessage("Cannot save RecordLink to DB!");

		//find by id
		RecordLink a = DB.getRecordLinkDAO().getByDbId(new ObjectId(recId.getId().toString()));
		assertThat(a)
		.overridingErrorMessage("RecordLink not retreived using dbId.")
		.isNotNull();
	}


	public RecordLink storeRecordLink() {
			RecordLink recordLink = new RecordLink();
			recordLink.setDescription("This is a test RecordLink");
			recordLink.setRights("CC");
			recordLink.setSource("Europeana");
			recordLink.setSourceId("item_42");
			recordLink.setSourceUrl("http://eur");
			recordLink.setThumbnailUrl("http://www.ntua.gr/ntua-01.jpg");
			recordLink.setTitle("Test recordLink!");
			recordLink.setType("The blue-black or white-gold dress");

			MediaDAOTest mediaDAO = new MediaDAOTest();
			Media image = null;
			try {
				image = mediaDAO.testMediaStorage();
			} catch(Exception e) {
				System.out.println("Cannot save image (media object) to database!");
			}
			recordLink.setThumbnail(image);

			Key<RecordLink> recId = DB.getRecordLinkDAO().makePermanent(recordLink);
			assertThat(recId).isNotNull();
			return recordLink;
	}
}
