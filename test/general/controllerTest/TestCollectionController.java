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


package general.controllerTest;

//all test should use those
import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;
import static play.test.Helpers.running;
import static play.test.Helpers.status;
import general.TestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

import model.Collection;
import model.CollectionRecord;
import model.User;

import org.junit.Assert;
import org.junit.Test;

import play.libs.Json;
import play.mvc.Result;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import db.DB;

public class TestCollectionController {

	@Test
	public void testGetCollection() {

		User user = new User();
		user.setEmail("test@controller.gr");
		user.setUsername("controller");
		DB.getUserDAO().makePermanent(user);


		Collection col = new Collection();
		col.setDescription("Collection from Controller");
		col.setTitle("Test collection from Controller " + TestUtils.randomString());
		col.setCategory("Music");
		col.setCreated(new Date());
		col.setLastModified(new Date());
		col.setPublic(false);
		col.setOwnerId(user);
		DB.getCollectionDAO().makePermanent(col);

		running( fakeApplication(), new Runnable() {
			@Override
			public void run() {
				Result result = route(fakeRequest("GET", "/collection/" + col.getDbId()));
			    assertThat(status(result)).isEqualTo(OK);

			    JsonParser parser = new JsonParser();
			    Gson gson = new GsonBuilder().setPrettyPrinting().create();
			    JsonElement el = parser.parse(contentAsString(result));
			    System.out.println(gson.toJson(el));
			}
		});
	}


	@Test
	public void testDeleteCollection() {

		User user = new User();
		user.setEmail("test" + TestUtils.randomString() + "@controller.gr");
		user.setUsername("controller");
		DB.getUserDAO().makePermanent(user);


		Collection col = new Collection();
		col.setDescription("Collection from Controller");
		col.setTitle("Test collection from Controller " + TestUtils.randomString());
		col.setCategory("Music");
		col.setCreated(new Date());
		col.setLastModified(new Date());
		col.setPublic(false);
		col.setOwnerId(user);
		DB.getCollectionDAO().makePermanent(col);

		running( fakeApplication(), new Runnable() {
			@Override
			public void run() {
				Result result = route(fakeRequest("DELETE", "/collection/" + col.getDbId()));
			    assertThat(status(result)).isEqualTo(OK);

			    JsonParser parser = new JsonParser();
			    Gson gson = new GsonBuilder().setPrettyPrinting().create();
			    JsonElement el = parser.parse(contentAsString(result));
			    System.out.println(gson.toJson(el));
			}
		});
	}

	@Test
	public void testEditCollection() {

		User user = new User();
		user.setEmail("test@controller.gr");
		user.setUsername("controller");
		DB.getUserDAO().makePermanent(user);


		Collection col = new Collection();
		col.setDescription("Collection from Controller");
		col.setTitle("Test collection from Controller " + TestUtils.randomString() + TestUtils.randomString());
		col.setCategory("Music");
		col.setCreated(new Date());
		col.setLastModified(new Date());
		col.setPublic(false);
		col.setOwnerId(user);
		DB.getCollectionDAO().makePermanent(col);

		running( fakeApplication(), new Runnable() {
			@Override
			public void run() {
				final ObjectNode json = Json.newObject();
				json.put("title", "The EDITED test title " + TestUtils.randomString() + TestUtils.randomString());
				json.put("ownerId", user.getDbId().toString());
				Result result = route(fakeRequest("POST", "/collection/" + col.getDbId())
						.withJsonBody(json));

			    JsonParser parser = new JsonParser();
			    Gson gson = new GsonBuilder().setPrettyPrinting().create();
			    JsonElement el = parser.parse(contentAsString(result));
			    System.out.println(gson.toJson(el));

			    if(status(result) == 200)
				    assertThat(status(result)).isEqualTo(OK);
			    else {
			    	System.out.println(status(result));
			    	Assert.fail();
			    }

			}
		});
	}

	@Test
	public void testCreateCollection() {

		User user = new User();
		user.setEmail("test@controller.gr");
		user.setUsername("controller");
		DB.getUserDAO().makePermanent(user);

		running( fakeApplication(), new Runnable() {
			@Override
			public void run() {
				final ObjectNode json = Json.newObject();
				json.put("title", "The newly CREATED test title " + TestUtils.randomString() + TestUtils.randomString());
				json.put("ownerId", user.getDbId().toString());
				Result result = route(fakeRequest("POST", "/collection/create")
						.withJsonBody(json));

			    JsonParser parser = new JsonParser();
			    Gson gson = new GsonBuilder().setPrettyPrinting().create();
			    JsonElement el = parser.parse(contentAsString(result));
			    System.out.println(gson.toJson(el));

			    if(status(result) == 200)
				    assertThat(status(result)).isEqualTo(OK);
			    else {
			    	System.out.println(status(result));
			    	Assert.fail();
			    }

			}
		});
	}

	@Test
	public void testListUserCollections() {

		User user = new User();
		user.setEmail("test@controller.gr");
		user.setUsername("controller");
		DB.getUserDAO().makePermanent(user);

		Collection col = new Collection();
		col.setDescription("Collection from Controller");
		col.setTitle("Test_1 collection from Controller " + TestUtils.randomString());
		col.setCategory("Dance");
		col.setCreated(new Date());
		col.setLastModified(new Date());
		col.setPublic(false);
		col.setOwnerId(user);
		DB.getCollectionDAO().makePermanent(col);


		Collection col1 = new Collection();
		col1.setDescription("Collection from Controller");
		col1.setTitle("Test_2 collection from Controller " + TestUtils.randomString());
		col1.setCategory("Dance");
		col1.setCreated(new Date());
		col1.setLastModified(new Date());
		col1.setPublic(false);
		col1.setOwnerId(user);
		DB.getCollectionDAO().makePermanent(col1);

		running( fakeApplication(), new Runnable() {
			@Override
			public void run() {
				final ObjectNode json = Json.newObject();
				json.put("title", "The newly CREATED test title");
				json.put("ownerId", user.getDbId().toString());
				Result result = route(fakeRequest("GET", "/collection/list?"
						+ "username=Testuser&email=heres42@mongo.gr&"
						+ "ownerId=" + user.getDbId())
						.withJsonBody(json));

			    JsonParser parser = new JsonParser();
			    Gson gson = new GsonBuilder().setPrettyPrinting().create();
			    JsonElement el = parser.parse(contentAsString(result));
			    System.out.println(gson.toJson(el));

			    if(status(result) == 200)
				    assertThat(status(result)).isEqualTo(OK);
			    else {
			    	System.out.println(status(result));
			    	Assert.fail();
			    }

			}
		});
	}


	@Test
	public void testListFirstUserCollectionRecords() {

		CollectionRecord record = new CollectionRecord();
		record.setTitle("Test Record from Controller");
		DB.getCollectionRecordDAO().makePermanent(record);

		User user = new User();
		user.setEmail("test@controller.gr");
		user.setUsername("controller");
		DB.getUserDAO().makePermanent(user);

		Collection col = new Collection();
		col.setDescription("Collection from Controller");
		col.setTitle("Test_1 collection from Controller " + TestUtils.randomString());
		col.setCategory("Dance");
		col.setCreated(new Date());
		col.setLastModified(new Date());
		col.setPublic(false);
		col.setOwnerId(user);
		col.getFirstEntries().add(record);
		DB.getCollectionDAO().makePermanent(col);


		Collection col1 = new Collection();
		col1.setDescription("Collection from Controller");
		col1.setTitle("Test_2 collection from Controller " + TestUtils.randomString());
		col1.setCategory("Dance");
		col1.setCreated(new Date());
		col1.setLastModified(new Date());
		col1.setPublic(false);
		col1.setOwnerId(user);
		col1.getFirstEntries().add(record);
		DB.getCollectionDAO().makePermanent(col1);

		running( fakeApplication(), new Runnable() {
			@Override
			public void run() {
				final ObjectNode json = Json.newObject();
				json.put("ownerId", user.getDbId().toString());
				Result result = route(fakeRequest("POST", "/collection/listByUser")
						.withJsonBody(json));

			    JsonParser parser = new JsonParser();
			    Gson gson = new GsonBuilder().setPrettyPrinting().create();
			    JsonElement el = parser.parse(contentAsString(result));
			    System.out.println(gson.toJson(el));

			    if(status(result) == 200)
				    assertThat(status(result)).isEqualTo(OK);
			    else {
			    	System.out.println(status(result));
			    	Assert.fail();
			    }

			}
		});
	}

	@Test
	public void testListCollectionRecords() {


		User user = new User();
		user.setEmail("test@controller.gr");
		user.setUsername("controller");
		DB.getUserDAO().makePermanent(user);

		Collection col = new Collection();
		col.setDescription("Collection from Controller");
		col.setTitle("Test_1 collection from Controller " + TestUtils.randomString() + "test_purpose "+ Math.random() + TestUtils.randomString());
		col.setCategory("Dance");
		col.setCreated(new Date());
		col.setLastModified(new Date());
		col.setPublic(false);
		col.setOwnerId(user);
		DB.getCollectionDAO().makePermanent(col);

		for(int i=0;i<30;i++) {
			CollectionRecord entry = new CollectionRecord();
			entry.setCollectionId(col);
			try {
				entry.getContent().put("XML-EDM",
						new String(Files.readAllBytes(Paths.get("test/resources/sample-euscreenxml-core.xml"))));
				entry.getContent().put("XML-ITEM/CLIP",
						new String(Files.readAllBytes(Paths.get("test/resources/sample-euscreenxl-item_clip.xml"))));
			} catch (IOException e) {
				e.printStackTrace();
			}
			DB.getCollectionRecordDAO().makePermanent(entry);
		}

		running( fakeApplication(), new Runnable() {
			@Override
			public void run() {
				Result result = route(fakeRequest("GET", "/collection/"	+ col.getDbId()	+ "/list"
						+ "?"
						+"format=all"));


			    JsonParser parser = new JsonParser();
			    Gson gson = new GsonBuilder().setPrettyPrinting().create();
			    JsonElement el = parser.parse(contentAsString(result));
			    System.out.println(gson.toJson(el));

			    if(status(result) == 200)
				    assertThat(status(result)).isEqualTo(OK);
			    else {
			    	System.out.println(status(result));
			    	Assert.fail();
			    }

			}
		});
	}

	@Test
	public void testAddRecordToCollection() {

		User user = new User();
		user.setEmail("test@controller.gr");
		user.setUsername("controller");
		DB.getUserDAO().makePermanent(user);

		Collection col = new Collection();
		col.setDescription("Collection from Controller");
		col.setTitle("Test_1 collection from Controller " + TestUtils.randomString());
		col.setCategory("Dance");
		col.setCreated(new Date());
		col.setLastModified(new Date());
		col.setPublic(false);
		col.setOwnerId(user);
		DB.getCollectionDAO().makePermanent(col);


		Collection col1 = new Collection();
		col1.setDescription("Collection from Controller");
		col1.setTitle("Test_2 collection from Controller " + TestUtils.randomString());
		col1.setCategory("Dance");
		col1.setCreated(new Date());
		col1.setLastModified(new Date());
		col1.setPublic(false);
		col1.setOwnerId(user);
		DB.getCollectionDAO().makePermanent(col1);

		running( fakeApplication(), new Runnable() {
			@Override
			public void run() {
				ObjectNode json = Json.newObject();
				String recordJson = "{}";
				try {
					recordJson = new String(Files.readAllBytes(Paths.get("test/resources/sample_record.json")));
				} catch (IOException e) {
					e.printStackTrace();
				}
				json = (ObjectNode) Json.parse(recordJson);
				Result result = route(fakeRequest("POST", "/collection/"
						+ col.getDbId()
						+ "/addRecord")
						.withJsonBody(json));

			    JsonParser parser = new JsonParser();
			    Gson gson = new GsonBuilder().setPrettyPrinting().create();
			    JsonElement el = parser.parse(contentAsString(result));
			    System.out.println(gson.toJson(el));

			    if(status(result) == 200)
				    assertThat(status(result)).isEqualTo(OK);
			    else {
			    	System.out.println(status(result));
			    	Assert.fail();
			    }

			}
		});
	}
}
