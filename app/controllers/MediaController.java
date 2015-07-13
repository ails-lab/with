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
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import model.Media;

import org.apache.commons.io.FileUtils;
import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import utils.AccessManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class MediaController extends Controller {
	public static final ALogger log = Logger.of(MediaController.class);

	/**
	 * get the specified media object from DB
	 */
	public static Result getMetadataOrFile(String mediaId, boolean file) {

		Media media = null;
		try {
			media = DB.getMediaDAO().findById(new ObjectId(mediaId));
		} catch (Exception e) {
			log.error("Cannot retrieve media document from database", e);
			return internalServerError("Cannot retrieve media document from database");
		}

		if (file) {
			return ok(media.getData()).as(media.getMimeType());
		} else {
			JsonNode result = Json.toJson(media);
			return ok(result);
		}

	}

	/**
	 * edit metadata or the actual file of the media object
	 */
	public static Result editMetadataOrFile(String id, boolean file) {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();

		if (json == null) {
			result.put("message", "Invalid json!");
			return badRequest(result);
		}

		if (file) {
			return ok(Json.newObject().put("message", "not implemeted yet!"));
		} else {
			Media newMedia = null;
			try {
				newMedia = DB.getMediaDAO().findById(new ObjectId(id));

				// set metadata

				if (json.has("width"))
					newMedia.setWidth(json.get("width").asInt());
				if (json.has("height"))
					newMedia.setHeight(json.get("height").asInt());
				if (json.has("duration"))
					newMedia.setDuration((float) json.get("duration")
							.asDouble());
				if (json.has("mimeType"))
					newMedia.setMimeType(json.get("mimeType").asText());
				if (json.has("type"))
					newMedia.setType(Media.BaseType.valueOf(json.get("type")
							.asText()));

				DB.getMediaDAO().makePermanent(newMedia);
			} catch (Exception e) {
				log.error("Cannot store Media object to database!", e);
				result.put("message", "Cannot store Media object to database");
				return internalServerError(result);
			}
			result.put("message", "Media object succesfully stored!");
			result.put("media_id", id);
			return ok(result);
		}
	}

	/**
	 * delete a media object from database
	 */
	public static Result deleteMedia(String id) {
		ObjectNode result = Json.newObject();

		try {
			DB.getMediaDAO().deleteById(new ObjectId(id));
		} catch (Exception e) {
			result.put("message", "Cannot delete media object from database");
			return internalServerError(result);
		}
		result.put("message", "Succesfully delete object from database!");
		return ok(result);
	}

	/**
	 * Allow media create with two different methods, by first supplying
	 * metadata or a file File data can arrive in different ways. Whole body is
	 * file content, form based file upload, or json field with encoded file
	 * data.
	 * 
	 * @param fileData
	 * @return
	 */
	public static Result createMedia(boolean fileData) {
		ObjectNode result = Json.newObject();
		List<String> userIds = AccessManager.effectiveUserIds(session().get(
				"effectiveUserIds"));
		if (userIds.isEmpty())
			return forbidden();
		if (fileData) {
			final Http.MultipartFormData multipartBody = request().body()
					.asMultipartFormData();
			if (multipartBody != null) {
				ArrayNode allRes = result.arrayNode();
				for (FilePart fp : multipartBody.getFiles()) {
					try {
						ObjectNode singleRes = Json.newObject();
						// lets start by making binary objects for every part
						Media med = new Media();
						med.setMimeType(fp.getContentType());
						med.setFilename(fp.getFilename());
						med.setOwnerId(new ObjectId(userIds.get(0)));
						if (med.getMimeType().toLowerCase().contains("image")) {
							med.setType(Media.BaseType.IMAGE);
							BufferedImage image = ImageIO.read(fp.getFile());
							int height = image.getHeight();
							int width = image.getWidth();
							med.setHeight(height);
							med.setWidth(width);
							med.setOriginal(true);
							if (width < 212) {
								med.setThumbnail(true);
							} else {
								// Resize image and put new width, height and
								// bytes to data
								Image ithumb = image.getScaledInstance(211, -1,
										Image.SCALE_SMOOTH);
								// Create a buffered image with transparency
								BufferedImage thumb = new BufferedImage(
										ithumb.getWidth(null),
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
								Media thumbMedia = new Media();
								thumbMedia = med;
								thumbMedia.setData(thumbByte);
								thumbMedia.setWidth(thumb.getWidth());
								thumbMedia.setHeight(thumb.getHeight());
								thumbMedia.setThumbnail(true);
								thumbMedia.setOriginal(false);
								DB.getMediaDAO().makePermanent(thumbMedia);
								singleRes.put(fp.getFilename() + "_thumb",
										thumbMedia.getDbId().toString());
							}
						}
						med.setData(FileUtils.readFileToByteArray(fp.getFile()));
						DB.getMediaDAO().makePermanent(med);
						singleRes.put(fp.getFilename(), med.getDbId()
								.toString());
						allRes.add(singleRes);
					} catch (Exception e) {
						ObjectNode singleRes = Json.newObject();
						singleRes.put("error", "Couldn't create from file "
								+ fp.getFilename());
						allRes.add(singleRes);
						log.error("Media create error", e);
					}
				}
				result.put("results", allRes);
			} else {
				final Map<String, String[]> req = request().body()
						.asFormUrlEncoded();
				if (req != null) {
					// this should be rare for file data
				} else {
					final JsonNode jsonBody = request().body().asJson();
					if (jsonBody != null) {
						// we extract the media and maybe some metadata from the
						// json body

					} else {
						// raw body to file upload
						// problem, there is absolutely no metadata, so don't
						// know what to put in the Media Object
						try {
							Media med = new Media();
							med.setData(request().body().asRaw().asBytes());
							DB.getMediaDAO().makePermanent(med);
							result.put("Success", "Media object created!");
							result.put("mediaId", med.getDbId().toString());
						} catch (Exception e) {
							result.put("error", "Couldn't create Media object");
							log.error("Media create error", e);
						}
					}
				}
			}
		} else {
			// metadata based media creation
		}
		return ok(result);
	}
}
