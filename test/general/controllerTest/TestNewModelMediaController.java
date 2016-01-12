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
import static play.test.Helpers.testServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import model.EmbeddedMediaObject.Quality;
import model.EmbeddedMediaObject.WithMediaRights;
import model.EmbeddedMediaObject.WithMediaType;
import model.Media;
import model.MediaObject;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.LiteralOrResource.ResourceType;
import model.usersAndGroups.User;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;

import play.libs.Json;
import play.mvc.Result;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.net.MediaType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.JsonPath;

import db.DB;





public class TestNewModelMediaController {

	
	private MediaObject createImage() {
		MediaObject image = new MediaObject();
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
		
		//embedded
		image.setType(WithMediaType.IMAGE);
		Set<WithMediaRights> set = new HashSet<WithMediaRights>();
		set.add(WithMediaRights.Public);
		image.setWithRights(set);
		image.setHeight(599);
		image.setWidth(755);
		//image.setThumbHeight(120);
		//image.setThumbWidth(100);
		image.setUrl("http://www.ntua.gr/ntua-01.jpg");
		//image.setThumbnailUrl("http://www.ntua.gr/ntua-01.jpg");
		LiteralOrResource lor = new LiteralOrResource();
		lor.setResource(ResourceType.dbpedia, "<http://pt.dbpedia.org/resource/Brasil>");
		image.setOriginalRights(lor);
		image.setMimeType(MediaType.parse("image/jpeg"));
		image.setSize(rawbytes.length);
		image.setQuality(Quality.IMAGE_SMALL);
		
		//extended
		image.setMediaBytes(rawbytes);
		//image.setThumbnailBytes(rawbytes);
		image.setOrientation(); //auto
		//set the rest!
		

		try {
			DB.getMediaObjectDAO().makePermanent(image);
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertThat( image.getDbId()).isNotNull();
		return image;
	}

	
	
//	@Test
	public void testGetMetadata() {
		MediaObject image = createImage();

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


//	@Test
	public void testGetFile() {
		MediaObject image = createImage();

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

//	@Test
	public void testDeleteMedia() {
		MediaObject image = createImage();

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

//	@Test
	public void testEditMedia() {
		MediaObject image = createImage();

		running( fakeApplication(), new Runnable() {
			@Override
			public void run() {
				ObjectNode json = Json.newObject();
				
				//TODO: create json file metadata

				//TODO: test all possible results (extract method to use in create)
				
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

	@Test
// Testing file upload doesn't seem to be supported	
	public void testJSONCreateMedia() {
		// make a user with password
		User u = new User();
		u.setEmail("my@you.me");
		u.setUsername("cool_url");
		// set password after email, email salts the password!
		u.setPassword("secret");
		DB.getUserDAO().makePermanent(u);

		running(testServer(3333), ()->{
			try {
				HttpClient hc = new DefaultHttpClient();
				HttpPost loginToWith = new HttpPost( "http://localhost:3333/user/login" );
				String json = "{\"email\":\"my@you.me\",\"password\":\"secret\"}";
				StringEntity se = new StringEntity( json, "UTF8" );
				loginToWith.setEntity( se );
				loginToWith.addHeader( "Content-type", "text/json");

				HttpResponse response = hc.execute(loginToWith);
				if( response.getStatusLine().getStatusCode() != 200 ) {
					Assert.fail( "Login failed");
				}
				loginToWith.releaseConnection();
								
				//TODO: create json file metadata

				//TODO: test all possible results

				
				
				
			} catch( Exception e ) {
				Assert.fail( e.toString() );
			}
	    });
	}	
	
	
	@Test
	// Testing file upload doesn't seem to be supported	
		public void testCreateMedia() {
			// make a user with password
			User u = new User();
			u.setEmail("my@you.me");
			u.setUsername("cool_url");
			// set password after email, email salts the password!
			u.setPassword("secret");
			DB.getUserDAO().makePermanent(u);

			running(testServer(3333), ()->{
				try {
					HttpClient hc = new DefaultHttpClient();
					HttpPost loginToWith = new HttpPost( "http://localhost:3333/user/login" );
					String json = "{\"email\":\"my@you.me\",\"password\":\"secret\"}";
					StringEntity se = new StringEntity( json, "UTF8" );
					loginToWith.setEntity( se );
					loginToWith.addHeader( "Content-type", "text/json");

					HttpResponse response = hc.execute(loginToWith);
					if( response.getStatusLine().getStatusCode() != 200 ) {
						Assert.fail( "Login failed");
					}
					loginToWith.releaseConnection();
									
					// now try to upload a file
					HttpPost aFile = new HttpPost( "http://localhost:3333/media/create?file=true");
					File testFile = new File( "public/images/dancer.jpg");
					MultipartEntityBuilder builder = MultipartEntityBuilder.create();
					builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
					builder.addBinaryBody("upfile", testFile, ContentType.create("image/jpeg"), "testImage001.jpg");
					aFile.setEntity( builder.build());
					response = hc.execute( aFile );
					String jsonResponse = EntityUtils.toString(
							response.getEntity(), "UTF8");
					String id = JsonPath.parse( jsonResponse ).read( "$['results'][0]['mediaId']"); 
					aFile.releaseConnection();
					
					assertThat( id ).isNotEmpty();
					// maybe retrieve to see if its there
					HttpGet get = new HttpGet( "http://localhost:3333/media/"+id);
					
					
					
					// maybe remove the media again
					HttpDelete del = new HttpDelete( "http://localhost:3333/media/"+id);
					response = hc.execute(del);
					assertThat( response.getStatusLine().getStatusCode() ).isEqualTo( 200 );
					
				} catch( Exception e ) {
					Assert.fail( e.toString() );
				}
		    });
		}	

}
