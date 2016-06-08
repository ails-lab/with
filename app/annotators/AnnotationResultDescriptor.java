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

import java.util.ArrayList;

public class AnnotationResultDescriptor {
	
	private String name;
	private ArrayList<String> classes;
	private boolean span;
	private boolean id;
	
	public AnnotationResultDescriptor(String name, ArrayList<String> classes, boolean id, boolean span) {
		this.name = name;
		this.classes = classes;
		this.id = id;
		this.span = span;
	}
	
	public String getName() {
		return name;
	}
	
	public ArrayList<String> getClasses() {
		return classes;
	}
	
	public boolean getSpan() {
		return span;
	}

	public boolean getID() {
		return id;
	}

	public String toString() {
		return "<" + name +  ":" + classes + ":" + span + ">";
	}
	
}
