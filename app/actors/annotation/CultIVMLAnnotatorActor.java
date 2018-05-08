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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import play.Play;
import annotators.CultIVMLAnnotator;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class CultIVMLAnnotatorActor extends RequestAnnotatorActor {
	
	public static String ip = "";

	static {
		try {
			ip = "http://" + InetAddress.getLocalHost().getHostAddress() + ":" + Play.application().configuration().getString("http.port");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public static AnnotatorDescriptor descriptor = new Descriptor();
	
	public static class Descriptor implements RequestAnnotatorActor.Descriptor {
	
		@Override
		public AnnotatorType getType() {
			return AnnotatorType.IMAGE;
		}
	
		@Override
		public String getName() {
			return CultIVMLAnnotator.getName();
		}
		
		@Override
		public String getResponseApi() {
			return CultIVMLAnnotator.reponseApi;
		}
		
		@Override
		public String getService() {
			return CultIVMLAnnotator.service;
		}
		
		@Override
		public int getDataLimit() {
			return 100;
		}
		
		@Override
		public ObjectNode createDataEntry(String imageURL, String recordId, long token) {
			return CultIVMLAnnotator.createDataEntry(imageURL, recordId, token);
		}
		
		@Override
		public ObjectNode createMessage(String rid, List<ObjectNode> list, long token) {
			return CultIVMLAnnotator.createMessage(rid, list, token);
		}
	}


	@Override
	protected void reply(String requestId, String messageId) {
	}
}
