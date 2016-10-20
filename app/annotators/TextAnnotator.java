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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;

import akka.actor.ActorSelection;

import model.annotations.Annotation;
import model.annotations.targets.AnnotationTarget;
import model.basicDataTypes.Language;

public abstract class TextAnnotator extends Annotator {

	protected Language lang;
	
	public interface Descriptor extends AnnotatorDescriptor {

		default ActorSelection getAnnotator(Language lang) {
			return getAnnotator(lang, false);
		}
			
		public ActorSelection getAnnotator(Language lang, boolean cs);
	}

	
	public static class AnnotateTextMessage {
		public ObjectId userId;
		public String text;
		public AnnotationTarget target;
		public Map<String, Object> props;
		public String requestId;

		public AnnotateTextMessage(ObjectId userId, String text, AnnotationTarget target, Map<String, Object> props, String requestId) {
			this.userId = userId;
			this.text = text;
			this.target = target;
			this.props = props;
			this.requestId = requestId;
		}
	}
	
	public void onReceive(Object msg) throws Exception {
		// TODO Auto-generated method stub
		if (msg instanceof AnnotateTextMessage) {
			AnnotateTextMessage atm = (AnnotateTextMessage)msg;
			List<Annotation> annotations = annotate(atm.text, atm.userId, atm.target, atm.props);
			storeAnnotatations(annotations, atm.userId);
			reply(atm.requestId);
		}
	}
	
	public abstract List<Annotation> annotate(String text, ObjectId user, AnnotationTarget target, Map<String, Object> properties) throws Exception;



	private static Pattern p = Pattern.compile("(<.*?>)");

	public static String strip(String text) {
		Matcher m = p.matcher(text);
		
		StringBuffer sb = new StringBuffer();
		
		int prev = -1;
		while (m.find()) {
			int s = m.start(1);
			int e = m.end(1);
			
			if (prev == -1) {
				sb.append(text.substring(0,s));
			} else {
				sb.append(text.substring(prev, s));
			}
			
			char[] c = new char[e - s];
			Arrays.fill(c, ' ');
			
			sb.append(c);
			
			prev = e;
		}
		
		if (prev == -1) {
			return text;
		} else {
			sb.append(text.substring(prev));
		
			return sb.toString();
		}
	}

}
