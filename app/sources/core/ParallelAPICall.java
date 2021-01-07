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


package sources.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import akka.dispatch.ExecutionContexts;
import play.Logger;
import play.libs.Json;
import play.mvc.Result;

public class ParallelAPICall {

	
	public enum Priority {
		
		BACKEND( Executors.newFixedThreadPool(4) ),
				
				
		FRONTEND(  Executors.newFixedThreadPool(4) );
		private Executor executor;
		
		private Priority( Executor ex ) {
			this.executor = ex;
			
		}
		
		public Executor getExecutor() {
			return executor;
		}
	}

	/**
	 * @param methodQuery
	 *            method that forms and issues the API query
	 * @param input1
	 *            , input 2 input parameters used as input to methodQuery's call
	 *            method
	 * @return a Promise with result <R>
	 */
	public static <I, U, R> CompletionStage<R> createPromise(
			final BiFunction<I, U, R> methodQuery, final I input1,
			final U input2) {
		return createPromise(methodQuery, input1, input2, Priority.BACKEND);

	}

	public static <I, U, R> CompletionStage<R> createPromise(
			final Function<I, R> methodQuery, final I input) {
		return createPromise(methodQuery, input, Priority.BACKEND);
	}
	

	public static <I, U, R> CompletionStage<R> createPromise(
			final Function<I, R> methodQuery, final I input, Priority priority) {
		CompletionStage<R> p = CompletableFuture.supplyAsync( ()->methodQuery.apply(input)
				, priority.getExecutor());
		return p;
	}
	
	public static <U, R> CompletionStage<R> createPromise(
			final Supplier<R> methodQuery, Priority priority) {
		CompletionStage<R> p = CompletableFuture.supplyAsync( () ->
			methodQuery.get(), priority.getExecutor() );
		return p;
	}

	public static <I, U, R> CompletionStage<R> createPromise(
			final BiFunction<I, U, R> methodQuery, final I input1,
			final U input2, Priority priority) {

		CompletionStage<R> p = CompletableFuture.supplyAsync( 
				() -> methodQuery.apply(input1, input2)
				, priority.getExecutor()
			);
		return p;
	}

	/**
	 * @param responseCollectionMethod
	 *            method that combines the API results and returns them as a
	 *            List<R>
	 * @param promises
	 *            list of Promises
	 * @return a Promise with Result
	 */
	public static <R> CompletionStage<Result> combineResponses(
			final Function<R, Boolean> responseCollectionMethod,
			Iterable<CompletionStage<R>> promises, Priority priority) {
		final ArrayList<CompletableFuture<R>> tmpProm = new ArrayList<>();
		for( CompletionStage<R> cs: promises) tmpProm.add( cs.toCompletableFuture() );
		
		CompletionStage<Void> promisesSequence = CompletableFuture.allOf(tmpProm.toArray( new CompletableFuture[0]));

		CompletionStage<Result> promiseResult = promisesSequence
				.thenApplyAsync(new Function<Void, Result>() {
					List<R> finalResponses = new ArrayList<R>();

					public Result apply(Void v) {
						for( CompletableFuture<R> cf: tmpProm ) {
							try {
								if( responseCollectionMethod.apply(cf.get()))
									finalResponses.add( cf.get());
							} catch( Exception e ) {
								Logger.error("", e);
							}
						}
						// Logger.debug("Total time for all sources to respond: "
						// + (System.currentTimeMillis()- initTime));
						return toStatus(finalResponses);
					}
				}, priority.getExecutor());
		return promiseResult;
	}

	public static <R> CompletionStage<Result> combineResponses(
			final Function<R, Boolean> responseCollectionMethod,
			Iterable<CompletionStage<R>> promises,
			final Function<List<R>, List<R>> filter) {

		final ArrayList<CompletableFuture<R>> tmpProm = new ArrayList<>();
		for( CompletionStage<R> cs: promises) tmpProm.add( cs.toCompletableFuture() );
		
		CompletionStage<Void> promisesSequence = CompletableFuture.allOf(tmpProm.toArray( new CompletableFuture[0]));

		CompletionStage<Result> promiseResult = promisesSequence
				.thenApplyAsync(new Function<Void, Result>() {
					List<R> finalResponses = new ArrayList<R>();

					public Result apply(Void v) {
						for( CompletableFuture<R> cf: tmpProm ) {
							try {
								if( responseCollectionMethod.apply(cf.get()))
									finalResponses.add( cf.get());
							} catch( Exception e ) {
								Logger.error("", e);
							}
						}
						
						
						// Logger.debug("Total time for all sources to respond: "
						// + (System.currentTimeMillis()- initTime));
						return toStatus(filter.apply( finalResponses));
					}
				}, Priority.BACKEND.getExecutor());
		return promiseResult;
	}

	public static <R> List<R> iterateResponses(
			final java.util.function.Function<R, Boolean> responseCollectionMethod,
			Iterable<R> responses) {
		List<R> finalResponses = new ArrayList<R>();
		for (R r : responses) {
			if (responseCollectionMethod.apply(r))
				finalResponses.add(r);
		}
		return finalResponses;
	}

	private static <R> Result toStatus(List<R> responses) {
		if (!responses.isEmpty()) {
			return play.mvc.Results.ok(Json.toJson(responses));
		} else
			return play.mvc.Results.noContent();
	}

}
