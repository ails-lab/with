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
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bson.types.ObjectId;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.net.MediaType;

import actors.MediaCheckerActor.MediaCheckMessage;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import db.DB;
import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.WithMediaRights;
import model.EmbeddedMediaObject.WithMediaType;
import model.MediaObject;
import model.basicDataTypes.Language;
import model.resources.RecordResource;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Akka;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import sources.core.ApacheHttpConnector;
import sources.core.ApacheHttpConnector.FileAndType;
import sources.core.HttpConnector;
import sources.core.ParallelAPICall;
import utils.MetricsUtils;

public class MediaController extends WithController {
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
			media = DB.getMediaObjectDAO().getByUrlAndVersion(url, mediaVersion);
			if (media != null) {
				response().setHeader(Http.HeaderNames.CACHE_CONTROL, "PUBLIC, max-age=" + 86400);
				response().setHeader(Http.HeaderNames.EXPIRES,
						DateUtils.formatDate(new Date(System.currentTimeMillis() + 86400000)));
				// WOuld like to set
				// response().setHeader(Http.HeaderNames.LAST_MODIFIED,
				// but there is now store date on the MediaObject (yet 9/2016)
				log.debug("Found '" + url + "' Version: " + version + " in database");
				return ok(media.getMediaBytes()).as(media.getMimeType().toString());
			}
			// Cache media
			downloadMediaAsync(url, mediaVersion);
			FileAndType med = ((ApacheHttpConnector) ApacheHttpConnector.getApacheHttpConnector())
					.getContentAndType(url);
			return ok(med.data).as(med.mimeType);
			// return redirect(url);
		} catch (Exception e) {
			log.debug("Cannot retrieve media document from database", e);
			FileAndType med;
			try {
				med = ((ApacheHttpConnector) ApacheHttpConnector.getApacheHttpConnector()).getContentAndType(url);
			} catch (Exception e1) {
				return internalServerError("Cannot retrieve media document");
			}
			return ok(med.data).as(med.mimeType);
		}
	}

	private static void downloadMediaAsync(String url, MediaVersion version) {
		BiFunction<String, MediaVersion, Boolean> methodQuery = (String imageUrl, MediaVersion mediaVersion) -> {
			try {
				downloadMedia(imageUrl, mediaVersion);
				return true;
			} catch (Exception e) {
				log.debug("Couldn't cache image:" + e.getMessage());
				return false;
			}

		};
		ParallelAPICall.createPromise(methodQuery, url, version);
	}

	public static MediaObject downloadMedia(String url, MediaVersion version) {
		try {
			MediaObject media = DB.getMediaObjectDAO().getByUrlAndVersion(url, version);
			if ( media != null && media.getType() != null)
				return media;
			media = new MediaObject();
			log.info("Downloading " + url);
			FileAndType img = ((ApacheHttpConnector) ApacheHttpConnector.getApacheHttpConnector())
					.getContentAndType(url);
			byte[] mediaBytes = IOUtils.toByteArray(new FileInputStream(img.data));
			img.data.delete();
			media.setUrl(url);
			media.setMediaBytes(mediaBytes);
			if (version != null) {
				media.setMediaVersion(version);
				media.setMimeType(MediaType.parse(img.mimeType));
				log.debug("Storing '" + url + "' Version: " + version + " in database");
				DB.getMediaObjectDAO().makePermanent(media);
				MediaCheckMessage mcm = new MediaCheckMessage(media);
				ActorSelection api = Akka.system().actorSelection("user/mediaChecker");
				api.tell(mcm, ActorRef.noSender());
			} else {
				throw new Exception("Media version is null");
			}
			return media;
		} catch (Exception e) {
			log.error("Couldn't download image at '" + url + "' version " + version, e);
			return null;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Result downloadCollectionMedia(String collectionId, String version) {
		try {
			ObjectId collectionDbId = new ObjectId(collectionId);
			List<RecordResource> records = DB.getRecordResourceDAO().getByCollection(collectionDbId,
					Arrays.asList("media"));
			Set<String> urls = new HashSet<String>();
			for (RecordResource record : records) {
//				if (urls.size() >= 100)
//					break;
				urls.addAll(((List<HashMap<MediaVersion, EmbeddedMediaObject>>) record.getMedia()).stream()
						.filter(m -> m.containsKey(MediaVersion.Original)
								&& m.get(MediaVersion.Original).getType() != null
								&& m.get(MediaVersion.Original).getType().equals(WithMediaType.IMAGE))
						.map(m -> m.get(MediaVersion.Original).getUrl()).collect(Collectors.toSet()));
			}
			PipedOutputStream out = new PipedOutputStream();
			InputStream in = new PipedInputStream(out);
			ZipOutputStream zipout = new ZipOutputStream(out);
			new Thread(new Runnable() {
				public void run() {
					MediaObject image;
					byte[] imageBytes;
					int i = 0;
					for (String url : urls) {
						if (((image = downloadMedia(url, MediaVersion.Original)) != null)
								&& (imageBytes = image.getMediaBytes()) != null) {
							try {
								zipout.putNextEntry(new ZipEntry(URLEncoder.encode(url, StandardCharsets.UTF_8.toString())));
								zipout.write(imageBytes);
								zipout.closeEntry();
							} catch (IOException e) {
								log.debug(e.getMessage(), e);
							}
						}
					}
					try {
						zipout.close();
					} catch (IOException e) {
						log.debug(e.getMessage(), e);
					}
				}
			}).start();
			response().setHeader("content-disposition", "attachment; filename=\""
					+ DB.getCollectionObjectDAO().getById(collectionDbId).getDescriptiveData().getLabel().get(Language.DEFAULT).get(0) + "\"");
			return ok(in).as("application/zip");
		} catch (Exception e) {
			log.debug("Couldn't get images ", e);
			return internalServerError();
		}
	}

	// Make a thumbnail for a specific media object
	public static MediaObject makeThumbnail(MediaObject media) {
		if (media.getType() == WithMediaType.IMAGE)
			try {
				MediaObject thumbnail = makeThumbNew(media, MediaVersion.Thumbnail);
				thumbnail.setMediaVersion(MediaVersion.Thumbnail);
				thumbnail.setParentId(media.getDbId());
				DB.getMediaObjectDAO().makePermanent(thumbnail);
				thumbnail.setUrl("/media/" + thumbnail.getDbId().toString() + "?file=true");
				DB.getMediaObjectDAO().makePermanent(thumbnail);
				return thumbnail;
			} catch (Exception e) {
				return null;
			}
		else
			return null;
	}

	/**
	 * Allow media create with two different methods, by first supplying metadata or
	 * a file File data can arrive in different ways. Whole body is file content,
	 * form based file upload, or json field with encoded file data.
	 *
	 * @param fileData
	 * @return
	 * @throws Exception
	 */
	public static Result createMedia(boolean filedata) throws Exception {

		ObjectNode result = Json.newObject();
		MediaObject med = new MediaObject();

		final Http.MultipartFormData multipartBody = request().body().asMultipartFormData();
		final JsonNode jsonn = request().body().asJson();
		if (multipartBody != null) {
			try {
				Map<String, String[]> formData = multipartBody.asFormUrlEncoded();
				if (formData.containsKey("withMediaRights")) {
					String withMediaRights = formData.get("withMediaRights")[0];
					parseMediaRights(med, withMediaRights);
				} else {
					med.setWithRights(WithMediaRights.UNKNOWN);
				}
				if (!multipartBody.getFiles().isEmpty()) {
					FilePart fp = multipartBody.getFiles().get(0);
					File x = fp.getFile();
					result = storeMedia(med, x);
					MediaCheckMessage mcm = new MediaCheckMessage(med);
					ActorSelection api = Akka.system().actorSelection("user/mediaChecker");
					api.tell(mcm, ActorRef.noSender());
				} else {
					result.put("error", "no image file");
					return badRequest(result);
				}
				return ok(result);
			} catch (Exception e) {
				log.debug("Media create error", e);
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
			if (jsonn.hasNonNull("url")) {
				med.setUrl(jsonn.get("url").asText());
			} else {
				result.put("error", "must provide a url in json request");
				return badRequest(result);
			}
			try {
				// image = HttpConnector.getContent(formData.get("url")[0]);
				File x = HttpConnector.getWSHttpConnector().getURLContentAsFile(jsonn.get("url").asText());
				result = storeMedia(med, x);
				MediaCheckMessage mcm = new MediaCheckMessage(med);
				ActorSelection api = Akka.system().actorSelection("user/mediaChecker");
				api.tell(mcm, ActorRef.noSender());
				return ok(result);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error("", e);
				result.put("error", "error creating from url");
				return badRequest(result);
			}
		} else {
			result.put("error", "MultiPart or Json body is null!");
			return badRequest(result);
		}
	}

	private static MediaObject makeThumbNew(MediaObject mediaObject, MediaVersion version) throws IOException {

		int newWidth = 0;
		int newHeight;

		boolean crop = false;

		switch (version) {
		case Tiny:
			newWidth = 100;
			break;
		case Square:
			newWidth = 150;
			crop = true;
			break;
		case Thumbnail:
			newWidth = 300;
			break;
		case Medium:
			newWidth = 640;
			break;
		default:
			break;
		}
		InputStream mediaBytes = new ByteArrayInputStream(mediaObject.getMediaBytes());
		BufferedImage originalImage = ImageIO.read(mediaBytes);
		int originalWidth = originalImage.getWidth();
		int originalHeight = originalImage.getHeight();
		MediaObject thumbnailObject = new MediaObject(mediaObject);

		// first check if we need to scale width
		if ((originalWidth <= newWidth) && !crop) {
			// there is no need for thumbnail
			thumbnailObject = new MediaObject(mediaObject);
			thumbnailObject.setMediaVersion(version);
			return thumbnailObject;
		}
		// scale height to maintain aspect ratio
		if (!crop)
			newHeight = (newWidth * originalHeight) / originalWidth;
		else
			newHeight = newWidth;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		if (!crop)
			Thumbnails.of(originalImage).size(newWidth, newHeight).keepAspectRatio(true).outputFormat("jpeg")
					.toOutputStream(baos);
		else
			Thumbnails.of(originalImage).size(newWidth, newHeight).crop(Positions.CENTER).outputFormat("jpeg")
					.toOutputStream(baos);
		thumbnailObject.setMimeType(MediaType.JPEG);
		thumbnailObject.setMediaBytes(baos.toByteArray());
		thumbnailObject.setWidth(newWidth);
		thumbnailObject.setHeight(newHeight);
		thumbnailObject.setMediaVersion(version);
		// closes the media bytes input stream
		mediaBytes.close();
		originalImage.flush();
		return thumbnailObject;
	}

	private static ObjectNode storeMedia(MediaObject med, File x) throws IOException, Exception {

		final Timer storeMedia_timer;
		Timer.Context storeMedia_timeContext = null;
		if (DB.getConf().getBoolean("measures.mediaController.storeMedia.time")) {
			storeMedia_timer = MetricsUtils.registry
					.timer(MetricsUtils.registry.name(MediaController.class, "storeMedia", "time"));
			storeMedia_timeContext = storeMedia_timer.time();
		}
		ObjectNode result = Json.newObject();
		if (!x.isFile()) {
			result.put("error", "uploaded file is not valid");
			return result;
		}
		med.setSize(x.length() / 1024);
		BufferedImage image = ImageIO.read(x);
		// TODO: VERY IMPORTANT TO FIND A WAY AROUND THIS AND THE PROMISE!
		med.setType(WithMediaType.IMAGE);
		// TODO: THIS IS A TEMPORARY FIX TO A MAYBE BIG BUG!
		// med.setMimeType(MediaType.parse(Files.probeContentType(x.toPath())));
		InputStream is = new BufferedInputStream(new FileInputStream(x));
		String mimeType = URLConnection.guessContentTypeFromStream(is);
		med.setMimeType(MediaType.parse(mimeType));

		byte[] mediaBytes = IOUtils.toByteArray(new FileInputStream(x));
		med.setMediaBytes(mediaBytes);
		DB.getMediaObjectDAO().makePermanent(med);
		med.setUrl("/media/" + med.getDbId().toString() + "?file=true");
		DB.getMediaObjectDAO().makePermanent(med);

		result = makeThumbs(med, image);
		try {
			return result;
		} finally {
			if (DB.getConf().getBoolean("measures.mediaController.storeMedia.time") && (storeMedia_timeContext != null))
				storeMedia_timeContext.stop();
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
			if (StringUtils.equals(wmright.name().toLowerCase(), rights.toLowerCase())) {
				med.setWithRights(wmright);
			}
		}

	}

	/*
	 * private static ResourceType parseOriginalRights(String resourceType) { for
	 * (ResourceType type : ResourceType.values()) { if
	 * (StringUtils.equals(type.name().toLowerCase(), resourceType.toLowerCase())) {
	 * return type; } } return null; }
	 */

	private static void editMediaAfterChecker(MediaObject med, JsonNode json) {

		MediaType mime = MediaType.parse(json.get("mimetype").asText().toLowerCase());
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
		med.computeQuality();
		med.setMediaVersion(MediaVersion.Original);
		med.setParentId(med.getDbId());
	}

	// TODO:don't hate i will clean it!
	private static ObjectNode makeThumbs(MediaObject med, BufferedImage image) throws Exception {

		final Timer makeThumbs_timer;
		Timer.Context makeThumbs_timeContext = null;
		if (DB.getConf().getBoolean("measures.mediaController.makeThumbs.time")) {
			makeThumbs_timer = MetricsUtils.registry
					.timer(MetricsUtils.registry.name(MediaController.class, "makeThumbs", "time"));
			makeThumbs_timeContext = makeThumbs_timer.time();
		}

		// makeThumb(med, image);
		MediaObject tiny = new MediaObject();
		MediaObject square = new MediaObject();
		MediaObject thumbnail = new MediaObject();
		MediaObject medium = new MediaObject();

		tiny = makeThumbNew(med, MediaVersion.Tiny);
		square = makeThumbNew(med, MediaVersion.Square);
		thumbnail = makeThumbNew(med, MediaVersion.Thumbnail);
		medium = makeThumbNew(med, MediaVersion.Medium);

		ObjectId dbid = med.getDbId();
		tiny.setParentId(dbid);
		square.setParentId(dbid);
		thumbnail.setParentId(dbid);
		medium.setParentId(dbid);

		DB.getMediaObjectDAO().makePermanent(tiny);
		DB.getMediaObjectDAO().makePermanent(square);
		DB.getMediaObjectDAO().makePermanent(thumbnail);
		DB.getMediaObjectDAO().makePermanent(medium);

		tiny.setUrl("/media/" + tiny.getDbId().toString() + "?file=true");
		square.setUrl("/media/" + square.getDbId().toString() + "?file=true");
		thumbnail.setUrl("/media/" + thumbnail.getDbId().toString() + "?file=true");
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

		try {
			return results;
		} finally {
			if (DB.getConf().getBoolean("measures.mediaController.makeThumbs.time"))
				makeThumbs_timeContext.stop();
		}
	}

	// use the libraries we will use for video editing!
	// TODO
	private static MediaObject makeThumb(MediaObject med, BufferedImage image, int width, boolean crop)
			throws IOException {

		if ((image.getWidth() <= 150) && (image.getHeight() <= 150)) {
			crop = false;
		}
		Boolean res = true;
		if ((image.getWidth() <= 150) ^ (image.getHeight() <= 150)) {
			res = false;
		}
		// TODO: comments left on purpose because this needs a bit of cleaning
		IMOperation op = new IMOperation();
		op.addImage(); // input
		// op.blur(2.0).paint(10.0);
		// image.getHeight();
		// image.getWidth();
		if (crop) {
			if (res) {
				op.resize(width, width, "^");
			} else {
				// op.resize(width,width, ">");
			}
			// op.gravity("center");

			// op.extent(width, width);
			// op.crop(width, width);

		} else {
			op.resize(width, width, ">");
		}
		op.addImage(); // output
		// GraphicsMagickCmd cmd = new GraphicsMagickCmd("convert");
		ConvertCmd cmd = new ConvertCmd();
		String outfile = "tempFile2";
		try {
			cmd.run(op, image, outfile);
		} catch (InterruptedException e) {
		} catch (IM4JavaException e) {
		}
		File newFile = new File(outfile);
		BufferedImage ithumb = ImageIO.read(newFile);
		if (crop) {
			op = new IMOperation();
			op.addImage(); // input
			op.gravity("center");
			op.crop(width, width, 0, 0);
			op.p_repage(); // ???check!
			op.addImage();
			ConvertCmd cmd2 = new ConvertCmd();
			String outfile2 = outfile;
			try {
				cmd2.run(op, ithumb, outfile2);
			} catch (InterruptedException e) {
			} catch (IM4JavaException e) {
			}
			newFile = new File(outfile);
			ithumb = ImageIO.read(newFile);
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(ithumb, "jpg", baos);
		baos.flush();
		byte[] thumbByte = baos.toByteArray();
		baos.close();
		// TODO: DO THESE IN A PROMISE!
		MediaObject mthumb = new MediaObject();
		mthumb.setDbId(null);
		mthumb.setMediaBytes(thumbByte);
		mthumb.setMimeType(MediaType.JPEG);
		mthumb.setWidth(ithumb.getWidth(null));
		mthumb.setHeight(ithumb.getHeight(null));

		return mthumb;
	}

	public static void parseMediaFile(File fToParse, MediaObject med) {

		String fName = fToParse.getName();

		BiFunction<File, String, JsonNode> methodQuery = (File fileToParse, String fileName) -> {
			log.info("filename: " + fileName);
			// HttpClient hc = new DefaultHttpClient();
			CloseableHttpClient hc = HttpClients.createDefault();
			JsonNode resp = Json.newObject();
			try {

				HttpPost aFile = new HttpPost("http://mediachecker.image.ntua.gr/api/extractmetadata");
				// File testFile = fileToParse;
				FileBody fileBody = new FileBody(fileToParse);
				MultipartEntityBuilder builder = MultipartEntityBuilder.create();
				builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
				// builder.addBinaryBody("mediafile", testFile,
				// ContentType.create("image/jpeg"), fileName);
				builder.addPart("mediafile", fileBody);
				aFile.setEntity(builder.build());
				CloseableHttpResponse response = hc.execute(aFile);
				String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF8");
				// String id =
				// JsonPath.parse(jsonResponse).read("$['results'][0]['mediaId']");
				resp = Json.parse(jsonResponse);

				log.info(jsonResponse);

				aFile.releaseConnection();

				response.close();
				hc.close();

			} catch (Exception e) {
			}
			return resp;
		};
		Promise<JsonNode> b = ParallelAPICall.createPromise(methodQuery, fToParse, fName);
		JsonNode parsed = b.get(10, TimeUnit.SECONDS);
		editMediaAfterChecker(med, parsed);

	}

	public static void parseMediaURL(String mediaURL, MediaObject med) {

		String url = "http://mediachecker.image.ntua.gr/api/extractmetadata";
		// String queryURL =
		// "http://mediachecker.image.ntua.gr/api/extractmetadata?url="
		// + mediaURL;
		// Logger.info("URL: " + queryURL);
		JsonNode response = null;
		try {
			response = HttpConnector.getWSHttpConnector().postURLContent(url, mediaURL, "url");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error("", e);
		}
		editMediaAfterChecker(med, response);
	}

	// EDIT, DELETE, GET

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
				log.debug("Cannot store Media object to database!", e);
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
			log.debug("Cannot retrieve media document from database", e);
		}
		if (media == null) {
			return internalServerError("Cannot retrieve media" + mediaId + " document from database");
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

	public static boolean hasAccessToMedia(String mediaUrl, List<ObjectId> effectiveIds, Action action) {
		List<RecordResource> resources = DB.getRecordResourceDAO().getByMedia(mediaUrl);
		if (!resources.isEmpty()) {
			for (RecordResource r : resources) {
				if (DB.getRecordResourceDAO().hasAccess(effectiveIds, action, r.getDbId()))
					return true;
			}
			return false;
		} else
			return true;
	}

	public static Result deleteOrphanMedia() {
		DB.getMediaObjectDAO().deleteOrphanMediaObjects();
		return ok();
	}

}
