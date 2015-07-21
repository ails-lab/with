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

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.imageio.ImageIO;

import model.Media;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import play.Logger;
import play.Logger.ALogger;
import play.mvc.Controller;
import play.mvc.Result;
import db.DB;
import espace.core.ParallelAPICall;

public class CacheController extends Controller {

	public static final ALogger log = Logger.of(CacheController.class);

	public static Result getImage(String externalUrl, boolean thumbnail) {
		Media image = DB.getMediaDAO().getByExternalUrl(externalUrl, thumbnail);
		if (image != null) {
			String imageId = image.getDbId().toString();
			updateCache(externalUrl, thumbnail);
			return MediaController.getMetadataOrFile(imageId, true);
		} else {
			cacheImage(externalUrl);
			return redirect(externalUrl);
		}
	}

	// TODO add more fields (like last access)
	private static void updateCache(String externalUrl, boolean thumbnail) {
		BiFunction<String, Boolean, Boolean> methodQuery = (String imageUrl,
				Boolean thumb) -> {
			try {
				Media image = DB.getMediaDAO()
						.getByExternalUrl(imageUrl, thumb);
				if (image != null) {
					image.incrAccessCount();
				}
				DB.getMediaDAO().makePermanent(image);
				return true;
			} catch (Exception e) {
				log.error(e.getMessage());
				return false;
			}
		};
		ParallelAPICall.createPromise(methodQuery, externalUrl, thumbnail);
	}

	private static void cacheImage(String externalUrl) {
		Function<String, Boolean> methodQuery = (String imageUrl) -> {
			URL url;
			byte[] imageBytes = null;
			URLConnection connection = null;
			try {
				url = new URL(externalUrl);
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
				int height = image.getHeight();
				int width = image.getWidth();
				Media media = new Media();
				media.setType(Media.BaseType.IMAGE);
				media.setMimeType(mimeType);
				media.setHeight(height);
				media.setWidth(width);
				media.setData(imageBytes);
				media.setExternalUrl(externalUrl);
				media.setOriginal(true);
				if (width < 212) {
					media.setThumbnail(true);
					DB.getMediaDAO().makePermanent(media);
					return true;
				}
				// image is big
				// store original and resize for thumbnail
				media.setThumbnail(false);
				Media fullImage = media;
				DB.getMediaDAO().makePermanent(fullImage);
				// Resize image and put new width, height and bytes to data
				Image ithumb = image.getScaledInstance(211, -1,
						Image.SCALE_SMOOTH);
				// Create a buffered image with transparency
				BufferedImage thumb = new BufferedImage(ithumb.getWidth(null),
						ithumb.getHeight(null), image.getType());
				// Draw the image on to the buffered image
				Graphics2D bGr = thumb.createGraphics();
				bGr.drawImage(ithumb, 0, 0, null);
				bGr.dispose();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(thumb, "jpg", baos);
				baos.flush();
				byte[] thumbByte = baos.toByteArray();
				baos.close();
				media.setDbId(null);
				media.setData(thumbByte);
				media.setWidth(thumb.getWidth());
				media.setHeight(thumb.getHeight());
				media.setThumbnail(true);
				media.setOriginal(false);
				DB.getMediaDAO().makePermanent(media);
				return true;
			} catch (Exception e) {
				log.error("Couldn't cache thumbnail:" + e.getMessage());
				return false;
			}
		};
		ParallelAPICall.createPromise(methodQuery, externalUrl);
	}

	public static Result clearCache() {
		try {
			DB.getMediaDAO().deleteCached();
			return ok("Cache cleared");
		} catch (Exception e) {
			log.error("Couldn't clear cache:" + e.getMessage());
			return internalServerError(e.getMessage());
		}
	}
}
