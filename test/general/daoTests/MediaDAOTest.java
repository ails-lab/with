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

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;

import model.Media;
import model.WithAccess;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.junit.Test;

import com.sun.media.jfxmedia.MediaError;

import db.DB;

public class MediaDAOTest {

	@Test
	public void testCRUD() throws Exception {
		//create
		Media thumb = new Media();
		byte[] rawbytes = null;
		try {
			URL url = new URL("http://www.ntua.gr/schools/ece.jpg");
			File file = new File("test_java.txt");
			FileUtils.copyURLToFile(url, file);
			FileInputStream fileStream = new FileInputStream(
					file);

			rawbytes = IOUtils.toByteArray(fileStream);
		} catch(Exception e) {
			System.out.println(e);
			System.exit(-1);
		}

		thumb.setData(rawbytes);
		thumb.setType(Media.BaseType.IMAGE);
		thumb.setMimeType("image/jpeg");
		thumb.setHeight(599);
		thumb.setWidth(755);

		thumb.getRights().setPublic( true );
		thumb.getRights().put( new ObjectId() ,WithAccess.Access.OWN);
		thumb.getRights().put( new ObjectId() ,WithAccess.Access.WRITE);
		
		DB.getMediaDAO().makePermanent(thumb);
		//test succesful storage
		assertThat(thumb.getDbId()).isNotNull()
			.overridingErrorMessage("Media object didn't not save correctly in DB!");

		//retrieve from db
		Media a = DB.getMediaDAO().findById(thumb.getDbId());
		assertThat(a).isNotNull()
		.overridingErrorMessage("Test media not found after store.");

		//check is gone
		// DB.getMediaDAO().makeTransient(a);
		Media b = DB.getMediaDAO().findById(thumb.getDbId());
		assertThat( b )
		.overridingErrorMessage("User not deleted!")
		.isNull();

	}


	public Media testMediaStorage() throws Exception {

		Media image = null;
			//Create a Media Object
			image = new Media();


			URL url = new URL("http://www.ntua.gr/ntua-01.jpg");
			File file = new File("test_java.txt");
			FileUtils.copyURLToFile(url, file);
			FileInputStream fileStream = new FileInputStream(
					file);

			byte[] rawbytes = IOUtils.toByteArray(fileStream);


			image.setData(rawbytes);
			image.setType(Media.BaseType.IMAGE);
			image.setMimeType("image/jpeg");
			image.setHeight(599);
			image.setWidth(755);

			DB.getMediaDAO().makePermanent(image);

		assertThat( image.getDbId()).isNotNull();
		return image;

	}

}
