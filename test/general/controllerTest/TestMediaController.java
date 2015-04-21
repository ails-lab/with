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
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;
import static play.test.Helpers.running;
import static play.test.Helpers.status;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import model.Media;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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

public class TestMediaController {

	@Test
	public void testGetMetadata() {
		Media image = new Media();
		URL url;
		byte[] rawbytes = null;
		try {
			url = new URL("http://www.ntua.gr/ntua-01.jpg");
			File file = new File("test_java.txt");
			FileUtils.copyURLToFile(url, file);
			FileInputStream fileStream = new FileInputStream(
					file);
			rawbytes = IOUtils.toByteArray(fileStream);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		image.setData(rawbytes);
		image.setDuration(320.0f);
		image.setType("IMAGE");
		image.setMimeType("image/jpeg");
		image.setHeight(599);
		image.setWidth(755);

		try {
			DB.getMediaDAO().makePermanent(image);
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertThat( image.getDbId()).isNotNull();

		running( fakeApplication(), new Runnable() {
			@Override
			public void run() {
				Result result = route(fakeRequest("GET", "/media"
						+ "/"+image.getDbId()));

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
	public void testGetFile() {
		Media image = new Media();
		URL url;
		byte[] rawbytes = null;
		try {
			url = new URL("http://www.ntua.gr/ntua-01.jpg");
			File file = new File("test_java.txt");
			FileUtils.copyURLToFile(url, file);
			FileInputStream fileStream = new FileInputStream(
					file);
			rawbytes = IOUtils.toByteArray(fileStream);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		image.setData(rawbytes);
		image.setDuration(320.0f);
		image.setType("IMAGE");
		image.setMimeType("image/jpeg");
		image.setHeight(599);
		image.setWidth(755);

		try {
			DB.getMediaDAO().makePermanent(image);
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertThat( image.getDbId()).isNotNull();

		running( fakeApplication(), new Runnable() {
			@Override
			public void run() {
				Result result = route(fakeRequest("GET", "/media"
						+ "/"+image.getDbId()
						+ "?file=true"));


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
	public void testDeleteMedia() {
		Media image = new Media();
		URL url;
		byte[] rawbytes = null;
		try {
			url = new URL("http://www.ntua.gr/ntua-01.jpg");
			File file = new File("test_java.txt");
			FileUtils.copyURLToFile(url, file);
			FileInputStream fileStream = new FileInputStream(
					file);
			rawbytes = IOUtils.toByteArray(fileStream);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		image.setData(rawbytes);
		image.setDuration(320.0f);
		image.setType("IMAGE");
		image.setMimeType("image/jpeg");
		image.setHeight(599);
		image.setWidth(755);

		try {
			DB.getMediaDAO().makePermanent(image);
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertThat( image.getDbId()).isNotNull();

		running( fakeApplication(), new Runnable() {
			@Override
			public void run() {
				Result result = route(fakeRequest("DELETE", "/media"
						+ "/"+image.getDbId()));

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
	public void testEditMedia() {
		Media image = new Media();
		URL url;
		byte[] rawbytes = null;
		try {
			url = new URL("http://www.ntua.gr/ntua-01.jpg");
			File file = new File("test_java.txt");
			FileUtils.copyURLToFile(url, file);
			FileInputStream fileStream = new FileInputStream(
					file);
			rawbytes = IOUtils.toByteArray(fileStream);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		image.setData(rawbytes);
		image.setDuration(322.0f);
		image.setType("IMAGE");
		image.setMimeType("image/jpeg");
		image.setHeight(599);
		image.setWidth(755);

		try {
			DB.getMediaDAO().makePermanent(image);
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertThat( image.getDbId()).isNotNull();

		running( fakeApplication(), new Runnable() {
			@Override
			public void run() {
				ObjectNode json = Json.newObject();
				json.put("type", "Edited IMAGE");
				Result result = route(fakeRequest("POST", "/media"
						+ "/"+image.getDbId())
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
