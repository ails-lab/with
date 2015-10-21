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

import java.util.Date;

import model.CollectionRecord;
import model.Media;

import org.bson.types.ObjectId;
import org.junit.Test;
import org.mongodb.morphia.Key;

import db.DB;

public class RecordDAO {

	@Test
	public void testCRUD() {

		// create and store
		CollectionRecord recordLink = new CollectionRecord();
		recordLink.setDescription("Testing CRUD for RecordLink");
		recordLink.setItemRights("CC");
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
		//recordLink.setThumbnail(image.getDbId());

		Key<CollectionRecord> recId = DB.getCollectionRecordDAO().makePermanent(recordLink);
		assertThat(recId).isNotNull()
			.overridingErrorMessage("Cannot save RecordLink to DB!");

		//find by id
		CollectionRecord a = DB.getCollectionRecordDAO().getById(new ObjectId(recId.getId().toString()));
		assertThat(a)
		.overridingErrorMessage("RecordLink not retreived using dbId.")
		.isNotNull();
	}


	public CollectionRecord storeRecordLink() {
			CollectionRecord record = new CollectionRecord();
			record.setDescription("This is a test RecordLink");
			record.setItemRights("CC");
			record.setSource("Europeana");
			record.setSourceId("item_42");
			record.setSourceUrl("http://eur");
			record.setThumbnailUrl("http://www.ntua.gr/ntua-01.jpg");
			record.setTitle("Test recordLink!");
			record.setType("The blue-black or white-gold dress");
			record.setCreated(new Date());


			MediaDAOTest mediaDAO = new MediaDAOTest();
			Media image = null;
			try {
				image = mediaDAO.testMediaStorage();
			} catch(Exception e) {
				System.out.println("Cannot save image (media object) to database!");
			}
			//record.setThumbnail(image.getDbId());

			Key<CollectionRecord> recId = DB.getCollectionRecordDAO().makePermanent(record);
			assertThat(recId).isNotNull();
			return record;
	}
}
