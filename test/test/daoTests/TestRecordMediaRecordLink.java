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


package test.daoTests;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import model.Media;
import model.RecordLink;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import db.DB;


public class TestRecordMediaRecordLink  {

	@Test
	public void test_Record_and_Media_storage() throws IOException, URISyntaxException {

		for(int i = 0; i < 100; i++) {
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
		}

		//Create Record Object
		//Record record = new Record();
		//DB.getRecordDAO().save(record);

		//Create a RecordLink Object
		//and references to Media and Record

		//Get Media object
		Media imageRetrieved = DB.getMediaDAO().findById("55003261e4b0dadbf3dbbd10");
		//Get Record object
		//Record recordRetrieved = DB.getRecordDAO().find().get();


		for(int i = 0; i < 1000; i++) {
			RecordLink rlink = new RecordLink();
			rlink.setDescription("test RecordLink");
			rlink.setRights("CC");
			rlink.setSource("blabla");
			rlink.setSourceId("sourceId");
			rlink.setSourceUrl("http://blabgla.gr");
			rlink.setType("image");
			rlink.setTitle("The test title");
			rlink.setThumbnail(imageRetrieved);
			//rlink.setRecordReference(recordRetrieved);

			DB.getRecordLinkDAO().save(rlink);
		}
	}

}
