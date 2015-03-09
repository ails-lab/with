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


package test;
import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import model.Media;
import model.Record;
import model.RecordLink;
import model.Search;
import model.User;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import play.twirl.api.Content;

import com.mongodb.MongoException;

import db.DB;

/**
 *
 * Simple (JUnit) tests that can call all parts of a play app. If you are
 * interested in mocking a whole application, see the wiki for more details.
 *
 */
//@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DBTests {



	@Test
	public void test_Record_and_Media_storage() throws IOException, URISyntaxException {

		//Create a Media Object
		Media image = new Media();

		URL url = new URL("http://clips.vorwaerts-gmbh.de/VfE_html5.mp4");
		File file = new File("test_java.txt");
		FileUtils.copyURLToFile(url, file);
		FileInputStream fileStream = new FileInputStream(
				file);

		byte[] rawbytes = IOUtils.toByteArray(fileStream);

		image.setData(rawbytes);
		image.setType("video/mp4");
		image.setMimeType("mp4");
		image.setDuration(0.0f);
		image.setHeight(1024);
		image.setWidth(1080);

		DB.getMediaDAO().makePermanent(image);

		//Create Record Object
		Record record = new Record();
		DB.getRecordDAO().save(record);

		//Create a RecordLink Object
		//and references to Media and Record

		//Get Media object
		Media imageRetrieved = DB.getMediaDAO().find("54ef0a09e4b0af9ca4dc8fbc");
		//Get Record object
		Record recordRetrieved = DB.getRecordDAO().find().get();

		RecordLink rlink = new RecordLink();
		rlink.setThumbnail(imageRetrieved);
		rlink.setRecordReference(recordRetrieved);

		DB.getRecordLinkDAO().save(rlink);

	}


	@Test
	public void renderTemplate() {
		Content html = views.html.index
				.render("Your new application is ready.");
		assertThat(contentType(html)).isEqualTo("text/html");
		assertThat(contentAsString(html)).contains(
				"Your new application is ready.");
	}

}
