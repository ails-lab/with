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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import model.EmbeddedMediaObject.Quality;
import model.EmbeddedMediaObject.WithMediaType;
import model.Media;
import model.MediaObject;
import model.basicDataTypes.Literal;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.LiteralOrResource.ResourceType;

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
import com.google.common.net.MediaType;

import db.DB;

public class MediaController extends Controller {
	public static final ALogger log = Logger.of(MediaController.class);

	/**
	 * get the specified media object from DB
	 */
	public static Result getMetadataOrFile(String mediaId, boolean file) {

		MediaObject media = null;
		try {
			media = DB.getMediaObjectDAO().findById(new ObjectId(mediaId));
		} catch (Exception e) {
			log.error("Cannot retrieve media document from database", e);
			return internalServerError("Cannot retrieve media document from database");
		}

		if (file) {
//			confirm this is right! .as Changes the Content-Type header for this result. 
			//Logger.info(media.getMimeType().toString());
			return ok(media.getMediaBytes()).as(media.getMimeType().toString());
		} else {
			Logger.info("boom");
			//Logger.info(media.getMimeType().toString());
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
			MediaObject newMedia = null;
			try {
				newMedia = DB.getMediaObjectDAO().findById(new ObjectId(id));

				// set metadata
				
//				//why should these be set by a user if we can extract them automatically?
//				//is this just a workaround for videos?
				if (json.has("width"))
					newMedia.setWidth(json.get("width").asInt());
				if (json.has("height"))
					newMedia.setHeight(json.get("height").asInt());
				//how to check if it's in seconds?
				if (json.has("duration"))
					newMedia.setDurationSeconds((float) json.get("duration")
							.asDouble());
				if (json.has("mimeType"))
					newMedia.setMimeType(MediaType.parse(json.get("mimeType").asText()));

//				MAKE custom parser like above or just delete this option? maybe just put
//				it as a string like marios said?				
//								
//				if (json.has("type"))
//					newMedia.setType(Media.BaseType.valueOf(json.get("type")
//							.asText()));

				DB.getMediaObjectDAO().makePermanent(newMedia);
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
			DB.getMediaObjectDAO().deleteById(new ObjectId(id));
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
		//if (userIds.isEmpty())
		//	return forbidden();
		if (fileData) {
			final Http.MultipartFormData multipartBody = request().body()
					.asMultipartFormData();
			if (multipartBody != null) {
				ArrayNode allRes = result.arrayNode();
				for (FilePart fp : multipartBody.getFiles()) {
					try {
						ObjectNode singleRes = Json.newObject();
						MediaObject med = new MediaObject();
						
						med.setMimeType(MediaType.parse(fp.getContentType()));
						Logger.info(med.getMimeType().toString());
						
						// in KB!
						med.setSize(fp.getFile().length()/1024);
						
						Map<String, String[]> formData = multipartBody.asFormUrlEncoded();
						
//						//make them nested ifs? i mean if there is no image url can there be thumb url?
						
						if(formData.containsKey("url")){
							med.setUrl(formData.get("url")[0]);
						}
						
						if(formData.containsKey("thumbnailUrl")){
							med.setThumbnailUrl(formData.get("thumbnailUrl")[0]);
						}
						
//						//how do we get these? :/ is it possible to post a json array in multipartform?
//						a function to withmediarights from string :(								
						//String[] withMediaRights = formData.get("withMediaRights");
						
						if(formData.containsKey("resourceType")){
							if(formData.containsKey("uri")){
								LiteralOrResource lit = new LiteralOrResource();
//								ASKOASKASKASK	
//								a function to parse resourcetype from string :(								
								//String x = formData.get("resourceType")[0];
								// if they upload a new resource...
								lit.setResource(ResourceType.withRepository, formData.get("uri")[0]);
								med.setOriginalRights(lit);
							}
						}
						
						if(med.getMimeType().is(MediaType.ANY_IMAGE_TYPE)){
							med.setType(WithMediaType.IMAGE);
							BufferedImage image = ImageIO.read(fp.getFile());
							int height = image.getHeight();
							int width = image.getWidth();
							med.setHeight(height);
							med.setWidth(width);
							
//							//PROBABLY WRONG!
							long size = med.getSize();
							
							if(size<1){
								med.setQuality(Quality.IMAGE_SMALL);
							} else if(size<500){
								med.setQuality(Quality.IMAGE_500k);
							} else if(size<1000) {
								med.setQuality(Quality.IMAGE_1);
							} else {
								med.setQuality(Quality.IMAGE_4);
							}

							//thumbnail
							
							
							if(!formData.containsKey("thumbnailUrl")){
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
								med.setThumbnailBytes(thumbByte);
							} else { //cache thumb from the url!
								URL url = new URL(med.getThumbnailUrl());
								Image ithumb = ImageIO.read(url);
							    
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
								med.setThumbnailBytes(thumbByte);							    
							}
							//endimage
							
							
//						} else if(med.getMimeType().is(MediaType.ANY_VIDEO_TYPE)){
//							med.setType(WithMediaType.VIDEO);
//							//durationSeconds
//							//width, height
//							//thumbnailBytes
							//Quality
//							
							//
							
//						} else if(med.getMimeType().is(MediaType.ANY_TEXT_TYPE)){
//							med.setType(WithMediaType.TEXT);
//
//						} else if(med.getMimeType().is(MediaType.ANY_AUDIO_TYPE)){
//							med.setType(WithMediaType.AUDIO);
//							//durationSeconds
//							//Quality

						} else {
							
							Logger.info("OHNO");
							//impossible!? (ANY_APPLICATION_TYPE) (reject)
							singleRes.put("error", "Unsupported media type "
									+ fp.getFilename());
							allRes.add(singleRes);
							log.error("Media create error", "Unsupported media type");
						}
						
						med.setMediaBytes(FileUtils.readFileToByteArray(fp.getFile()));						
						med.setDbId(null);
						DB.getMediaObjectDAO().makePermanent(med);
						
						singleRes.put("isShownBy", "/media/"
								+ med.getDbId().toString());
						singleRes.put("externalId", med.getDbId().toString());
						allRes.add(singleRes);
					} catch (Exception e) {
						ObjectNode singleRes = Json.newObject();
						singleRes.put("error", "Couldn't create from file "
								+ fp.getFilename());
						allRes.add(singleRes);
						log.error("Media create error", e);
					}
				}
				
				
//				med.setUrl() //how should the call be changed if the user wants to provide this?
//							 //also this needs to be made permanent first and then get its dbid
//							 //to set the internalurl.... see your friend the user manager !

				result.put("results", allRes);
			} else {
				final Map<String, String[]> req = request().body()
						.asFormUrlEncoded();
				Logger.info("whosp");
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
							MediaObject med = new MediaObject();
							med.setMediaBytes(request().body().asRaw().asBytes());
							DB.getMediaObjectDAO().makePermanent(med);
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
			
			//does this mean we have to connect to the uri and get the media?
			System.out.println("whops!");

		}
		return ok(result);
	}
	
	
	///delete?

	/**
	 * Allow media create with two different methods, by first supplying
	 * metadata or a file File data can arrive in different ways. Whole body is
	 * file content, form based file upload, or json field with encoded file
	 * data.
	 * 
	 * @param fileData
	 * @return
	 */
	public static Result oldCreateMedia(boolean fileData) {
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
								//med.setThumbnail(true);
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
								thumbMedia.setMimeType(med.getMimeType());
								thumbMedia.setFilename(med.getFilename());
								thumbMedia.setOwnerId(med.getOwnerId());
								med.setType(med.getType());
								thumbMedia.setData(thumbByte);
								thumbMedia.setWidth(thumb.getWidth());
								thumbMedia.setHeight(thumb.getHeight());
								thumbMedia.setThumbnail(true);
								thumbMedia.setOriginal(false);
								DB.getMediaDAO().makePermanent(thumbMedia);
								singleRes.put("thumbnailUrl", "/media/"
										+ thumbMedia.getDbId().toString());
								
							
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
								thumbMedia.setMimeType(med.getMimeType());
								thumbMedia.setFilename(med.getFilename());
								thumbMedia.setOwnerId(med.getOwnerId());
								med.setType(med.getType());
								thumbMedia.setData(thumbByte);
								thumbMedia.setWidth(thumb.getWidth());
								thumbMedia.setHeight(thumb.getHeight());
								thumbMedia.setThumbnail(true);
								thumbMedia.setOriginal(false);
								DB.getMediaDAO().makePermanent(thumbMedia);
								singleRes.put("thumbnailUrl", "/media/"
										+ thumbMedia.getDbId().toString());
							}
						}
						med.setData(FileUtils.readFileToByteArray(fp.getFile()));
						med.setDbId(null);
						DB.getMediaDAO().makePermanent(med);
						singleRes.put("isShownBy", "/media/"
								+ med.getDbId().toString());
						singleRes.put("externalId", med.getDbId().toString());
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
