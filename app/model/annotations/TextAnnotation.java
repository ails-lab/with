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


package model.annotations;

import model.annotations.Annotation.AnnotationTarget;
import model.annotations.Annotation.AnnotationBody;
import model.basicDataTypes.Language;


public class TextAnnotation extends Annotation<AnnotationBody, TextAnnotation.TextAnnotationTarget>{
	
	public static class TextAnnotationTarget extends AnnotationTarget {
		//full json path (?)
		String propertyName;
		Language language;
		//why store original value, it is in the record
		String originalValue;
		//what is start end?
		int start, end;
	}

}
