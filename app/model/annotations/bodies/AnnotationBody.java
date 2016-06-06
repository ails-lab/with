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


package model.annotations.bodies;

import java.util.ArrayList;
import org.bson.types.ObjectId;

public class AnnotationBody {
	
	double confidence;
	
	AnnotationScore score;
		
	public double getConfidence() {
		return confidence;
	}

	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}

	public AnnotationScore getScore() {
		return score;
	}

	public void setScore(AnnotationScore score) {
		this.score = score;
	}
	
	public static class AnnotationScore {

		/**
		 * An arrayList with the user ids who approved this annotation body.
		 */
		ArrayList<ObjectId> approvedBy;
		
		/**
		 * An arrayList with the user ids who rejected this annotation body.
		 */
		ArrayList<ObjectId> rejectedBy;
		
		/**
		 * An arrayList with the user ids who didn't comment on this annotation body.
		 */
		ArrayList<ObjectId> dontKnowBy;
		
		
		public ArrayList<ObjectId> getApprovedBy() {
			return approvedBy;
		}

		public void setApprovedBy(ArrayList<ObjectId> approvedBy) {
			this.approvedBy = approvedBy;
		}

		public ArrayList<ObjectId> getRejectedBy() {
			return rejectedBy;
		}

		public void setRejectedBy(ArrayList<ObjectId> rejectedBy) {
			this.rejectedBy = rejectedBy;
		}

		public ArrayList<ObjectId> getDontKnowByBy() {
			return dontKnowBy;
		}

		public void setDontKnowBy(ArrayList<ObjectId> dontKnowBy) {
			this.dontKnowBy = dontKnowBy;
		}
	}



}
