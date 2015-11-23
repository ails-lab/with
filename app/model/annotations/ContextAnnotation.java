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

import org.bson.types.ObjectId;
import model.annotations.Annotation.AnnotationTarget;
import model.annotations.Annotation.AnnotationBody;

public class ContextAnnotation<T1 extends AnnotationBody> extends Annotation<AnnotationBody, ContextAnnotation.ContextAnnotationTarget> {
	
	public static class ContextAnnotationTarget extends AnnotationTarget {
		ObjectId collectionId;
		int position;
	}

}
