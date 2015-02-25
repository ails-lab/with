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

import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSInputFile;

import model.Media;
import play.Logger;
import play.Logger.ALogger;

public class MediaDAO extends DAO<Media> {
	public static final ALogger log = Logger.of(MediaDAO.class);

	public MediaDAO() {
		super(Media.class);
	}

	public void saveImage(Media media) {
		GridFS gridfs = DB.getGridFs();
		GridFSInputFile mediaGridfsFile = gridfs.createFile(media.getData());

		// set metadata
		mediaGridfsFile.setContentType(media.getMimeType());
		DBObject metadata = mediaGridfsFile.getMetaData();
		metadata.put("width", media.getWidth());
		metadata.put("height", media.getHeight());
		metadata.put("duration", media.getDuration());
		metadata.put("mimeType", media.getMimeType());
		metadata.put("type", media.getType());

		// save the file
		mediaGridfsFile.save();
	}

}
