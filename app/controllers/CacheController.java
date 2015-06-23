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


package controllers;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import javax.imageio.ImageIO;

import model.Media;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import db.DB;

public class CacheController extends Controller {

	public static final ALogger log = Logger.of(CacheController.class);

	public static Result getThumbnail(String thumbnailUrl) {
		if (DB.getMediaDAO().getByExternalUrl(thumbnailUrl) != null) {
			String thumbnailId = DB.getMediaDAO()
					.getByExternalUrl(thumbnailUrl).getDbId().toString();
			return MediaController.getMetadataOrFile(thumbnailId, true);
		} else {
			return cacheThumbnail(thumbnailUrl);
		}
	}

	public static Result cacheThumbnail(String thumbnailUrl) {
		URL url;
		byte[] imageBytes = null;
		URLConnection connection = null;
		try {
			url = new URL(thumbnailUrl);
			connection = (URLConnection) url.openConnection();
			BufferedImage image = ImageIO.read(url);
			String mimeType = connection.getHeaderField("content-type");
			if (mimeType.contains("base64")) {
				imageBytes = Base64.decodeBase64(imageBytes);
				mimeType = mimeType.replace(";base64", "");
			}
			if (mimeType == null) {
				mimeType = connection.getContentType();
			}
			imageBytes = IOUtils.toByteArray(connection.getInputStream());
			Media media = new Media();
			media.setType("IMAGE");
			media.setMimeType(mimeType);
			media.setHeight(image.getHeight());
			media.setWidth(image.getWidth());
			media.setData(imageBytes);
			media.setExternalUrl(thumbnailUrl);
			DB.getMediaDAO().makePermanent(media);
			return MediaController.getMetadataOrFile(
					media.getDbId().toString(), true);
		} catch (Exception e) {
			log.error("Couldn't cache thumbnail:" + e.getMessage());
			return internalServerError(Json.parse("{\"error\":\""
					+ e.getMessage() + "\"}"));
		} finally {
			try {
				connection.getInputStream().close();
			} catch (IOException e) {
				log.error("Couldn't close connection:" + e.getMessage());
			}
		}

	}

}
