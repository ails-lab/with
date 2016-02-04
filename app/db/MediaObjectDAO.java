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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.mongodb.morphia.mapping.cache.DefaultEntityCache;

import play.Logger;
import play.Logger.ALogger;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;

import model.EmbeddedMediaObject.MediaVersion;
import model.MediaObject;

public class MediaObjectDAO {
	public static final ALogger log = Logger.of(MediaObjectDAO.class);

	/**
	 * Converts GridFSDBFile to MediaObject. We use this method for convertion
	 * because we cannot cast directly to Media. We also cannot add Media as a
	 * subclass to GridFSDBFile because otherwise we will have to "_id" fields
	 * and the Morphia document mapper will fail.
	 *
	 * @param gridfsDbFile
	 * @return
	 */
	private MediaObject gridFsDbFileToMediaObj(GridFSDBFile gridfsDbFile) {

		if (gridfsDbFile == null)
			return null;

		// some things are not quite right, so we repair those

		try {
			MediaObject media = DB
					.getMorphia()
					.getMapper()
					.fromDBObject(MediaObject.class, gridfsDbFile,
							new DefaultEntityCache());
			media.setMediaBytes(IOUtils.toByteArray(gridfsDbFile
					.getInputStream()));
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

	/**
	 * Retrieve a MediaObject from GridFS using the dbId
	 * 
	 * @param dbId
	 * @return
	 */
	public MediaObject findById(ObjectId dbId) {
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

	/**
	 * Delete a MediaObject file from GridFS using it's dbId
	 * 
	 * @param dbId
	 */
	public void deleteById(ObjectId dbId) {
		try {
			DB.getGridFs().remove(dbId);
		} catch (Exception e) {
			log.error("Cannot delete Media document from GridFS", e);
			throw e;
		}
	}

	/**
	 * Stores the MediaObject to GridFS filesystem. This method internally
	 * converts the MediaObject POJO to a GridFSFile and then stores it to
	 * GridFS.
	 * 
	 * @param media
	 * @throws Exception
	 */
	public void makePermanent(MediaObject media) throws Exception {
		GridFSFile mediaGridFsFile;

		try {

			if (media.getDbId() != null) {
				mediaGridFsFile = DB.getGridFs().find(media.getDbId());
			} else {
				if (media.getMediaBytes() == null) {
					String tmp = new String(); // an empty string

					mediaGridFsFile = DB.getGridFs().createFile(
							new ByteArrayInputStream(tmp.getBytes()));
				} else
					mediaGridFsFile = DB.getGridFs().createFile(
							media.getMediaBytes());
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

	/**
	 * Deletes a MediaObject from GridFS.
	 * 
	 * @param media
	 */
	public void makeTransient(MediaObject media) {
		try {
			DB.getGridFs().remove(media.getDbId());
		} catch (Exception e) {
			log.error("Cannot delete Media document from GridFS", e);
			throw e;
		}
	}

	/**
	 * Retrive a MediaObject from GridFS according to it's external url.
	 * According to the value specified, we return either the thumbnail or the
	 * original media.
	 * 
	 * @param externalUrl
	 * @param thumbnail
	 * @return
	 */
	public MediaObject getByThumbnailUrl(String withThumbUrl) {
		GridFSDBFile media = null;
		try {
			// here I have to ask where we will use 'url' or 'withUrl' or both
			BasicDBObject query = new BasicDBObject("withThumbnailUrl",
					withThumbUrl);
			media = DB.getGridFs().findOne(query);
			if (media.containsField("mediaBytes"))
				media.removeField("mediaBytes");
			return gridFsDbFileToMediaObj(media);
		} catch (Exception e) {
			log.error("Problem in find file from GridFS " + withThumbUrl);
			return null;
		}
	}

	/**
	 * Retrive a MediaObject from GridFS according to it's external url.
	 * According to the value specified, we return either the thumbnail or the
	 * original media.
	 * 
	 * @param externalUrl
	 * @param thumbnail
	 * @return
	 */
	public MediaObject getByUrl(String withUrl) {
		GridFSDBFile media = null;
		try {
			// here I have to ask where we will use 'url' or 'withUrl' or both
			BasicDBObject query = new BasicDBObject("url", withUrl);
			media = DB.getGridFs().findOne(query);
			if (media.containsField("thumbnailBytes"))
				media.removeField("thumbnailBytes");
			return gridFsDbFileToMediaObj(media);
		} catch (Exception e) {
			log.error("Problem in find file from GridFS " + withUrl);
			return null;
		}
	}

	public MediaObject getByUrlAndVersion(String url, MediaVersion version) {
		GridFSDBFile media = null;
		try {
			// here I have to ask where we will use 'url' or 'withUrl' or both
			BasicDBObject query = new BasicDBObject("url", url);
			query.append("mediaVersion", version.toString());
			media = DB.getGridFs().findOne(query);
			return gridFsDbFileToMediaObj(media);
		} catch (Exception e) {
			log.error("Problem in find file from GridFS " + url);
			return null;
		}
	}

	/**
	 * Count all documents that match the query specified
	 * 
	 * @param query
	 * @return
	 */
	public int countAll(BasicDBObject query) {
		return DB.getGridFs().find(query).size();
	}

	/**
	 * Delete all MediaObject entities from GridFs
	 */
	public void deleteCached() {
		try {
			BasicDBObject query = new BasicDBObject();
			query.containsField("url");
			DB.getGridFs().remove(query);
		} catch (Exception e) {
			log.error("Cannot delete files from GridFS", e);
			throw e;
		}
	}

}
