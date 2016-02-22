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


package actors;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.Quality;
import model.MediaObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.entity.NByteArrayEntity;
import org.apache.http.util.EntityUtils;
import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import akka.actor.UntypedActor;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.MediaType;

import db.DB;

/**
 *
 * @author Arne Stabenau The actor that talks with the web app.
 */
public class MediaCheckerActor extends UntypedActor {
	public static final ALogger log = Logger.of(MediaCheckerActor.class);

	public AtomicInteger parallelRequests = new AtomicInteger(20);
	public List<MediaCheckMessage> pendingRequests = new ArrayList<MediaCheckMessage>();
	CloseableHttpAsyncClient hc = null;

	public void preStart() {
		hc = HttpAsyncClients.createDefault();
		hc.start();
	}

	public void postStop() {
	}

	public static class MediaCheckMessage {
		public ObjectId mediaObjectId;

		public MediaCheckMessage(MediaObject mediaObject) {
			this.mediaObjectId = mediaObject.getDbId();
		}
	}

	public synchronized void executeRequest(MediaCheckMessage message) {
		HttpPost httpPost = null;
		CloseableHttpResponse response = null;
		try {
			ObjectId mediaObjectId = message.mediaObjectId;
			MediaObject mediaObject = DB.getMediaObjectDAO().findById(
					mediaObjectId);
			httpPost = new HttpPost(
					"http://mediachecker.image.ntua.gr/api/extractmetadata");
			String boundary = UUID.randomUUID().toString();
			if (mediaObject.getMediaBytes() != null) {
				HttpEntity mpEntity = MultipartEntityBuilder
						.create()
						.addPart(
								"mediafile",
								new ByteArrayBody(mediaObject.getMediaBytes(),
										"mediafile"))
						.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
						.setBoundary("-------------" + boundary).build();
				ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
				mpEntity.writeTo(baoStream);
				HttpEntity nByteEntity = new NByteArrayEntity(
						baoStream.toByteArray(),
						ContentType.MULTIPART_FORM_DATA);
				httpPost.setHeader("Content-Type",
						"multipart/form-data;boundary=-------------" + boundary);
				httpPost.setEntity(nByteEntity);
			} else {
				URI uri = RequestBuilder.get()
						.addParameter("url", mediaObject.getUrl()).getUri();
				httpPost.setURI(uri);
			}
			hc.execute(httpPost, new FutureCallback<HttpResponse>() {
				public void completed(final HttpResponse response) {
					try {
						String jsonResponse = EntityUtils.toString(
								response.getEntity(), "UTF8");
						log.info(jsonResponse);
						JsonNode resp = Json.parse(jsonResponse);
						editMediaAfterChecker(mediaObject, resp);
					} catch (ParseException | IOException e) {
						log.error("Http error", e);
					} finally {
						sendQueuedRequests();
					}
				}

				public void failed(final Exception ex) {
					log.error("Http error", ex);
					sendQueuedRequests();
				}

				public void cancelled() {
					log.error("Http canceled");
					sendQueuedRequests();
				}
			});

		} catch (Exception e) {
			log.error("Media checker problem", e);
		} finally {
			try {
				if (response != null)
					response.close();
			} catch (IOException e) {
			}
		}

	}

	private void sendQueuedRequests() {
		MediaCheckMessage mcm = null;
		synchronized (pendingRequests) {
			if (pendingRequests.size() > 0) {
				mcm = pendingRequests.remove(0);
			}
		}
		if (mcm != null)
			executeRequest(mcm);
		else
			parallelRequests.incrementAndGet();
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
		med.setParentId(med.getDbId());

	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof MediaCheckMessage) {
			if (parallelRequests.get() > 0) {
				parallelRequests.getAndDecrement();
				executeRequest((MediaCheckMessage) message);
			} else
				synchronized (pendingRequests) {
					pendingRequests.add((MediaCheckMessage) message);
				}
		}
	}
}
