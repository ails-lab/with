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

import play.Logger;
import play.libs.Json;

import java.util.function.BiFunction;
import java.util.function.Function;

import play.libs.F.Function0;
import play.libs.F.Promise;
import play.mvc.Result;
import utils.MethodCallable;

public class ParallelAPICall {
			
	/**
	 * @param methodQuery	method that forms and issues the API query
	 * @param responseCollectionMethod		method that combines the API results and returns them as a List<R>
	 * @param input		input parameters used as input to methodQuery's call method
	 * @return 	a list of Promises with <R>
	 */
	public static <I, U, R> Promise<R> createPromise(final BiFunction<I, U, R> methodQuery, 
			final I input1, final U input2) {
		Promise<R> p = Promise.promise(new Function0<R>() {
				public R apply() throws Throwable {
					return methodQuery.apply(input1, input2);
				}
			 });
		return p;
	}
	
	/**
	 * @param responseCollectionMethod	method that combines the API results and returns them as a List<R>
	 * @param promises	list of Promises
	 * @return 	a Promise with Result
	 */
	public static <R> Promise<Result> combineResponses(final Function<R, Boolean> responseCollectionMethod,
			Iterable<Promise<R>> promises) {		
		Promise<List<R>> promisesSequence = Promise.sequence(promises);		 
        Promise<Result> promiseResult = promisesSequence.map(
    		new play.libs.F.Function<Iterable<R>, Result>() {
    			List<R> finalResponses = new ArrayList<R>();
    			public Result apply(Iterable<R> responses) {
    				finalResponses.addAll(iterateResponses(responseCollectionMethod, responses));
    				//Logger.debug("Total time for all sources to respond: " + (System.currentTimeMillis()- initTime));
    				return toStatus(finalResponses);
    			}
    		}
        );
        return promiseResult;	
	}
	
	public static <R> Promise<Result> combineResponses(final Function<R, Boolean> responseCollectionMethod,
			Iterable<Promise<R>> promises, final Function<List<R>, List<R>> filter) {		
		Promise<List<R>> promisesSequence = Promise.sequence(promises);		 
        Promise<Result> promiseResult = promisesSequence.map(
    		new play.libs.F.Function<Iterable<R>, Result>() {
    			public Result apply(Iterable<R> responses) {
    				List<R> combinedResponses, finalResponses = new ArrayList<R>();
					combinedResponses = iterateResponses(responseCollectionMethod, responses);					
					finalResponses = filter.apply(combinedResponses);
    				return toStatus(finalResponses);
    				//Logger.debug("Total time for all sources to respond: " + (System.currentTimeMillis()- initTime));
    			}
    		}
        );
        return promiseResult;
	}
	
	public static <R> List<R> iterateResponses(final java.util.function.Function<R, Boolean> responseCollectionMethod, Iterable<R> responses) {
		List<R> finalResponses = new ArrayList<R>();
		for (R r: responses) {
			if (responseCollectionMethod.apply(r))
				finalResponses.add(r);
		}
		return finalResponses;
	}
	
	
	private static <R> Result toStatus(List<R> responses) {
		if (!responses.isEmpty()) {
			return play.mvc.Results.ok(Json.toJson(responses));
		}
		else
			return play.mvc.Results.noContent();
	}
	
}
