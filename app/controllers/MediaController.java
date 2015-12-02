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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import model.EmbeddedMediaObject.Quality;
import model.EmbeddedMediaObject.WithMediaRights;
import model.EmbeddedMediaObject.WithMediaType;
import model.ExternalBasicRecord.ItemRights;
import model.Media;
import model.MediaObject;
import model.basicDataTypes.Literal;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.LiteralOrResource.ResourceType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
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
//				//extract method!!! 				
				
				
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
		ArrayNode allRes = result.arrayNode();
		

		
		
		List<String> userIds = AccessManager.effectiveUserIds(session().get(
				"effectiveUserIds"));
		//if (userIds.isEmpty())
		//	return forbidden();
		ObjectNode singleRes = Json.newObject();
		MediaObject med = new MediaObject();

		if (fileData) {
			final Http.MultipartFormData multipartBody = request().body()
					.asMultipartFormData();
			if (multipartBody != null) {
				for (FilePart fp : multipartBody.getFiles()) {
					try {
						med.setMimeType(MediaType.parse(fp.getContentType()));
						// in KB!
						med.setSize(fp.getFile().length()/1024);
						
						//get data from multipartBody
						Map<String, String[]> formData = multipartBody.asFormUrlEncoded();
						
						if(formData.containsKey("url")){
							singleRes.put("warn", "External url is ignored when uploading files");
							allRes.add(singleRes);
						}
						
//						String[] withMediaRights = formData.get("withMediaRights");
						
						
//						name keys accordignly?						
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
							imageUpload(allRes, singleRes, med, fp, formData);
							
//						} else if(med.getMimeType().is(MediaType.ANY_VIDEO_TYPE)){
//							med.setType(WithMediaType.VIDEO);//durationSeconds //width, height //thumbnailBytes //Quality						
//						
//						} else if(med.getMimeType().is(MediaType.ANY_TEXT_TYPE)){
//							med.setType(WithMediaType.TEXT);
//
//						} else if(med.getMimeType().is(MediaType.ANY_AUDIO_TYPE)){
//							med.setType(WithMediaType.AUDIO); //durationSeconds	//Quality

						} else {
							//(ANY_APPLICATION_TYPE?)
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
						singleRes = Json.newObject();
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
//					this means we have form data but no file, do we even allow this??
//					don't we want to force json in this case?					
					// this should be rare for file data
				} else {
					final JsonNode jsonBody = request().body().asJson();
					if (jsonBody != null) {
//						then why do we even need the boolean parameter???						
						// we extract the media and maybe some metadata from the
						// json body

					} else {
//						again why should we even allow this?						
						// raw body to file upload
						// problem, there is absolutely no metadata, so don't
						// know what to put in the Media Object
						try {
							med = new MediaObject();
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
			
			JsonNode json = null;
			ObjectNode error = Json.newObject();
			
			json = request().body().asJson();
			if(json==null){
				error.put("emptyBody", "Empty Json Body (file parameter is false)");
				result.put("error", error);
				return badRequest(result);
			}
			
			if(json.has("url")){
				med.setUrl(json.get("url").asText());
			} else {
				error.put("emptyUrl", "Empty url for the media object");
				result.put("error", error);
				return badRequest(result);
			}
			
			String type = null;
			if(json.has("type")){
				type = json.get("type").asText();
			} else {
				//maybe after we can extract this by connecting to the url
				error.put("emptyType", "Empty mandatory field Type");
				result.put("error", error);
				return badRequest(result);
			}
			
//	TESTEST!!			
			if(json.has("mediaRights")){
				Set<WithMediaRights> rightsSet = new HashSet<WithMediaRights>();
				JsonNode rightsArray = json.get("mediaRights");
				if(rightsArray.isArray()){
					for(JsonNode rightNode : rightsArray){
						for(WithMediaRights right: WithMediaRights.values()){
							if(StringUtils.equals(right.name().toLowerCase(), rightNode.asText())){
								rightsSet.add(right);
							}
						}
					}
					med.setWithRights(rightsSet);
				} else {
					error.put("JSON", "mediaRights field should be a Json Array");
					result.put("error", error);
					return badRequest(result);
				}
			} else {
				error.put("emptyType", "Empty mandatory field mediaRights");
				result.put("error", error);
				return badRequest(result);
			}

			
			//parse mimeType - this is not a mandatory field right?
			//maybe there can be a mismatch here with type,
			//mimeType will override it (good for clean data)
			if(json.has("mimeType")) {
				med.setMimeType(MediaType.parse(json.get("mimeType").asText()));
				if(med.getMimeType().is(MediaType.ANY_IMAGE_TYPE)){
					type = "image";
				} else if(med.getMimeType().is(MediaType.ANY_VIDEO_TYPE)){
					type = "video";
				} else if(med.getMimeType().is(MediaType.ANY_AUDIO_TYPE)){
					type = "audio";
				} else if(med.getMimeType().is(MediaType.ANY_TEXT_TYPE)){
					type = "text";
				} else{
					error.put("mimeType", "Unsupported or bad mimeType");
					result.put("error", error);
					return badRequest(result);
				}
			}
			
			//will eventually add all extended model fields
			if(type.toLowerCase().contains("image")){
				med.setType(WithMediaType.IMAGE);
								
				if(!parseJsonDimensionsAndThumbnail(med, json, error)){
					result.put("error", error);
					//return badRequest(result);
				} else {
					med.setOrientation();
				}
				
			} else if(type.toLowerCase().contains("video")){
				med.setType(WithMediaType.VIDEO);//durationSeconds //width, height //Quality	
				
				if(!parseJsonDimensionsAndThumbnail(med, json, error)){
					result.put("error", error);
					//return badRequest(result);
				} else {
					med.setOrientation();
				}
				
				if(!parseJsonDuration(med, json, error)){
					result.put("error", error);
					//return badRequest(result);
				}
				
				
			} else if(type.toLowerCase().contains("text")){
				med.setType(WithMediaType.TEXT);

			} else if(type.toLowerCase().contains("audio")){
				med.setType(WithMediaType.AUDIO); //Quality
				
				if(!parseJsonDuration(med, json, error)){
					result.put("error", error);
					return badRequest(result);
				}
				
			} else { 
				result.put("error", "Wrong or unsupported media type");
				return badRequest(result);
			}
		}
		return ok(result);
	}

	
	private static boolean parseJsonDuration(MediaObject med, JsonNode json, ObjectNode error) {
		if(json.has("durationSeconds")){
			if(json.get("durationSeconds").canConvertToInt()){
				med.setDurationSeconds(json.get("durationSeconds").asInt());
			} else {
				error.put("duration", "Duration needs to be an integer");
				return false;
			}
		}
		return true;
	}

	private static boolean parseJsonDimensionsAndThumbnail(MediaObject med, JsonNode json, ObjectNode error) {
		if(json.has("height")&&json.has("width")){
			if(json.get("height").canConvertToInt() && json.get("width").canConvertToInt()){
				med.setHeight(json.get("height").asInt());
				med.setWidth(json.get("width").asInt());
			} else {
				error.put("dimensions", "Height and width need to be integers");
				return false;
			}
		} else {
			if(json.has("height")||json.has("width")){
				error.put("dimensions", "You need to provide both height and width");
				return false;
			}
		}
		
		//make thumb if empty?
		if(json.has("thumbnail")){
			med.setThumbnailUrl(json.get("thumbnail").asText());
			
			if(json.has("thumbHeight")&&json.has("thumbWidth")){
				if(json.get("thumbHeight").canConvertToInt() && json.get("thumbWidth").canConvertToInt()){
					med.setThumbHeight(json.get("thumbHeight").asInt());
					med.setThumbWidth(json.get("thumbWidth").asInt());
				} else {
					error.put("thumbDimensions", "Thumbnail height and width need to be integers");
					return false;
				}
			} else
				if(json.has("height")||json.has("width")){
					error.put("thumbDimensions", "You need to provide both thumbnail height and width");
					return false;
				}
			}
		return true;
	}

	private static void imageUpload(ArrayNode allRes, ObjectNode singleRes, MediaObject med, FilePart fp,
			Map<String, String[]> formData) throws IOException {
		med.setType(WithMediaType.IMAGE);
		BufferedImage image = ImageIO.read(fp.getFile());
		int height = image.getHeight();
		int width = image.getWidth();
		med.setHeight(height);
		med.setWidth(width);
		
		//thumbnail
		
		if(!formData.containsKey("thumbnailUrl")){
			makeThumb(med, image);
		} else {
			//need a method to check if this is a valid url that contains an image! 
			med.setThumbnailUrl(formData.get("thumbnailUrl")[0]);
		}
		
//		Discuss quality enumeration!
//		long size = med.getSize();
//				
//		if(size<1){
//			med.setQuality(Quality.IMAGE_SMALL);
//		} else if(size<500){
//			med.setQuality(Quality.IMAGE_500k);
//		} else if(size<1000) {
//			med.setQuality(Quality.IMAGE_1);
//		} else {
//			med.setQuality(Quality.IMAGE_4);
//		}
		
	}


//	use the libraries we will use for video editing!
	private static void makeThumb(MediaObject med, BufferedImage image) throws IOException {
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
		
		med.setThumbWidth(thumb.getWidth());
		med.setThumbHeight(thumb.getHeight());
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
