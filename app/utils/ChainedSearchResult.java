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


package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.types.ObjectId;

import akka.actor.Cancellable;
import play.libs.Akka;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.F.RedeemablePromise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import search.Query;
import search.Response;

/**
 * 
 * @author Arne Stabenau
 * 
 * This class allows to issue a query to many sources and for each fullfilled promise it 
 * will create a Result that can be send to the Client. It will shortly stay in memory, the 
 * intended use is, that the client issues a followup request as soon as he receives 
 * a (partial) response.
 * 
 *  
 *
 */
public class ChainedSearchResult {
	
	// the running queries, 
	public static HashMap<String, ChainedSearchResult> cache = new HashMap<String, ChainedSearchResult>();

	// unclear how we do the timeouts
	// between a response from "createSearchResponse" and the next "createSearchResponse" there is a timelimit
	public static ArrayList<Promise<?>> timeouts = new ArrayList<Promise<?>>();

	public String uuid;
	public int countPromises;
	public Query query;
	public Response lastResponse;
	
	public List<Response.SingleResponse> currentResponses = new ArrayList<Response.SingleResponse>();
	
	public AtomicInteger finishedPromises = new AtomicInteger();	
	public RedeemablePromise<Result> pendingPromise = null;

	public FiniteDuration SUICIDE_TIMEOUT = Duration.create(10, TimeUnit.SECONDS);
	
	public Cancellable suicide;
	// need to keep track of delivered results, to de-duplicate
	// need to keep the accumulated filters
	

	private ChainedSearchResult( String uuid, Iterable<Promise<Response.SingleResponse>> allPromises, Query query ) {
		this.countPromises = 0;
		for( Promise<Response.SingleResponse> pr: allPromises ) {
			installHandler( pr );
			this.countPromises++;
		}
		this.query = query;
		this.uuid = uuid;
	}
	
	/**
	 * Every Promise gets a handler that adds the results to this ChainedSearchResult.
	 * @param pr
	 */
	public void installHandler( final Promise<Response.SingleResponse> pr ) {
		final ChainedSearchResult thisResult = this;
		
		// each promise needs to add its result to this chain separately and update the filters
		pr.onRedeem( new F.Callback<Response.SingleResponse>() {
			public void invoke(Response.SingleResponse sr) throws Throwable {
				thisResult.collectSourceResponse( sr );
				thisResult.finishedPromises.incrementAndGet();
			}
		} );
		pr.onFailure(new F.Callback<Throwable>() {
			public void invoke(Throwable arg0) throws Throwable {
				thisResult.finishedPromises.incrementAndGet();
			}
		});
	}
	
	/**
	 * When promises for SingleResponse are fullfilled, they are added here to this ChainedSearchResult
	 * @param sr
	 */
	// add this sourceRepsonse to the next return value
	public synchronized void collectSourceResponse( Response.SingleResponse sr ) {
		// if there is a SearchResult promise, fullfill it
		// else just add the response to the ChainedSearchResponse
		currentResponses.add( sr );
		
		if( pendingPromise != null ) {
			Response searchResponse = responseFromCurrent();
			pendingPromise.success( Controller.ok(Json.toJson(searchResponse)));
			pendingPromise = null;
			
			// install new timeout
			suicide = Akka.system().scheduler().scheduleOnce(SUICIDE_TIMEOUT,
					  () -> {
						  ChainedSearchResult.cache.remove( uuid );
					  }, Akka.system().dispatcher());
		}
	}
	
	/**
	 * Create a search.Response from all so far submitted Response.SingleResponse (in currentResponses)
	 * if this is a continuation, it means that 
	 * @return
	 */
	private Response responseFromCurrent() {
		Response nextResponse = new Response();
		nextResponse.query = query;
		nextResponse.continuationId = uuid;
		
		currentResponses.forEach(sr -> {
			nextResponse.addSingleResponse(sr);
		});
		nextResponse.createAccumulated();
		
		if( lastResponse != null ) {
			// need to merge the accumulated values in
			nextResponse.mergeAccumulated(lastResponse);
		}

		// check if we have all responses and add uuid only if we expect more
		if( finishedPromises.get() < countPromises) 
			 nextResponse.continuationId = uuid;
		
		currentResponses.clear();
		lastResponse = nextResponse;
		
		return nextResponse;
	}
	
	// all responses currently added to the class are made into the Result.
	// if there are none, make a promise with the first one
	private synchronized Promise<Result> createSearchResponse() {
		// cancel the timeout functionality
		if( suicide != null ) {
			suicide.cancel();
			suicide = null;
		}
		
		if( currentResponses.size() > 0 ) {
			// respond immediately
			Response sr = responseFromCurrent();
			// install new timeout
			suicide = Akka.system().scheduler().scheduleOnce(SUICIDE_TIMEOUT,
					  () -> {
						  ChainedSearchResult.cache.remove( uuid );
					  }, Akka.system().dispatcher());
			return Promise.pure( Controller.ok(Json.toJson(sr)));
		} else {
			pendingPromise = RedeemablePromise.empty();
			return pendingPromise;
		}
	}
	
	/**
	 * if you have a uuid for the search, this call should get you the next promise.
	 * @param uuid
	 * @return
	 */
	public static Promise<Result> search( String uuid ) {
		// find the pending search
		// does it contain stuff, return it
		// otherwise create a promise to return
		ChainedSearchResult csr = cache.get( uuid );
		if( csr != null ) {
			return csr.createSearchResponse();
		} else {
			return Promise.pure(Controller.badRequest("Unknown UUID for request. Probably expired!"));			
		}
	}
	
	/**
	 * Create the ChainedSearchResult and return the "createSearchResponse" return value
	 * @param promises
	 * @return
	 */
	public static Promise<Result> create( Iterable<Promise<Response.SingleResponse>> promises, Query query ) {
		String uuid = ObjectId.get().toString();
		ChainedSearchResult csr = new ChainedSearchResult(uuid, promises, query);
		ChainedSearchResult.cache.put( uuid, csr);
		return csr.createSearchResponse();
	}
	
	
}
