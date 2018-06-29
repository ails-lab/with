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


package model.quality;

import java.util.HashMap;

import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.WithMediaRights;
import sources.core.Utils;
import sources.utils.JsonContextRecord;

public class RecordQuality {
	
	public double compute(JsonContextRecord rec){
		WeightedSum topq = new WeightedSum();
		topq.init();
		
		topq.add(proportional(rec.getValue("provenance").size()));
//		TODO check the media
		rec.enterContext("descriptiveData");
		topq.add(computeLiteral(rec.getMultiLiteralValue("label")));
		topq.add(computeLiteral(rec.getMultiLiteralValue("description")));
		topq.add(computeLiteral(rec.getMultiLiteralValue("isShownAt")));
		topq.add(computeLiteral(rec.getMultiLiteralValue("isShownBy")));
		topq.add(proportional(rec.getValue("dates").size()));
		topq.add(computeLiteral(rec.getMultiLiteralValue("country")));
		topq.add(computeLiteral(rec.getMultiLiteralValue("dcLanguage")));
		rec.exitContext();
	    rec.setValue("qualityMeasure", ""+topq.computeWeight());
		return topq.computeWeight();
	}
	
	
	protected double computeRights(EmbeddedMediaObject l){
		WeightedSum s = new WeightedSum().init();
		s.add(Utils.hasInfo(l.getOriginalRights()));
		s.add(l.getWithRights()!=WithMediaRights.UNKNOWN);
		// TODO check for creative....
		s.add(l.getWithRights()!=WithMediaRights.UNKNOWN);
		s.add(4,proportional(l.getSize()));
		return s.computeWeight();
	}
	
	protected double computeLiteral(HashMap l){
		if (Utils.hasInfo(l)){
			return proportional(l.size());
		} else return 0;
	}

	private double proportional(double x) {
		return 1-1/(1+x);
	}
	
	private double exponential(double x) {
		return 1-1/Math.pow(2, -x);
	}

}
