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


package annotators;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import notifications.AnnotationNotification;
import notifications.Notification.Activity;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.bson.types.ObjectId;

import play.libs.Akka;
import play.libs.Json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import controllers.AnnotationController;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import sources.core.QueryBuilder;
import utils.NotificationCenter;
import model.annotations.targets.AnnotationTarget;
import model.basicDataTypes.Language;
import actors.TokenLoginActor.TokenCreateMessage;
import actors.TokenLoginActor.TokenResponseMessage;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;
import akka.util.Timeout;
import annotators.Annotator.AnnotatorDescriptor;

public class AnnotationControlActor extends UntypedActor {

	private static Timeout timeout = new Timeout(Duration.create(5, "seconds"));

	private String requestId;
	private ObjectId recordId; 
	private ObjectId userId;
	private boolean record;
	
	private Map<RequestAnnotator.Descriptor, List<ObjectNode>> requestMap;
	private Map<RequestAnnotator.Descriptor, Integer> requestCount;
	private Set<String> requestIds;
	
	private boolean textCompleted;
	
	public AnnotationControlActor(String requestId, ObjectId recordId, ObjectId userId, boolean record) {
		this.requestId = requestId;
		this.recordId = recordId;
		this.userId = userId;
		this.record = record;
		this.textCompleted = true;
		
		requestMap = new HashMap<>();
		requestCount = new HashMap<>();
		requestIds = new HashSet<>();
	}

	
	@Override
	public void onReceive(Object msg) {
		if (msg instanceof TextAnnotateMessage) {
			System.out.println("**** TextAnnotateMessage");
			textCompleted = false;
			TextAnnotateMessage atm = (TextAnnotateMessage)msg;

			ActorSelection annotator = atm.annotator.getAnnotator(atm.lang);
			annotator.tell(new TextAnnotator.AnnotateTextMessage(atm.userId, atm.text, atm.target, atm.props, requestId), ActorRef.noSender());

		} else if (msg instanceof RequestAnnotateMessage) {
			System.out.println("**** RequestAnnotateMessage");
			RequestAnnotateMessage atm = (RequestAnnotateMessage)msg;

			handleRequestAnnotateMessage(atm);
		} else if (msg instanceof RequestAnnotationReceived) {
			
			AnnotationController.addAnnotation(AnnotationController.getAnnotationFromJson(((RequestAnnotationReceived) msg).annotation), userId);
			
		} else if (msg instanceof AnnotateRequestsCompleted) {
			System.out.println("**** AnnotateRequestsCompleted");
			try {
				sendPendingRequests();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (msg instanceof TextAnnotationsCompleted) {
			System.out.println("**** TextAnnotationsCompleted");
			textCompleted = true;
			sendFinishNotification();
		} else if (msg instanceof RequestAnnotationsCompleted) {
			System.out.println("**** RequestAnnotationsCompleted");
			RequestAnnotationsCompleted atm = (RequestAnnotationsCompleted)msg;
			
			requestIds.remove(atm.requestId);
			
			sendFinishNotification();
		}
	}

	private void sendFinishNotification() {
		if (textCompleted && requestIds.size() == 0) {
	        AnnotationNotification notification = new AnnotationNotification();
	
			if (record) {
	            notification.setActivity(Activity.RECORD_ANNOTATING_COMPLETED);
			} else {
	            notification.setActivity(Activity.COLLECTION_ANNOTATING_COMPLETED);
			}
	
			notification.setOpenedAt(new Timestamp(new Date().getTime()));
	        notification.setResource(recordId);
	        notification.setReceiver(userId);
	        
	        NotificationCenter.sendNotification(notification);
	        
	        getContext().stop(getSelf());
		}
	}
	
	private void sendPendingRequests() {
		for (Map.Entry<RequestAnnotator.Descriptor, List<ObjectNode>> entry : requestMap.entrySet()) {
			sendRequests(entry.getKey(), entry.getValue());
		}
	}
	
	private void sendRequests(RequestAnnotator.Descriptor ad, List<ObjectNode> list) {
		if (list.size() == 0) {
			return;
		}
		
		int count = requestCount.get(ad);
		
		ArrayNode array = Json.newObject().arrayNode();
		for (ObjectNode obj : list) {
			array.add(obj);
		}

		String rid = requestId + "." + count;
		ObjectNode json = Json.newObject();
		json.put("requestId", rid);
		json.put("data", array);
		
		HttpClient client = HttpClientBuilder.create().build();

		HttpPost request = new HttpPost(ad.getService());
		request.setHeader("content-type", "application/json");
//		request.setHeader("accept", "application/json");

		try {
			request.setEntity(new StringEntity(json.toString()));
		
			System.out.println(json.toString());
		
			list.clear();

//			if (client.execute(request).getStatusLine().getStatusCode() == 200) {
//				requestCount.put(ad, ++count);
//				requestIds.add(rid);
//			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void handleRequestAnnotateMessage(RequestAnnotateMessage atm)	{
		ActorSelection tokenLoginActor = Akka.system().actorSelection("/user/tokenLoginActor");
		
		TokenCreateMessage tokenCreateMsg = new TokenCreateMessage(atm.userId);
		
		for (String url : atm.urls) {
			ObjectNode json = Json.newObject();
		
			QueryBuilder qb = new QueryBuilder(url);
		
			Future<Object> future1 = Patterns.ask(tokenLoginActor, tokenCreateMsg, timeout);
			Future<Object> future2 = Patterns.ask(tokenLoginActor, tokenCreateMsg, timeout);

			TokenResponseMessage trm1;
			try {
				trm1 = (TokenResponseMessage)Await.result(future1, timeout.duration());
				TokenResponseMessage trm2 = (TokenResponseMessage)Await.result(future2, timeout.duration());

				qb.addSearchParam("token", trm1.token + "");
			
//				String id = (System.currentTimeMillis() + Math.abs(rand.nextLong())) + "" + Math.abs(rand.nextLong());
			
//				json.put("requestId", requestId + "." + imageRequests.size());
				json.put("imageURL", qb.getHttp());
				json.put("annotationURL", atm.annotator.getResponseApi() + "?token=" + trm2.token);
				json.put("recordId", atm.target.getRecordId().toString());

				List<ObjectNode> list = requestMap.get(atm.annotator);
				if (list == null) {
					list = new ArrayList<>();
					requestMap.put(atm.annotator, list);
					requestCount.put(atm.annotator, 0);
				}
				list.add(json);
				
				if (list.size() == atm.annotator.getDataLimit()) {
					sendRequests(atm.annotator, list);
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static class TextAnnotateMessage {
		public ObjectId userId;
		public String text;
		public AnnotationTarget target;
		public Map<String, Object> props;
		public TextAnnotator.Descriptor annotator;
		public Language lang;

		public TextAnnotateMessage(ObjectId userId, String text, AnnotationTarget target, Map<String, Object> props, TextAnnotator.Descriptor annotator, Language lang) {
			this.userId = userId;
			this.text = text;
			this.target = target;
			this.props = props;
			this.annotator = annotator;
			this.lang = lang;
		}
	}
	
	public static class RequestAnnotateMessage {
		public ObjectId userId;
		public String[] urls;
		public AnnotationTarget target;
		public Map<String, Object> props;
		public RequestAnnotator.Descriptor annotator;

		public RequestAnnotateMessage(ObjectId userId, String[] urls, AnnotationTarget target, Map<String, Object> props, RequestAnnotator.Descriptor annotator) {
			this.userId = userId;
			this.urls = urls;
			this.target = target;
			this.props = props;
			this.annotator = annotator;
		}
	}
	
	public static class TextAnnotationsCompleted {
	}

	public static class RequestAnnotationsCompleted {
		public String requestId;
		
		public RequestAnnotationsCompleted(String requestId) {
			this.requestId = requestId;
		}
	}

	public static class AnnotateRequestsCompleted {
	}

	public static class RequestAnnotationReceived {
		public JsonNode annotation;
		
		public RequestAnnotationReceived(JsonNode annotation) {
			this.annotation = annotation;
		}
	}
	

}
