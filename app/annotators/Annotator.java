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

import java.util.List;

import org.bson.types.ObjectId;

import play.libs.Akka;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.UntypedActor;
import controllers.AnnotationController;
import model.annotations.Annotation;

public abstract class Annotator extends UntypedActor {

	public interface AnnotatorDescriptor {

		public static enum AnnotatorType {
			LOOKUP,
			NER,
			IMAGE
		}
		
		public String getName();
		
		public AnnotatorType getType();
	}
	
	public static String LANGUAGE = "lang";
	public static String TEXT = "text";
	public static String TYPE = "type";
	public static String TEXT_FIELDS = "text_fields";
	public static String IMAGE_ANNOTATOR = "image";
	public static String TEXT_ANNOTATOR = "text";
	public static String[] textfields = new String[] {"description", "label"};
	
	
	public static class AnnotationsMessage {
		public List<Annotation> annotations;

		public AnnotationsMessage(List<Annotation> annotations) {
			this.annotations = annotations;
		}
	}
	
	public void storeAnnotatations(List<Annotation> annotations, ObjectId user) {
		for (Annotation ann : annotations) {
			AnnotationController.addAnnotation(ann, user);
		}
	}
	
	protected abstract void reply(String requestId, String messageId);
	
}
