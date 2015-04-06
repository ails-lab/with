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


package espace.core;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import play.Logger;
import play.libs.Json;
import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.mvc.Result;
import play.mvc.Results;
import utils.MethodCallable;

public class ParallelAPICall {
			
	/**
	 * @param methodQuery	method that forms and issues the API query
	 * @param responseCollectionMethod		method that combines the API results and returns them as a List<R>
	 * @param input		input parameters used as input to methodQuery's call method
	 * @return 	a list of Promises with <R>
	 */
	public static <I, R> Promise<R> createPromise(final MethodCallable<I, R> methodQuery, 
			final I input) {
		Promise<R> p = Promise.promise(new Function0<R>() {
				public R apply() throws Throwable {
					return methodQuery.call(input);
				}
			 });
		return p;
	}
	
	/**
	 * @param responseCollectionMethod	method that combines the API results and returns them as a List<R>
	 * @param promises	list of Promises
	 * @return 	a Promise with Result
	 */
	public static <R> Promise<Result> combineResponses(final MethodCallable<R, Boolean> responseCollectionMethod,
			Iterable<Promise<R>> promises) {		
		Promise<List<R>> promisesSequence = Promise.sequence(promises);		 
        Promise<Result> promiseResult = promisesSequence.map(
    		new Function<Iterable<R>, Result>() {
    			List<R> finalResponses = new ArrayList<R>();
    			public Result apply(Iterable<R> responses) {
    				for (R r: responses) {
    					if (responseCollectionMethod.call(r))
    						finalResponses.add(r);
    				}
    				//Logger.debug("Total time for all sources to respond: " + (System.currentTimeMillis()- initTime));
    				if (!finalResponses.isEmpty()) {
    					return play.mvc.Results.ok(Json.toJson(finalResponses));
    				}
    				else
    					return play.mvc.Results.noContent();
    			}
    		}
        );
        return promiseResult;	
	}

}
