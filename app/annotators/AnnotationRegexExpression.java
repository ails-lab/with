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

public interface AnnotationRegexExpression {

	public boolean satisfies(AnnotatedObject ano);
	
	public boolean satisfies(AnnotatedObject ano, ArrayList<AnnotationRegexResult> result);
	
	public boolean satisfies2(AnnotatedObject ano, AnnotationResult result);
	
	public ArrayList<String> getReturnElements();
	
	public boolean isStart();
	
	public boolean isEnd();


}
