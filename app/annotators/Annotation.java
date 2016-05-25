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

public class Annotation {

	private Class<? extends Annotator> annotator;
	
	private String body;
	
	private int startPosition;
	private int endPosition;
	
	private String URI;
	private double confidence;
	
	public Annotation(Class<? extends Annotator> annotator, String body, int sp, int ep, String URI, double confidence) {
		this.setAnnotator(annotator);
		this.setBody(body);
		this.setStartPosition(sp);
		this.setEndPosition(ep);
		this.setURI(URI);
		this.setConfidence(confidence);
				
	}

	public Class<? extends Annotator> getAnnotator() {
		return annotator;
	}

	public void setAnnotator(Class<? extends Annotator> annotator) {
		this.annotator = annotator;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public int getStartPosition() {
		return startPosition;
	}

	public void setStartPosition(int startPosition) {
		this.startPosition = startPosition;
	}

	public int getEndPosition() {
		return endPosition;
	}

	public void setEndPosition(int endPosition) {
		this.endPosition = endPosition;
	}

	public String getURI() {
		return URI;
	}

	public void setURI(String uRI) {
		URI = uRI;
	}

	public double getConfidence() {
		return confidence;
	}

	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}
}
