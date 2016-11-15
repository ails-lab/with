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

import model.basicDataTypes.Language;


import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.routing.RoundRobinRouter;
import annotators.DBPediaAnnotator;
import annotators.TextAnnotator;
import play.libs.Akka;

public class DBPediaAnnotatorActor extends TextAnnotatorActor {

	private static int poolSize = 50;

	public static AnnotatorDescriptor descriptor = new Descriptor();
	
	public static class Descriptor implements TextAnnotatorActor.Descriptor {

		@Override
		public String getName() {
			return DBPediaAnnotator.getName();
		}

		@Override
		public AnnotatorType getType() {
			return AnnotatorType.NER;
		}

		private static Set<Language> created = new HashSet<>();

	    public synchronized ActorSelection getAnnotator(Language lang, boolean cs) {
	    	if (!DBPediaAnnotator.serverMap.containsKey(lang)) {
				return null;
			}
	    	
	    	String actorName = "DBPediaAnnotator-" + lang.getDefaultCode();

	    	if (created.add(lang)) {
				Akka.system().actorOf( Props.create(DBPediaAnnotatorActor.class, lang).withRouter(new RoundRobinRouter(poolSize)), actorName);
			}
		
			return Akka.system().actorSelection("user/" + actorName);
	    }
		
	}
	
    public DBPediaAnnotatorActor(Language lang) {
    	annotator = DBPediaAnnotator.getAnnotator(lang);
    }
}
