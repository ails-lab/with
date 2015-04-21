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


package db;

import java.io.IOException;

import model.Media;

import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;

import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;

public class MediaDAO  {
	public static final ALogger log = Logger.of(MediaDAO.class);



	/**Converts GridFSDBFile to Media object.
	 * We use this method for convertion because we cannot cast directly to Media.
	 * We also cannot add Media as a subclass to GridFSDBFile because otherwise
	 * we will have to "_id" fields and the Morphia document mapper will fail.
	 * @param gridfsDbFile
	 * @return
	 */
	private Media gridFsDbFileToMediaObj(GridFSDBFile gridfsDbFile) {

		if(gridfsDbFile == null)
			return null;

		Media media = new Media();

		try {
			// set metadata to media object
			media.setType((String)gridfsDbFile.get("type"));
			media.setMimeType((String)gridfsDbFile.get("mimeType"));
			if(gridfsDbFile.keySet().contains("duration"))
				media.setDuration(new Double((double) gridfsDbFile.get("duration")).floatValue());
			media.setHeight((int)gridfsDbFile.get("height"));
			media.setWidth((int)gridfsDbFile.get("width"));
			media.setDbId((ObjectId)gridfsDbFile.getId());
			media.setData(IOUtils.toByteArray(gridfsDbFile.getInputStream()));
		} catch (IOException e) {
			log.error("Error transforming media file's InputStream to raw bytes", e);
		} catch (Exception e) {
			log.error("Error setting properties to Media object", e);
		}

		return media;
	}


	public Media findById(ObjectId dbId) {
		GridFSDBFile media = null;
		try {
			media = DB.getGridFs().find(dbId);
		} catch (Exception e) {
			log.error("Problem in find file from GridFS " + dbId);
		}

		if (media == null)
			log.debug("Cannot find Media document with ID: " + dbId);
		else
			log.debug("Succesfully found Media GridFS document");

		return gridFsDbFileToMediaObj(media);
	}

	public void deleteById(ObjectId dbId) {
		try {
			DB.getGridFs().remove(dbId);
		} catch (Exception e) {
			log.error("Cannot delete Media document from GridFS", e);
			throw e;
		}
	}

	public void makePermanent(Media media) throws Exception {
		GridFSFile mediaGridFsFile;

		try {

			if( media.getDbId() != null ) {
				mediaGridFsFile = DB.getGridFs().find(media.getDbId());
			} else {
				mediaGridFsFile = DB.getGridFs().createFile(media.getData());
			}

			if(mediaGridFsFile == null)
				throw new Exception("Got a NULL mediaGridFsFile");

			// set metadata

			if(media.hasWidth())
				mediaGridFsFile.put("width", media.getWidth());
			if(media.hasHeight())
				mediaGridFsFile.put("height", media.getHeight());
			if(media.hasDuration())
				mediaGridFsFile.put("duration", media.getDuration());
			if(media.hasMimeType())
				mediaGridFsFile.put("mimeType", media.getMimeType());
			if(media.hasType())
				mediaGridFsFile.put("type", media.getType());

			// save the file
			mediaGridFsFile.save();
			media.setDbId((ObjectId) mediaGridFsFile.getId());
		} catch (Exception e) {
			log.error("Cannot save Media document to GridFS", e);
			throw e;
		}
	}


	public void makeTransient(Media media) {
		try {
			DB.getGridFs().remove(media.getDbId());
		} catch (Exception e) {
			log.error("Cannot delete Media document from GridFS", e);
			throw e;
		}
	}

}
