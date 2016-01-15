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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.Quality;
import model.EmbeddedMediaObject.WithMediaRights;
import model.EmbeddedMediaObject.WithMediaType;
import model.ExternalBasicRecord.ItemRights;
import model.Media;
import model.MediaObject;
import model.basicDataTypes.KeySingleValuePair.LiteralOrResource;
import model.basicDataTypes.ResourceType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
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
import sources.core.HttpConnector;

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
			//TODO: Implement...
			return ok(Json.newObject().put("message", "not implemeted yet!"));
		} else {
			MediaObject newMedia = null;
			ArrayNode allRes = result.arrayNode();

			try {
				newMedia = DB.getMediaObjectDAO().findById(new ObjectId(id));

				// set metadata
				if (json.has("URL"))
					allRes.addAll(parseURLFromJson(newMedia, json));
				if (json.has("mediaRights"))
					allRes.addAll(parseMediaRightsFromJson(newMedia, json));

				//TODO: finish investigating if an unparsable mimeType sets existing mimeType to null
				//	and also wherever else something like this occurs
				if (json.has("type")&&json.has("mimeType")){
					allRes.addAll(parseTypeMimeTypeFromJson(newMedia, json));
				} else if(json.has("mimeType")){
					ObjectNode temp = (ObjectNode) json;
					temp.put("type", newMedia.getType().name());
					//will overwrite type if there is a mismatch from saved
					allRes.addAll(parseTypeMimeTypeFromJson(newMedia, temp));
				} else if(json.has("type")){
					//will (should...) not change mimeType
					allRes.addAll(parseTypeMimeTypeFromJson(newMedia, json));
				}
				if (json.has("originalRights"))
					allRes.addAll(parseOriginalRightsFromJson(newMedia, json));

				//TODO:fix issue with thumbnail
				allRes.addAll(parseExtendedJson(newMedia, json));

				if(checkJsonArray(allRes,"error")){
					result.put("errors found", allRes);
					return badRequest(result);
				}
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
		//TODO: uncomment this after done testing
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
						
						JsonNode parsed = Json.newObject();
						
						if(formData.containsKey("url")){
							//change this policy?
							//allRes.add(Json.newObject().put("warn", "External url is ignored when uploading files"));
							Logger.info("IT CONTAINSWTF: " +formData.get("url")[0]);
							parsed = parseMediaURL(formData.get("url")[0]);
						} else {
							parsed = parseMediaFile(fp.getFile(), fp.getFilename());
						}
						
						Logger.info("mediacheckreply: " + parsed);
						
						parseMediaCheckerReply(med, parsed);

						//TODO: move these to editMedia()
						if(formData.containsKey("withMediaRights")){
							String[] withMediaRights = formData.get("withMediaRights");
							ArrayList<String> rights = new ArrayList<String>(Arrays.asList(withMediaRights));
							parseMediaRights(med, rights);
						} else {
							allRes.add(Json.newObject().put("error", "Empty mandatory field mediaRights"));
						}

/*
						//TODO: can this come in a different serialization from the frontend?
  						
						if(formData.containsKey("resourceType")){
							if(formData.containsKey("uri")){
								ResourceType type = parseOriginalRights(formData.get("resourceType")[0]);
								if (type==null){
									LiteralOrResource lit = new LiteralOrResource();
									lit.add(type, formData.get("uri")[0]);
									med.setOriginalRights(lit);
								} else {
									allRes.add(Json.newObject().put("error", "Bad resource type"));
								}
							}
						}
*/					
						
						Logger.info(parsed.asText());
						//TODO: fix errors (see parseExtended)
						//allRes.addAll(parseExtendedJson(med, parsed));
						if(checkJsonArray(allRes,"error")){
							result.put("errors found", allRes);
							return badRequest(result);
						} 
						if(med.getMimeType().is(MediaType.ANY_IMAGE_TYPE)){

							//we don't parse type here since media type will override it anyway
							//if we do decide however, remember to check for mismatch
							med.setType(WithMediaType.IMAGE);
							
							med.setMediaBytes(FileUtils.readFileToByteArray(fp.getFile()));
							med.setDbId(null);
							DB.getMediaObjectDAO().makePermanent(med);

							imageUpload(med, fp, formData);

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
							//allRes.add(Json.newObject().put("error", "Unsupported media type "
							//		+ fp.getFilename()));
							//log.error("Media create error", "Unsupported media type");
						}

						//if(checkJsonArray(allRes,"error")){
						//	result.put("errors found", allRes);
						//	return badRequest(result);
						//}


//						singleRes.put("isShownBy", "/media/"
//								+ med.getDbId().toString());
//						singleRes.put("externalId", med.getDbId().toString());
//						allRes.add(singleRes);
					} catch (Exception e) {
						allRes.add(Json.newObject().put("error", "Couldn't create from file "
								+ fp.getFilename()));
						log.error("Media create error", e);
						result.put("errors found", allRes);
						return badRequest(result);

					}
				}
				//result.put("results", allRes);
			}
//			} else {
//				final Map<String, String[]> req = request().body()
//						.asFormUrlEncoded();
//				if (req != null) {
//				//	this means we have form data but no file, do we even allow this??
//				//	don't we want to force json in this case?
//					// this should be rare for file data
//				} else {
//					final JsonNode jsonBody = request().body().asJson();
//					if (jsonBody != null) {
//					//	then why do we even need the boolean parameter???
//						// we extract the media and maybe some metadata from the
//						// json body
//
//					} else {
//					//	again why should we even allow this?
//						// raw body to file upload
//						// problem, there is absolutely no metadata, so don't
//						// know what to put in the Media Object
//						try {
//							med = new MediaObject();
//							med.setMediaBytes(request().body().asRaw().asBytes());
//							DB.getMediaObjectDAO().makePermanent(med);
//							result.put("Success", "Media object created!");
//							result.put("mediaId", med.getDbId().toString());
//						} catch (Exception e) {
//							result.put("error", "Couldn't create Media object");
//							log.error("Media create error", e);
//						}
//					}
//				}
//			}
		} else {
			// metadata based media creation
			//TODO: Use Enrique's code to better parse the json?

			//TODO: Find a way around all these validations! use @notnull if possible for complex checks
			//	abandoned this for now

			JsonNode json = null;
			json = request().body().asJson();

			//have two methods here instead of one because I use them in edit() as well
			allRes.addAll(parseEmbeddedJson(med, json));
			if(checkJsonArray(allRes,"error")){
				result.put("errors found", allRes);
				return badRequest(result);
			}
			
			allRes.addAll(parseExtendedJson(med, json));
			
			if(checkJsonArray(allRes,"error")){
				result.put("errors found", allRes);
				return badRequest(result);
			}
			
			med.setDbId(null);



			//TODO: this is temporary for testing, need to alter DAO
			//	fix and then delete this
//			File file = new File("testfile");
//			try {
//				FileUtils.copyURLToFile(new URL(med.getUrl()), file);
//				FileInputStream fileStream = new FileInputStream(
//						file);
//				med.setMediaBytes(IOUtils.toByteArray(fileStream));
//			} catch (MalformedURLException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			} catch (IOException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}



			try {
				DB.getMediaObjectDAO().makePermanent(med);
			} catch (Exception e) {
				allRes.add(Json.newObject().put("error", "Couldn't create from file "));
				log.error("Media create error", e);
				result.put("errors found", allRes);
				return badRequest(result);
			}

		}
		
		//plz don't hate
		//if(allRes.get(0).asText()=="null"){
		//	allRes.remove(0);
		//}

		
		singleRes.put("isShownBy", "/media/"
				+ med.getDbId().toString());
		singleRes.put("externalId", med.getDbId().toString());
		allRes.add(singleRes);
		result.put("results", allRes);

		result.put("Success", "Media object created!");
		return ok(result);
	}

	private static boolean checkJsonArray(ArrayNode allRes, String string){
		for(JsonNode x:allRes){
			if(x.has(string)){return true;}
		}
		return false;
	}

	private static ArrayNode parseEmbeddedJson(MediaObject med, JsonNode json) {
		ArrayNode allRes = Json.newObject().arrayNode();

		if(json.isNull()){
			allRes.add(Json.newObject().put("error", "Empty Json Body (file parameter is false)"));
			return allRes;
		}
		
		allRes.addAll(parseURLFromJson(med, json));
		allRes.addAll(parseMediaRightsFromJson(med, json));
		allRes.addAll(parseTypeMimeTypeFromJson(med, json));
		allRes.addAll(parseOriginalRightsFromJson(med, json));
		return allRes;
	}

	private static ArrayNode parseOriginalRightsFromJson(MediaObject med, JsonNode json) {
		ArrayNode allRes = Json.newObject().arrayNode();
		//TODO: ask if this is how this will arrive from the frontend
		if(json.hasNonNull("originalRights")){
			JsonNode rights = json.get("originalRights");
			if(rights.hasNonNull("resourceType")){
				if(rights.hasNonNull("uri")){
					ResourceType resType = parseOriginalRights(rights.get("resourceType").asText());
					if (resType==null){
						LiteralOrResource lit = new LiteralOrResource();
						lit.add(resType, rights.get("uri").asText());
						med.setOriginalRights(lit);
					} else {
						allRes.add(Json.newObject().put("error", "Bad resource type"));
					}
				}
			}
		}
		return allRes;
	}

	private static ArrayNode parseTypeMimeTypeFromJson(MediaObject med, JsonNode json) {

		ArrayNode allRes = Json.newObject().arrayNode();

		//parse mimeType - this is not a mandatory field right?
		//maybe there can be a mismatch here with type,
		//mimeType will override it (good for clean data)
		//however, if mimeType is empty, type can still be valid!

		WithMediaType wmtype = null;
		//First check for mimeType
		if(json.hasNonNull("mimeType")) {
			try{
				med.setMimeType(MediaType.parse(json.get("mimeType").asText().toUpperCase()));
				MediaType mime = med.getMimeType();
				//is it correct and one of four super types?
				wmtype = parseMimeType(mime, wmtype);
				if(wmtype==null){
					allRes.add(Json.newObject().put("warn", "Unsupported mimeType!"));
					//.zip acts as a flag here
					med.setMimeType(MediaType.ZIP);
				}
			} catch(Exception e){
				allRes.add(Json.newObject().put("warn", "Could not parse mimeType!"));
				med.setMimeType(MediaType.ZIP);
			}

			//Check if it also has withType
			if(json.hasNonNull("type")){
				//Is it a valid type?
				if(!parseType(json.get("type").asText(), med)){

					//Are both non valid?
					if(med.getMimeType()==MediaType.ZIP){
						allRes.add(Json.newObject().put("error", "Could not parse type and mimeType"));
						return allRes;
					}
					//Bad type, good mimeType
					allRes.add(Json.newObject().put("warn", "Could not parse field type, it has been inferred from mimeType"));
					med.setType(wmtype);
				}else {
					//Now check for a mismatch (no reason to check if type has been set from mimeType)
					if(typeMismatch(med.getMimeType(), med.getType())){
						//TODO: do this or just ignore mimeType?
						allRes.add(Json.newObject().put("warn", "mimeType and type mismatch, setting type from mimeType"));
					}
				}
			}

		//No mimeType, just check for type
		} else if(json.hasNonNull("type")){
				//Is it a valid type?
				if(!parseType(json.get("type").asText(), med)){
					allRes.add(Json.newObject().put("error", "Could not parse type field."));
					return allRes;
				}

		} else {
				allRes.add(Json.newObject().put("error", "You must provide a valid type or mimeType field."));
				return allRes;

		} //TODO: else : extract mimeType from media url

		return allRes;

	}

	private static ArrayNode parseMediaRightsFromJson(MediaObject med, JsonNode json) {
		ArrayNode allRes = Json.newObject().arrayNode();


		//parse mediaRights - mandatory field!
		if(json.hasNonNull("mediaRights")){
			ArrayList<String> rights = new ArrayList<String>();
			JsonNode rightsArray = json.get("mediaRights");
			if(rightsArray.isArray()){
				for(JsonNode rightNode : rightsArray){
					rights.add(rightNode.asText());
				}
			} else {
				allRes.add(Json.newObject().put("error", "mediaRights field should be a Json Array"));
				return allRes;
			}
			parseMediaRights(med, rights);
		} else {
			allRes.add(Json.newObject().put("error", "Empty mandatory field mediaRights"));
			return allRes;
		}
		return allRes;

	}

	private static ArrayNode parseURLFromJson(MediaObject med, JsonNode json) {
		ArrayNode allRes = Json.newObject().arrayNode();

		if(json.hasNonNull("url")){
			med.setUrl(json.get("url").asText());
		} else {
			allRes.add(Json.newObject().put("error", "Empty url for the media object"));
			return allRes;
		}

		return allRes;
	}


	//TODO: move this to deserializer ?
	private static void parseMediaRights(MediaObject med, ArrayList<String> rights){
		Set<WithMediaRights> rightsSet = new HashSet<WithMediaRights>();
			for(String right : rights){
				for(WithMediaRights wmright: WithMediaRights.values()){
					if(StringUtils.equals(wmright.name().toLowerCase(), right.toLowerCase())){
						rightsSet.add(wmright);
					}
				}
			}
		med.setWithRights(rightsSet);
	}

	private static ResourceType parseOriginalRights(String resourceType){
		for(ResourceType type: ResourceType.values()){
			if(StringUtils.equals(type.name().toLowerCase(), resourceType.toLowerCase())){
				return type;
			}
		}
		return null;
	}


	//Type is a mandatory field only if mimeType is not provided. If both are provided however,
	//we need to warn in case of a type mismatch (we can infer the media type from mimeType)
	//Also, we might want to check the actual media url in the future in order to parse stuff
	//	if (json.has("type"))
	//	newMedia.setType(Media.BaseType.valueOf(json.get("type")
	//	.asText()));

	// this isn't a serialization issue it's a check...
	private static WithMediaType parseMimeType(MediaType mime, WithMediaType type) {
		if(mime.is(MediaType.ANY_IMAGE_TYPE)){
			type = WithMediaType.IMAGE;
		} else if(mime.is(MediaType.ANY_VIDEO_TYPE)){
			type = WithMediaType.VIDEO;
		} else if(mime.is(MediaType.ANY_AUDIO_TYPE)){
			type = WithMediaType.AUDIO;
		} else if(mime.is(MediaType.ANY_TEXT_TYPE)){
			type = WithMediaType.TEXT;
		} else{
			return null;
		}
		return type;
	}

	//TODO: serializer/deserializer?
	private static boolean parseType(String type, MediaObject med) {
		if(type.toLowerCase().contains("image")){
			med.setType(WithMediaType.IMAGE);
		} else if(type.toLowerCase().contains("video")){
			med.setType(WithMediaType.VIDEO);
		} else if(type.toLowerCase().contains("audio")){
			med.setType(WithMediaType.AUDIO);
		} else if(type.toLowerCase().contains("text")){
			med.setType(WithMediaType.TEXT);
		} else{
			return false;
		}
		return true;
	}

	private static boolean typeMismatch(MediaType mime, WithMediaType with){
		if( (mime.is(MediaType.ANY_IMAGE_TYPE) && with.name().toLowerCase().contains("image")) ||
				(mime.is(MediaType.ANY_VIDEO_TYPE) && with.name().toLowerCase().contains("video")) ||
				(mime.is(MediaType.ANY_AUDIO_TYPE) && with.name().toLowerCase().contains("audio")) ||
				(mime.is(MediaType.ANY_TEXT_TYPE) && with.name().toLowerCase().contains("text")) ){
			return false;
		} else {
			return true;
		}
	}

	private static ArrayNode parseExtendedJson(MediaObject med, JsonNode json) {
		ArrayNode allRes = Json.newObject().arrayNode();
		//TODO: make a method that checks for conflicts with the external media checker!
		if(med.getType()==WithMediaType.IMAGE){
			parseImageFromJson(med, json, allRes);
		} else if(med.getType()==WithMediaType.VIDEO){
			parseVideoFromJson(med, json, allRes);
		} else if(med.getType()==WithMediaType.TEXT){

		} else if(med.getType()==WithMediaType.AUDIO){
			 parseAudioFromJson(med, json, allRes);
		} else {
			allRes.add(Json.newObject().put("error", "Wrong or unsupported media type"));
			return allRes;
		}
		//node.put("media", Json.toJson(med));
		allRes.add(Json.newObject().put("media", Json.toJson(med)));
		return allRes;
	}

	private static void parseAudioFromJson(MediaObject med, JsonNode json, ArrayNode allRes) {
		//Quality
		parseJsonDuration(med, json, allRes);
		
		if(json.hasNonNull("channels")){
			if(json.get("channels").canConvertToInt()){
				med.setAudioChannelNumber(json.get("channels").asInt());
			} else {
				allRes.add(Json.newObject().put("error", "Audio channels number needs to be an integer"));
			}
		}
		
		parseJsonBitRate(med, json, allRes);
		
		if(json.hasNonNull("samplerate")){
			if(json.get("samplerate").canConvertToInt()){
				med.setSampleRate(json.get("samplerate").asInt());
			} else {
				allRes.add(Json.newObject().put("error", "Sample rate needs to be an integer"));
			}
		}
		
		
		//bitdepth, fileformat? - image format?, palette
		
		//missing: componentColor, sampleSize
		
		// thumbnails from videos!
		
	}


	private static void parseVideoFromJson(MediaObject med, JsonNode json, ArrayNode allRes) {
		//durationSeconds //width, height //Quality

		if(parseJsonDimensionsAndThumbnail(med, json, allRes)){
			med.setOrientation();
		}

		parseJsonDuration(med, json, allRes);
		
		parseJsonBitRate(med, json, allRes);

		
		//TODO: reminder that this should be an Enum!
		if(json.hasNonNull("codec")){
			med.setCodec(json.get("codec").asText());
		}
		
		if(json.hasNonNull("framerate")){
			if(json.get("framerate").canConvertToInt()){
				med.setFrameRate(json.get("framerate").asInt());
			} else {
				allRes.add(Json.newObject().put("error", "Frame rate needs to be an integer"));
			}
		}
	}

	private static void parseImageFromJson(MediaObject med, JsonNode json, ArrayNode allRes) {
		if(parseJsonDimensionsAndThumbnail(med, json, allRes)){
			med.setOrientation();
		}
		
		//TODO: reminder that this should be an Enum!
		if(json.hasNonNull("colorspace")){
			med.setColorSpace(json.get("colorspace").asText());
		}
		
		
		
	}
	
	private static void parseJsonBitRate(MediaObject med, JsonNode json, ArrayNode allRes) {
		if(json.hasNonNull("bitrate")){
			if(json.get("bitrate").canConvertToInt()){
				med.setBitRate(json.get("bitrate").asInt());
			} else {
				allRes.add(Json.newObject().put("error", "Bit rate needs to be an integer"));
			}
		}
	}


	//these methods are boolean and not arraynodes for flow control during testing
	//with the media libraries we are going to use in the future
	private static boolean parseJsonDuration(MediaObject med, JsonNode json, ArrayNode allRes) {
		if(json.hasNonNull("durationSeconds")){
			if(json.get("durationSeconds").canConvertToInt()){
				med.setDurationSeconds(json.get("durationSeconds").asInt());
			} else {
				allRes.add(Json.newObject().put("error", "Duration needs to be an integer (seconds)"));
				return false;
			}
		}
		return true;
	}
	
	
	private static boolean parseJsonDimensionsAndThumbnail(MediaObject med, JsonNode json, ArrayNode allRes) {

		if(json.hasNonNull("height")&&json.hasNonNull("width")){
			if(json.get("height").canConvertToInt() && json.get("width").canConvertToInt()){
				med.setHeight(json.get("height").asInt());
				med.setWidth(json.get("width").asInt());
			} else {
				allRes.add(Json.newObject().put("error", "Height and width need to be integers (pixels)"));
				return false;
			}
		} else {
			if(json.hasNonNull("height")||json.hasNonNull("width")){
				allRes.add(Json.newObject().put("error", "You need to provide both height and width"));
				return false;
			}
		}
	
		//TODO:make thumb from image url if this is empty?
		if(json.hasNonNull("thumbnailUrl")){
			//med.setThumbnailUrl(json.get("thumbnailUrl").asText());

			if(json.hasNonNull("thumbHeight")&&json.hasNonNull("thumbWidth")){
				if(json.get("thumbHeight").canConvertToInt() && json.get("thumbWidth").canConvertToInt()){
					//med.setThumbHeight(json.get("thumbHeight").asInt());
					//med.setThumbWidth(json.get("thumbWidth").asInt());
				} else {
					allRes.add(Json.newObject().put("error", "Thumbnail height and width need to be integers"));
					return false;
				}
			} else if(json.hasNonNull("thumbHeight")||json.hasNonNull("thumbWidth")){
				allRes.add(Json.newObject().put("error", "You need to provide both thumbnail height and width"));
					return false;
			} //else parse thumb url to get the values

			}
		return true;
	}


	private static void parseMediaCheckerReply(MediaObject med, JsonNode json) {
		
		MediaType mime = MediaType.parse(json.get("mimetype").asText().toUpperCase());
		
		med.setMimeType(mime);
		
		med.setHeight(json.get("height").asInt());
		
		med.setWidth(json.get("width").asInt());
		
		
		//TODO: palette is component color?
		
		if(json.hasNonNull("palette")){
			ArrayList<String> rights = new ArrayList<String>();
			JsonNode paletteArray = json.get("palette");
			if(paletteArray.isArray()){
				for(JsonNode paletteNode : paletteArray){
					rights.add(paletteNode.asText());
				}
			}
		}
		
		med.setColorSpace(json.get("colorspace").asText());
		
		med.setOrientation();

//		TODO : fix this naive quality enumeration, for now just for testing!
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

		med.setMediaVersion(MediaVersion.Original);
		
	}

	private static JsonNode imageUpload(MediaObject med, FilePart fp, Map<String, String[]> formData)
			throws Exception {

		BufferedImage image = ImageIO.read(fp.getFile());


		//makeThumb(med, image);
		
		MediaObject tiny = new MediaObject();
		MediaObject square = new MediaObject();
		MediaObject thumbnail = new MediaObject();
		MediaObject medium = new MediaObject();
			
		if (med.getWidth() <= 100) {
			tiny = med;
			tiny.setDbId(null);
			square = med;
			thumbnail = med;
			medium = med;


			DB.getMediaObjectDAO().makePermanent(med);

			DB.getMediaObjectDAO().makePermanent(med);

			DB.getMediaObjectDAO().makePermanent(med);

			DB.getMediaObjectDAO().makePermanent(med);

		} else if(med.getWidth()<=150&&med.getHeight()<=150){
			square = med;
			thumbnail = med;
			medium = med;


			DB.getMediaObjectDAO().makePermanent(med);

			DB.getMediaObjectDAO().makePermanent(med);

			DB.getMediaObjectDAO().makePermanent(med);

		} else if(med.getWidth()<=300){
			thumbnail = med;
			medium = med;

			DB.getMediaObjectDAO().makePermanent(med);
			DB.getMediaObjectDAO().makePermanent(med);

		} else if(med.getWidth()<=640){
			medium = med;
			DB.getMediaObjectDAO().makePermanent(med);

		} else {
			tiny = makeThumb(med, image, 100, -1, false);
			if(med.getHeight()<med.getWidth()){
				square = makeThumb(med, image, -1, 150, true);
			} else{
				square = makeThumb(med, image, 150, -1, true);
			}
			
		}
		
		// image is big
		// store original and resize for thumbnail
		//TODO:THIS
		//Media fullImage = media;
		// Resize image and put new width, height and bytes to data

		DB.getMediaObjectDAO().makePermanent(med);
		
		ObjectNode results = Json.newObject();
		
		results.put("original", med.getDbId().toString());
		results.put("tiny", tiny.getDbId().toString());
		results.put("square", square.getDbId().toString());
		results.put("thumbnail", thumbnail.getDbId().toString());
		results.put("medium", medium.getDbId().toString());
		
//		singleRes.put("isShownBy", "/media/"
//				+ med.getDbId().toString());
		
		return results;
	}


//	use the libraries we will use for video editing!
	//TODO
	private static MediaObject makeThumb(MediaObject med, BufferedImage image, 
			int width, int height, boolean crop) throws IOException {
		
		MediaObject thumb = new MediaObject();
		
		//211, -1
		Image ithumb = image.getScaledInstance(width, height,
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

		med.setMediaBytes(thumbByte);
		med.setThumbWidth(thumb.getWidth());
		med.setThumbHeight(thumb.getHeight());
	}

	private static JsonNode parseMediaFile(File fileToParse, String fileName) {
		//TODO: fix exception (add allrez)
		
		Logger.info("mediafile, " + "file://"+fileToParse.getName()+ ", " + fileToParse);
		
		String queryURL = "http://mediachecker.image.ntua.gr/api/extractmetadata";
		Logger.info("URL: " + queryURL);
		JsonNode response = null;
		try {
			//response = sources.core.HttpConnector.postFile(queryURL, fileToParse, "mediafile", "file://"+fileToParse.getAbsolutePath());
			response = sources.core.HttpConnector.postMultiPartFormData(queryURL, fileToParse, 
					"mediafile", fileToParse.getName());
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
		
		
//		//String filename = fileToParse.getName();
//		
//		ObjectNode node = Json.newObject();
//		
//		node.put("mediafile", "file://"+fileToParse.getAbsolutePath());
//		
//		
//		String queryURL = "http://mediachecker.image.ntua.gr/api/extractmetadata";
//		Logger.info("URL: " + queryURL);
//		Logger.info("filename: " + fileToParse.getAbsolutePath());
//		Logger.info("filename: " + fileToParse.getPath());
//		Logger.info("filename: " + fileToParse.toPath());
//		
//		
//		JsonNode response = null;
//				
//		try {
//			
//			response = sources.core.HttpConnector.postJson(queryURL, node);
//
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			// e.printStackTrace();
//		}
		
		
		Logger.info("response: " + response);
		
		
		
		return response;
	}

	private static JsonNode parseMediaURL(String mediaURL) {
		//TODO: fix exception (add allrez)
			
		String queryURL = "http://mediachecker.image.ntua.gr/api/extractmetadata?url=" + mediaURL;
		Logger.info("URL: " + queryURL);
		JsonNode response = null;
				
		try {
			
			response = HttpConnector.postURLContent(queryURL);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}

		
		return response;
	}
}
