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

import model.User;

import org.apache.commons.lang3.StringUtils;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class UserManager extends Controller {
	public static final ALogger log = Logger.of(UserManager.class);

	/**
	 * Free to call by anybody, so we don't give lots of info.
	 * @param email
	 * @return
	 */
	public static Result findByEmail( String email ) {
		User u = DB.getUserDAO().getByEmail(email);
		if( u != null ) {
			ObjectNode res = Json.newObject();
			res.put( "displayName", u.getDisplayName());
			res.put( "email", u.getEmail());
			return ok( res );
		} else {
			return badRequest();
		}
	}
	
	public static Result findByDisplayName( String displayName ) {
		User u = DB.getUserDAO().getByDisplayName(displayName);
		if( u != null ) {
			ObjectNode res = Json.newObject();
			res.put( "displayName", u.getDisplayName());
			return ok( res );
		} else {
			return badRequest();
		}
	}

	/**
	 * 
	 * @return
	 */
	public static Result register() {
		
		return ok();
		
	}
	
	/**
	 * Should not be needed, is the same as login by email?
	 * Maybe need to store if its a facebook or google or password log inner
	 * @return
	 */
	public static Result findByFacebookId() {
		return ok();
	}
	
	public static Result login( String email, String password, String displayName ) {
		User u = null;
		ObjectNode result = Json.newObject();
		
		if( StringUtils.isNotEmpty(  email )) {
			u = DB.getUserDAO().getByEmail(email);
			if( u == null ) {
				result.put( "error", "Invalid email");
			}
		} else if( StringUtils.isNotEmpty(displayName )) {
			u = DB.getUserDAO().getByDisplayName( displayName );
			if( u== null)  {
				result.put("error","Invalid displayName");
			}
		}

		if( u != null ) {
			// check password
			if( u.checkPassword( password )) {
				session().put( "user", u.getDbId().toHexString());
				// now return the whole user stuff, just for good measure
				result = (ObjectNode) Json.parse( DB.getJson( u ));
				return ok(result);
			} else {
				result.put( "error", "Invalid Password");				
			}
		} else {
			result.put( "error", "Need 'displayName' or 'email' parameter" );
		}
		return badRequest( result );
	}
	
	/**
	 * This action clears the session, the user is logged out.
	 * @return
	 */
	public static Result logout() {
		session().clear();
		return ok();
	}	
}
