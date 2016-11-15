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

import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class RequestAnnotator extends Annotator {

	public interface Descriptor extends AnnotatorDescriptor {

		public String getService();
		
		public String getResponseApi();
		
		public int getDataLimit();
		
		public ObjectNode createDataEntry(String imageURL, String recordId, long token);
		
		public ObjectNode createMessage(String rid, List<ObjectNode> list, long token);
		
	}
	
	public void onReceive(Object msg) throws Exception {
		// TODO Auto-generated method stub
	}

}
