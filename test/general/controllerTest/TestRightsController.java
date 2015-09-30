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

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.BAD_REQUEST;
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

public class TestRightsController {



	@Test
	public void testSetRights() {
		/*User receiver = new User();
		receiver.setEmail("receive@controller.gr");
		receiver.setUsername("user1");
		DB.getUserDAO().makePermanent(receiver);

		User user = new User();
		user.setEmail("test@controller.gr");
		user.setUsername("user2");
		DB.getUserDAO().makePermanent(user);

		Collection col = new Collection();
		col.setDescription("Collection from Controller");
		col.setTitle("lalala");
		col.setCreated(new Date());
		col.setLastModified(new Date());
		col.setIsPublic(false);
		col.setOwnerId(user);
		DB.getCollectionDAO().makePermanent(col);

		CollectionRecord record = new CollectionRecord();
		record.setCollectionId(col.getDbId());
		record.setTitle("This is stored to test the controller");
		record.setDescription("Desc to test controller");
		record.setCreated(new Date());
		record.setSource("Youtube");
		record.setSourceId("123456");
		record.setSourceUrl("http://www.youtube.com/");
		record.setType("Music");
		record.setItemRights("all");

		try {
			record.getContent().put("XML-EDM",
					new String(Files.readAllBytes(Paths.get("test/resources/sample-euscreenxml-core.xml"))));
			record.getContent().put("XML-ITEM/CLIP",
					new String(Files.readAllBytes(Paths.get("test/resources/sample-euscreenxl-item_clip.xml"))));
		} catch (IOException e) {
			e.printStackTrace();
		}
		DB.getCollectionRecordDAO().makePermanent(record);*/


		//test set rights
		running( fakeApplication(), new Runnable() {
			@Override
			public void run() {
				Result result = route(fakeRequest("GET", "/rights"
						+ "/"+"55f7ea36e4b0516e94e105e0"
						+ "/WRITE"
						+ "?username=user2")
						.withSession("effectiveUserIds", "55f7ea36e4b0516e94e105df"));

			    if(status(result) == 200)
				    assertThat(status(result)).isEqualTo(OK);
			    else {
			    	System.out.println(status(result));
			    	Assert.fail();
			    }

			}
		});

		/*running( fakeApplication(), new Runnable() {
			@Override
			public void run() {
				final ObjectNode json = Json.newObject();
				json.put("title", "The EDITED test title " + TestUtils.randomString() + TestUtils.randomString());
				json.put("ownerId", user.getDbId().toString());
				Result result = route(fakeRequest("POST", "/collection/" + col.getDbId())
						.withJsonBody(json)
						.withSession("user", receiver.getDbId().toString()));

			    JsonParser parser = new JsonParser();
			    Gson gson = new GsonBuilder().setPrettyPrinting().create();
			    JsonElement el = parser.parse(contentAsString(result));
			    System.out.println(gson.toJson(el));

			    if( status(result) == 200 ) {
			    	System.out.println(status(result));
				    assertThat(status(result)).isEqualTo(OK);
			    }
			    else if( status(result) == 400 ) {
			    	System.out.println(status(result));
				    assertThat(status(result)).isEqualTo(BAD_REQUEST);
			    }
			    else {
			    	System.out.println(status(result));
			    	Assert.fail();
			    }

			}
		});*/

		//test list rights
		/*running( fakeApplication(), new Runnable() {
			@Override
			public void run() {
				Result result = route(fakeRequest("GET", "/rights"
						+ "/list"
						+ "?ownerId="+user.getDbId()));

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
		});*/


	}
}
