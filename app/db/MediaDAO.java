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

import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

public class MediaDAO extends DAO<Media> {
	public static final ALogger log = Logger.of(MediaDAO.class);

	public MediaDAO() {
		super(Media.class);
	}

	/**Converts GridFSDBFile to Media object.
	 * We use this method for convertion because we cannot cast directly to Media.
	 * We also cannot add Media as a subclass to GridFSDBFile because otherwise
	 * we will have to "_id" fields and the Morphia document mapper will fail.
	 * @param gridfsDbFile
	 * @return
	 */
	private Media gridFsDbFileToMediaObj(GridFSDBFile gridfsDbFile){
		Media media = new Media();
		media.setType((String)gridfsDbFile.get("type"));
		media.setMimeType(gridfsDbFile.getContentType());
		//media.setDuration((float)gridfsDbFile.get("duration"));
		media.setHeight((int)gridfsDbFile.get("height"));
		media.setWidth((int)gridfsDbFile.get("width"));
		media.setDbId((ObjectId)gridfsDbFile.getId());
		try {
			media.setData(IOUtils.toByteArray(gridfsDbFile.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return media;
	}

	public Media find(String dbId) {
		ObjectId objectId = new ObjectId(dbId);
		Media media = gridFsDbFileToMediaObj(DB.getGridFs().find(objectId));

		return media;
	}

	@Override
	public void makePermanent(Object obj) {
		Media media = (Media)obj;
		GridFS gridfs = DB.getGridFs();
		GridFSInputFile mediaGridfsFile = gridfs.createFile(media.getData());

		// set metadata
		mediaGridfsFile.setContentType(media.getMimeType());
		mediaGridfsFile.put("width", media.getWidth());
		mediaGridfsFile.put("height", media.getHeight());
		mediaGridfsFile.put("duration", media.getDuration());
		mediaGridfsFile.put("mimeType", media.getMimeType());
		mediaGridfsFile.put("type", media.getType());

		// save the file
		mediaGridfsFile.save();
	}

}
