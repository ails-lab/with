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
import java.util.ArrayList;
import java.util.List;

import model.Media;

import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.mongodb.morphia.mapping.cache.DefaultEntityCache;

import play.Logger;
import play.Logger.ALogger;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;

public class MediaDAO {
	public static final ALogger log = Logger.of(MediaDAO.class);

	/**
	 * Converts GridFSDBFile to Media object. We use this method for convertion
	 * because we cannot cast directly to Media. We also cannot add Media as a
	 * subclass to GridFSDBFile because otherwise we will have to "_id" fields
	 * and the Morphia document mapper will fail.
	 * 
	 * @param gridfsDbFile
	 * @return
	 */
	private Media gridFsDbFileToMediaObj(GridFSDBFile gridfsDbFile) {

		if (gridfsDbFile == null)
			return null;

		// some things are not quite right, so we repair those

		try {
			Media media = DB
					.getMorphia()
					.getMapper()
					.fromDBObject(Media.class, gridfsDbFile,
							new DefaultEntityCache());
			media.setData(IOUtils.toByteArray(gridfsDbFile.getInputStream()));
			return media;
		} catch (IOException e) {
			log.error(
					"Error transforming media file's InputStream to raw bytes",
					e);
		} catch (Exception e) {
			log.error("Error setting properties to Media object", e);
		}
		return null;
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

			if (media.getDbId() != null) {
				mediaGridFsFile = DB.getGridFs().find(media.getDbId());

			} else {
				mediaGridFsFile = DB.getGridFs().createFile(media.getData());
			}
			DBObject mediaDbObj = DB.getMorphia().getMapper().toDBObject(media);
			// remove stuff we don't want on the media object
			mediaDbObj.removeField("className");
			mediaDbObj.removeField("data");
			mediaDbObj.removeField("_id");

			for (String k : mediaDbObj.keySet()) {
				mediaGridFsFile.put(k, mediaDbObj.get(k));
			}
			mediaGridFsFile.save();
			media.setDbId((ObjectId) mediaGridFsFile.getId());

			// save the file
		} catch (Exception e) {
			log.error("Cannot save Media document to GridFS", e);
			throw e;
		}
	}

	public Media getByExternalUrl(String externalUrl, boolean thumbnail) {
		GridFSDBFile media = null;
		try {
			BasicDBObject query = new BasicDBObject("externalUrl", externalUrl);
			if (thumbnail) {
				query.append("thumbnail", true);
			} else {
				query.append("original", true);
			}
			media = DB.getGridFs().findOne(query);
			return gridFsDbFileToMediaObj(media);
		} catch (Exception e) {
			log.error("Problem in find file from GridFS " + externalUrl);
			return null;
		}
	}

	public ArrayList<Media> findByOwnerId(ObjectId ownerId) {
		List<GridFSDBFile> files = new ArrayList<GridFSDBFile>();
		try {
			BasicDBObject query = new BasicDBObject("ownerId", ownerId);
			files = DB.getGridFs().find(query);
		} catch (Exception e) {
			log.error("Problem in find file from GridFS " + ownerId);
		}

		if (files == null)
			log.debug("Cannot find Media document with owner ID: " + ownerId);
		else
			log.debug("Succesfully found Media GridFS document");
		ArrayList<Media> media = new ArrayList<Media>();
		for (GridFSDBFile file : files) {
			media.add(gridFsDbFileToMediaObj(file));
		}
		return media;
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
