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

public class WeightedSum {
	
	private boolean normalizeWeights;
	private double sum;
	private double weightsSum;
	
	public WeightedSum() {
		super();
		init();
	}

	public WeightedSum init() {
		return init(true);
	}
	
	public WeightedSum init(boolean normalize) {
		sum = 0;
		weightsSum = 0;
		setNormalizeWeights(normalize);
		return this;
	}
	
	public boolean isNormalizeWeights() {
		return normalizeWeights;
	}

	public void setNormalizeWeights(boolean normalizeWeights) {
		this.normalizeWeights = normalizeWeights;
	}

	public void add(double weight, double value){
		sum+=(weight*value);
		weightsSum+=weight;
	}
	
	public void add(double weight, boolean value){
		add(weight,value?1:0);
	}
	
	public void add(boolean value){
		add(value?1:0);
	}
	public void add(double value){
		add(1, value);
	}
	
	public double computeWeight(){
		return sum/ (normalizeWeights?weightsSum:1);
	}
	

}
