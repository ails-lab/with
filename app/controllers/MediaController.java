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
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.imageio.ImageIO;

import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.Quality;
import model.EmbeddedMediaObject.WithMediaRights;
import model.EmbeddedMediaObject.WithMediaType;
import model.MediaObject;
import model.basicDataTypes.ResourceType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import sources.core.HttpConnector;
import sources.core.ParallelAPICall;
import utils.AccessManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.net.MediaType;
import java.io.FileInputStream;

import db.DB;

public class MediaController extends Controller {
	public static final ALogger log = Logger.of(MediaController.class);


	// Cache media based on url and media version
	public static Result getMediaByUrl(String url, String version) {
		MediaObject media;
		try {
			MediaVersion mediaVersion;
			if (MediaVersion.contains(version)) {
				mediaVersion = MediaVersion.valueOf(version);
			} else {
				mediaVersion = MediaVersion.Original;
			}
			media = DB.getMediaObjectDAO()
					.getByUrlAndVersion(url, mediaVersion);
			if (media != null) {
				return ok(media.getMediaBytes()).as(
						media.getMimeType().toString());
			}
			// Cache media
			downloadMedia(url, mediaVersion);
			return redirect(url);
		} catch (Exception e) {
			log.error("Cannot retrieve media document from database", e);
			return internalServerError("Cannot retrieve media document from database");
		}
	}

	private static void downloadMedia(String url, MediaVersion version){
		BiFunction<String, MediaVersion, Boolean> methodQuery = (
				String imageUrl, MediaVersion mediaVersion) -> {
			try {
				JsonNode parsed = Json.newObject();
				BufferedImage image;
				MediaObject media = new MediaObject();
				parsed = parseMediaURL(url);
				String g = "::" + HttpConnector.getURLContentAsFile(url);
				Logger.info(g);
				File img = HttpConnector.getURLContentAsFile(url);
				image = ImageIO.read(img);
				byte[] mediaBytes = IOUtils
						.toByteArray(new FileInputStream(img));
				editMediaAfterChecker(media, parsed);
				media.setUrl(url);
				media.setMediaBytes(mediaBytes);
				if (version != null) {
					media.setMediaVersion(version);
					DB.getMediaObjectDAO().makePermanent(media);
				} else {
					makeThumbs(media, image);
				}
				return true;
			} catch (Exception e) {
				log.error("Couldn't cache image:" + e.getMessage());
				return false;
			}

		};
		ParallelAPICall.createPromise(methodQuery, url, version);
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
	public static Result createMedia(boolean filedata) {
		
		List<String> userIds = AccessManager.effectiveUserIds(session().get(
				"effectiveUserIds"));
		// TODO: uncomment this after done testing
		// if (userIds.isEmpty())
		// return forbidden();
		
		
		ObjectNode result = Json.newObject();
		MediaObject med = new MediaObject();
		JsonNode parsed = Json.newObject();

		final Http.MultipartFormData multipartBody = request().body()
				.asMultipartFormData();
		
		final JsonNode jsonn = request().body().asJson();
		
		if (multipartBody != null) {
			try {
				
				Map<String, String[]> formData = multipartBody
						.asFormUrlEncoded();

				if (formData.containsKey("withMediaRights")) {
					String withMediaRights = formData.get("withMediaRights")[0];
					parseMediaRights(med, withMediaRights);
				} else {
					med.setWithRights(WithMediaRights.UNKNOWN);
				}

				if (!multipartBody.getFiles().isEmpty()) {
					FilePart fp = multipartBody.getFiles().get(0);
					//med.setMimeType(MediaType.parse(fp.getContentType()));
					// in KB!
					File x = fp.getFile();

					parsed = parseMediaFile(x, x.getName());
					editMediaAfterChecker(med, parsed);
					
					result = storeMedia(med, x);
					
				} else {
					result.put("error", "no image file");
					return badRequest(result);
				}

				return ok(result);

				
			} catch (Exception e) {
				// allRes.add(Json.newObject().put("error", "Couldn't create
				// from file "
				// + fp.getFilename()));
				log.error("Media create error", e);
				result.put("error", "Couldn't create from file or url");
				return badRequest(result);

			}

		
		} else if (jsonn != null) {

			
			if (jsonn.hasNonNull("withMediaRights")) {
				String withMediaRights = jsonn.get("withMediaRights").asText();
				parseMediaRights(med, withMediaRights);
			} else {
				med.setWithRights(WithMediaRights.UNKNOWN);
			}

			
			if (jsonn.hasNonNull("url")){
				parsed = parseMediaURL(jsonn.get("url").asText());
				Logger.info(parsed.toString());
				editMediaAfterChecker(med, parsed);
			} else {
				result.put("error", "must provide a url in json request");
				return badRequest(result);
			}
			
			
			try {

				// image = HttpConnector.getContent(formData.get("url")[0]);
				File x = HttpConnector.getURLContentAsFile(jsonn.get("url").asText());
				result = storeMedia(med, x);
				
				return ok(result);

				

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				result.put("error", "error creating from url");
				return badRequest(result);

			}
			
			
		
		} else {
			result.put("error", "MultiPart or Json body is null!");

			return badRequest(result);

		}
	}
	
	

	private static ObjectNode storeMedia(MediaObject med, File x) 
			throws IOException, Exception {
		
		ObjectNode result = Json.newObject();
		
		if (!x.isFile()) {
			result.put("error", "uploaded file is not valid");
			return result;
		}
		
		
		med.setSize(x.length() / 1024);
		BufferedImage image = ImageIO.read(x);

		
		if (med.getMimeType().is(MediaType.ANY_IMAGE_TYPE)) {

			med.setType(WithMediaType.IMAGE);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "jpg", baos);
			baos.flush();
			byte[] thumbByte = baos.toByteArray();
			baos.close();
			med.setMediaBytes(thumbByte);
			med.setDbId(null);
			DB.getMediaObjectDAO().makePermanent(med);
			med.setUrl("/media/" + med.getDbId().toString()
					+ "?file=true");
			DB.getMediaObjectDAO().makePermanent(med);

			result = makeThumbs(med, image);
			return result;

			// } else
			// if(med.getMimeType().is(MediaType.ANY_VIDEO_TYPE)){
			// med.setType(WithMediaType.VIDEO);//durationSeconds
			// //width, height //thumbnailBytes //Quality
			//
			// } else
			// if(med.getMimeType().is(MediaType.ANY_TEXT_TYPE)){
			// med.setType(WithMediaType.TEXT);
			//
			// } else
			// if(med.getMimeType().is(MediaType.ANY_AUDIO_TYPE)){
			// med.setType(WithMediaType.AUDIO); //durationSeconds
			// //Quality

		} else {
			result.put("error", "Unsupported media type");
			return result;
		}
	}

	
	
	
	
	private static boolean checkJsonArray(ArrayNode allRes, String string) {
		for (JsonNode x : allRes) {
			if (x.has(string)) {
				return true;
			}
		}
		return false;
	}

	// TODO: move this to deserializer ?Hex
	private static void parseMediaRights(MediaObject med, String rights) {

		med.setWithRights(WithMediaRights.UNKNOWN);

		for (WithMediaRights wmright : WithMediaRights.values()) {
			if (StringUtils.equals(wmright.name().toLowerCase(),
					rights.toLowerCase())) {
				med.setWithRights(wmright);
			}
		}

	}

	private static ResourceType parseOriginalRights(String resourceType) {
		for (ResourceType type : ResourceType.values()) {
			if (StringUtils.equals(type.name().toLowerCase(),
					resourceType.toLowerCase())) {
				return type;
			}
		}
		return null;
	}

	private static void editMediaAfterChecker(MediaObject med, JsonNode json) {

		MediaType mime = MediaType.parse(json.get("mimetype").asText()
				.toLowerCase());

		med.setMimeType(mime);

		med.setHeight(json.get("height").asInt());

		med.setWidth(json.get("width").asInt());

		// TODO: palette is component color?

		if (json.hasNonNull("palette")) {
			ArrayList<String> rights = new ArrayList<String>();
			JsonNode paletteArray = json.get("palette");
			if (paletteArray.isArray()) {
				for (JsonNode paletteNode : paletteArray) {
					rights.add(paletteNode.asText());
				}

			}
		}

		med.setColorSpace(json.get("colorspace").asText());

		med.setOrientation();

		// TODO : fix this naive quality enumeration, for now just for testing!
		long size = med.getSize();

		if (size < 100) {
			med.setQuality(Quality.IMAGE_SMALL);
		} else if (size < 500) {
			med.setQuality(Quality.IMAGE_500k);
		} else if (size < 1000) {
			med.setQuality(Quality.IMAGE_1);
		} else {
			med.setQuality(Quality.IMAGE_4);
		}

		med.setMediaVersion(MediaVersion.Original);

		med.setParentID("self");

	}

	// TODO:don't hate i will clean it!
	private static ObjectNode makeThumbs(MediaObject med, BufferedImage image) throws Exception {

		// makeThumb(med, image);
		MediaObject tiny = new MediaObject();
		MediaObject square = new MediaObject();
		MediaObject thumbnail = new MediaObject();
		MediaObject medium = new MediaObject();
		tiny.setDbId(null);
		square.setDbId(null);
		thumbnail.setDbId(null);
		medium.setDbId(null);

		if (med.getWidth() <= 100) {
			tiny.setMediaBytes(med.getMediaBytes());
			if (med.getHeight() < 150) {
				square = makeThumb(med, image, -1, 150, true);
			} else {
				square.setMediaBytes(med.getMediaBytes());
				square.setMimeType(med.getMimeType());
			}
			thumbnail.setMediaBytes(med.getMediaBytes());
			thumbnail.setMimeType(med.getMimeType());
			medium.setMediaBytes(med.getMediaBytes());
			medium.setMimeType(med.getMimeType());

		} else if (med.getWidth() <= 150 && med.getHeight() <= 150) {
			tiny = makeThumb(med, image, 100, -1, false);

			square.setMediaBytes(med.getMediaBytes());
			square.setMimeType(med.getMimeType());
			thumbnail.setMediaBytes(med.getMediaBytes());
			thumbnail.setMimeType(med.getMimeType());
			medium.setMediaBytes(med.getMediaBytes());
			medium.setMimeType(med.getMimeType());

		} else if (med.getWidth() <= 300) {
			tiny = makeThumb(med, image, 100, -1, false);
			if (med.getHeight() < med.getWidth()) {
				square = makeThumb(med, image, -1, 150, true);
			} else {
				square = makeThumb(med, image, 150, -1, true);
			}

			thumbnail.setMediaBytes(med.getMediaBytes());
			thumbnail.setMimeType(med.getMimeType());
			medium.setMediaBytes(med.getMediaBytes());
			medium.setMimeType(med.getMimeType());

		} else if (med.getWidth() <= 640) {
			tiny = makeThumb(med, image, 100, -1, false);
			if (med.getHeight() < med.getWidth()) {
				square = makeThumb(med, image, -1, 150, true);
			} else {
				square = makeThumb(med, image, 150, -1, true);
			}
			thumbnail = makeThumb(med, image, 300, -1, false);

			medium.setMediaBytes(med.getMediaBytes());
			medium.setMimeType(med.getMimeType());

		} else {
			tiny = makeThumb(med, image, 100, -1, false);
			if (med.getHeight() < med.getWidth()) {
				square = makeThumb(med, image, -1, 150, true);
			} else {
				square = makeThumb(med, image, 150, -1, true);
			}
			thumbnail = makeThumb(med, image, 300, -1, false);
			medium = makeThumb(med, image, 640, -1, false);
		}

		String dbid = med.getDbId().toString();
		tiny.setParentID(dbid);
		square.setParentID(dbid);
		thumbnail.setParentID(dbid);
		medium.setParentID(dbid);

		DB.getMediaObjectDAO().makePermanent(tiny);
		DB.getMediaObjectDAO().makePermanent(square);
		DB.getMediaObjectDAO().makePermanent(thumbnail);
		DB.getMediaObjectDAO().makePermanent(medium);

		tiny.setUrl("/media/" + tiny.getDbId().toString() + "?file=true");
		square.setUrl("/media/" + square.getDbId().toString() + "?file=true");
		thumbnail.setUrl("/media/" + thumbnail.getDbId().toString()
				+ "?file=true");
		medium.setUrl("/media/" + medium.getDbId().toString() + "?file=true");

		DB.getMediaObjectDAO().makePermanent(tiny);
		DB.getMediaObjectDAO().makePermanent(square);
		DB.getMediaObjectDAO().makePermanent(thumbnail);
		DB.getMediaObjectDAO().makePermanent(medium);

		// image is big
		// store original and resize for thumbnail
		// TODO:THIS
		// Media fullImage = media;
		// Resize image and put new width, height and bytes to data

		// DB.getMediaObjectDAO().makePermanent(med);

		ObjectNode results = Json.newObject();

		results.put("original", med.getUrl());
		results.put("tiny", tiny.getUrl());
		results.put("square", square.getUrl());
		results.put("thumbnail", thumbnail.getUrl());
		results.put("medium", medium.getUrl());

		// singleRes.put("isShownBy", "/media/"
		// + med.getDbId().toString());

		// Logger.info(results.asText());
		return results;
	}

	// use the libraries we will use for video editing!
	// TODO
	private static MediaObject makeThumb(MediaObject med, BufferedImage image,
			int width, int height, boolean crop) throws IOException {

		// 211, -1
		Image ithumb = image.getScaledInstance(width, height,
				Image.SCALE_SMOOTH);
		// Create a buffered image with transparency
		BufferedImage thumb = new BufferedImage(ithumb.getWidth(null),
				ithumb.getHeight(null), image.getType());

		// Logger.info("Width: " + thumb.getWidth() + ", x = "+
		// (((thumb.getWidth() - 150)/2)-1));

		// BufferedImage thumb2 = null;

		// if(crop){
		// if(thumb.getHeight()>150){
		// int y = ((thumb.getHeight() - 150)/2)-1;
		// thumb2 = thumb.getSubimage(0, y, 150, 150);

		// }else if(thumb.getWidth()>150){
		// int x = ((thumb.getWidth() - 150)/2)-1;
		// thumb2 = thumb.getSubimage(x, 0, 150, 150);
		// }

		// thumb = thumb2;
		// }

		// Draw the image on to the buffered image
		Graphics2D bGr = thumb.createGraphics();
		bGr.drawImage(ithumb, 0, 0, null);
		bGr.dispose();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(thumb, "jpg", baos);
		baos.flush();
		byte[] thumbByte = baos.toByteArray();
		baos.close();

		MediaObject mthumb = new MediaObject();
		mthumb.setMimeType(med.getMimeType());
		mthumb.setDbId(null);
		mthumb.setMediaBytes(thumbByte);
		mthumb.setWidth(ithumb.getWidth(null));
		mthumb.setHeight(ithumb.getHeight(null));

		return mthumb;
	}

	private static JsonNode parseMediaFile(File fileToParse, String fileName) {
		Logger.info("filename: " + fileName);

		// HttpClient hc = new DefaultHttpClient();

		CloseableHttpClient hc = HttpClients.createDefault();

		JsonNode resp = Json.newObject();
		try {

			HttpPost aFile = new HttpPost(
					"http://mediachecker.image.ntua.gr/api/extractmetadata");
			// File testFile = fileToParse;
			FileBody fileBody = new FileBody(fileToParse);
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			// builder.addBinaryBody("mediafile", testFile,
			// ContentType.create("image/jpeg"), fileName);
			builder.addPart("mediafile", fileBody);
			aFile.setEntity(builder.build());
			CloseableHttpResponse response = hc.execute(aFile);
			String jsonResponse = EntityUtils.toString(response.getEntity(),
					"UTF8");
			// String id =
			// JsonPath.parse(jsonResponse).read("$['results'][0]['mediaId']");
			resp = Json.parse(jsonResponse);

			Logger.info(jsonResponse);

			Logger.info("Called!");
			aFile.releaseConnection();

			response.close();
			hc.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return resp;

	}

	private static JsonNode parseMediaURL(String mediaURL) {

		String url = "http://mediachecker.image.ntua.gr/api/extractmetadata";
		
		//String queryURL = "http://mediachecker.image.ntua.gr/api/extractmetadata?url="
		//		+ mediaURL;
		// Logger.info("URL: " + queryURL);
		JsonNode response = null;
		try {		
			response = HttpConnector.postURLContent(url, mediaURL, "url");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return response;
	}
	
	
	
	//EDIT, DELETE, GET
	
	
	
	
	

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
			// TODO: Implement...
			return ok(Json.newObject().put("message", "not implemeted yet!"));
		} else {
			MediaObject newMedia = null;
			ArrayNode allRes = result.arrayNode();

			try {
				newMedia = DB.getMediaObjectDAO().findById(new ObjectId(id));

				// set metadata
				// if (json.has("URL"))
				// allRes.addAll(parseURLFromJson(newMedia, json));
				// if (json.has("mediaRights"))
				// allRes.addAll(parseMediaRightsFromJson(newMedia, json));

				if (checkJsonArray(allRes, "error")) {
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
			// confirm this is right! .as Changes the Content-Type header for
			// this result.
			// Logger.info(media.getMimeType().toString());
			return ok(media.getMediaBytes()).as(media.getMimeType().toString());
		} else {
			// Logger.info(media.getMimeType().toString());
			JsonNode result = Json.toJson(media);
			return ok(result);
		}

	}

	
	//CODE FROM PARSE MEDIA FILE
	
	
	// TODO: fix exception (add allrez)

	// Logger.info("mediafile, " + "file://"+fileToParse.getName()+ ", " +
	// fileToParse);

	//
	// HttpClient hc = new DefaultHttpClient();
	//
	// JsonNode resp = Json.newObject();
	// try {
	//
	// HttpPost aFile = new
	// HttpPost("http://mediachecker.image.ntua.gr/api/extractmetadata");
	// File testFile = fileToParse;
	// MultipartEntityBuilder builder = MultipartEntityBuilder.create();
	// builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
	// builder.addBinaryBody("mediafile", testFile,
	// ContentType.create("image/jpeg"), fileName);
	// aFile.setEntity(builder.build());
	// HttpResponse response = hc.execute(aFile);
	// String jsonResponse = EntityUtils.toString(response.getEntity(),
	// "UTF8");
	// //String id =
	// JsonPath.parse(jsonResponse).read("$['results'][0]['mediaId']");
	// aFile.releaseConnection();
	// resp = Json.parse(jsonResponse);
	//
	// Logger.info(jsonResponse);
	//
	// Logger.info(resp.asText());
	//
	// } catch (Exception e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	//
	//

	//
	// String queryURL =
	// "http://mediachecker.image.ntua.gr/api/extractmetadata";
	// // Logger.info("URL: " + queryURL);
	// JsonNode response = null;
	// try {
	// // response = sources.core.HttpConnector.postFile(queryURL,
	// // fileToParse, "mediafile",
	// // "file://"+fileToParse.getAbsolutePath());
	// response = sources.core.HttpConnector.postMultiPartFormData(queryURL,
	// fileToParse, "mediafile",
	// fileToParse.getName());
	//
	// } catch (Exception e) {
	// // TODO Auto-generated catch block
	// // e.printStackTrace();
	// }

	// //String filename = fileToParse.getName();
	//
	// ObjectNode node = Json.newObject();
	//
	// node.put("mediafile", "file://"+fileToParse.getAbsolutePath());
	//
	//
	// String queryURL =
	// "http://mediachecker.image.ntua.gr/api/extractmetadata";
	// Logger.info("URL: " + queryURL);
	// Logger.info("filename: " + fileToParse.getAbsolutePath());
	// Logger.info("filename: " + fileToParse.getPath());
	// Logger.info("filename: " + fileToParse.toPath());
	//
	//
	// JsonNode response = null;
	//
	// try {
	//

	// response = sources.core.HttpConnector.postJson(queryURL, node);
	//
	// } catch (Exception e) {
	// // TODO Auto-generated catch block
	// // e.printStackTrace();
	// }

	// Logger.info("response: " + response);

	
	
	
	/*
	 * //TODO: can this come in a different serialization from the
	 * frontend?
	 * 
	 * if(formData.containsKey("resourceType")){
	 * if(formData.containsKey("uri")){ ResourceType type =
	 * parseOriginalRights(formData.get("resourceType")[0]); if
	 * (type==null){ LiteralOrResource lit = new
	 * LiteralOrResource(); lit.add(type, formData.get("uri")[0]);
	 * med.setOriginalRights(lit); } else {
	 * allRes.add(Json.newObject().put("error",
	 * "Bad resource type")); } } }
	 */

	
	
}
