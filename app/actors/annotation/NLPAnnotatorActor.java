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
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.routing.RoundRobinRouter;
import annotators.NLPAnnotator;
import utils.annotators.AnnotationIndex;
import model.basicDataTypes.Language;


public class NLPAnnotatorActor extends TextAnnotatorActor {

	private static int poolSize = 10;
	
	public static AnnotatorDescriptor descriptor = new Descriptor();
	
	public static class Descriptor implements TextAnnotatorActor.Descriptor {

		@Override
		public String getName() {
			return NLPAnnotator.getName();
		}

		@Override
		public AnnotatorType getType() {
			return AnnotatorType.NER;
		}
		
		private static Set<Language> created = new HashSet<>();

		public synchronized ActorSelection getAnnotator(Language lang, boolean cs) {
	    	if (lang != Language.EN) {
	    		return null;
	    	}
	      	
	       	String actorName = "NLPAnnotator-" + lang.getDefaultCode();

   			if (created.add(lang)) {
  				Akka.system().actorOf( Props.create(NLPAnnotatorActor.class, lang).withRouter(new RoundRobinRouter(poolSize)), actorName);
	    	}
	    	
	    	return Akka.system().actorSelection("user/" + actorName);
	    }
	    
	}
	
	public void onReceive(Object msg) throws Exception {
		super.onReceive(msg);
		
		if (msg instanceof ComputeAnnotationIndex) {
			ComputeAnnotationIndex atm = (ComputeAnnotationIndex)msg;
			sender().tell(new AnnotationIndexResult(analyze(atm.text)), getSelf());
		}
	}
	
	public static class ComputeAnnotationIndex {
		public String text;

		public ComputeAnnotationIndex(String text) {
			this.text = text;
		}
	}
	
	public static class AnnotationIndexResult {
		public AnnotationIndex ai;

		public AnnotationIndexResult(AnnotationIndex ai) {
			this.ai = ai;
		}
	}

    public NLPAnnotatorActor(Language lang) {
    	annotator = NLPAnnotator.getAnnotator(lang);
    }
    
	public AnnotationIndex analyze(String text) throws Exception {
		return ((NLPAnnotator)annotator).analyze(text);
	}
}
