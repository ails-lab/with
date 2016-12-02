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

import java.util.HashSet;
import java.util.Set;

import play.libs.Akka;
import model.basicDataTypes.Language;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.routing.RoundRobinRouter;
import annotators.LookupAnnotator;


public class LookupAnnotatorActor extends TextAnnotatorActor {
	
	private static int poolSize = 10;
	public static String VOCABULARIES = "VOCABULARIES";
	
	public static AnnotatorDescriptor descriptor = new Descriptor();
	
	public static class Descriptor implements TextAnnotatorActor.Descriptor {

		@Override
		public String getName() {
			return LookupAnnotator.getName();
		}

		@Override
		public AnnotatorType getType() {
			return AnnotatorType.LOOKUP;
		}
		
		private static Set<Language> created = new HashSet<>();
		
		public ActorSelection getAnnotator(Language lang) {
			return getAnnotator(lang, false);
		}
		
	    public synchronized ActorSelection getAnnotator(Language lang, boolean cs) {
	       	String actorName = "LookupAnnotator-" + lang.getDefaultCode();

   			if (created.add(lang)) {
   				Akka.system().actorOf( Props.create(LookupAnnotatorActor.class, lang, cs).withRouter(new RoundRobinRouter(poolSize)), actorName);
	    	}
	    	
	    	return Akka.system().actorSelection("user/" + actorName);
	    }  
	}

	private LookupAnnotatorActor(Language lang, boolean cs) {
		annotator = LookupAnnotator.getAnnotator(lang, cs);
	}
}
