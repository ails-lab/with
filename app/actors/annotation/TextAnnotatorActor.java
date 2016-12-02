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


package actors.annotation;

import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import play.libs.Akka;
import actors.annotation.AnnotationControlActor;
import actors.annotation.AnnotatorActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import annotators.TextAnnotator;
import model.annotations.Annotation;
import model.annotations.targets.AnnotationTarget;
import model.basicDataTypes.Language;

public abstract class TextAnnotatorActor extends AnnotatorActor implements TextAnnotator {

	public interface Descriptor extends AnnotatorDescriptor {

		default ActorSelection getAnnotator(Language lang) {
			return getAnnotator(lang, false);
		}
			
		public ActorSelection getAnnotator(Language lang, boolean cs);
	}
	
	public static class Annotate {
		public ObjectId userId;
		public String text;
		public AnnotationTarget target;
		public Map<String, Object> props;
		public String requestId;
		public String messageId;

		public Annotate(ObjectId userId, String text, AnnotationTarget target, Map<String, Object> props, String requestId, String messageId) {
			this.userId = userId;
			this.text = text;
			this.target = target;
			this.props = props;
			this.requestId = requestId;
			this.messageId = messageId;
		}
	}
	
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof Annotate) {
			Annotate atm = (Annotate)msg;
			try {
				storeAnnotatations(annotate(atm.text, atm.userId, atm.target, atm.props), atm.userId);
			} finally {
				reply(atm.requestId, atm.messageId);
			}
		}
	}
	
	protected void reply(String requestId, String messageId) {
		ActorSelection actor = Akka.system().actorSelection("user/" + requestId);
		actor.tell(new AnnotationControlActor.AnnotateTextDone(messageId), ActorRef.noSender());
	}
	
	protected TextAnnotator annotator;
	
	public List<Annotation> annotate(String text, ObjectId user, AnnotationTarget target, Map<String, Object> properties) throws Exception {
//		System.out.println(this + " " + annotator + " /// " + getSelf().path().name());
		return annotator.annotate(text, user, target, properties);
	}

}
