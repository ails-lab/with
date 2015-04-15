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


package controllers;

import static akka.pattern.Patterns.ask;
import model.ApiKey;
import model.ApiKey.AccessResponse;
import play.libs.Akka;
import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Result;
import actors.ApiKeyManager;
import akka.actor.ActorSelection;

public class CallAllowedCheck extends Action.Simple {

	@Override
	public Promise<Result> call(Context ctx) throws Throwable {
		// long reqSize = ctx.request().body();
		String[] apikeys = ctx.request().queryString().get( "apikey");
		ApiKeyManager.ApiAccess access = new ApiKeyManager.ApiAccess();
		if( apikeys != null ) {
			access.apikey = apikeys[0];
		} else if( ctx.session().containsKey("apikey")) {
			access.apikey = ctx.session().get( "apikey" );
		} else {
			// check if ip is allowed
			access.ip = ctx.request().remoteAddress();
		}
		access.call = ctx.request().path();

		ActorSelection api = Akka.system().actorSelection("user/apiKeyManager"); 
	
		return Promise.wrap(ask(api, access, 1000))
				.flatMap((Object response) -> {
					AccessResponse r;
					if( response instanceof AccessResponse ) { 
						r = (AccessResponse) response;
						if( r == AccessResponse.ALLOWED )
							return delegate.call(ctx);
					} 
					return Promise.pure( (Result) badRequest(response.toString()));
				});
	}

}
