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


package model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Transient;

import play.Logger;
import play.Logger.ALogger;

/**
 * 
 * @author stabenau An API key limits the calls a client can do. It can allow
 *         all. It can give you automated login for the proxy user.
 * 
 * 
 */
@Entity
public class ApiKey {
	public static final ALogger log = Logger.of(ApiKey.class);

	public static enum Response {
		// INVALID_xxxx have to be checked somewhere else
//		INVALID_IP, EXPIRED_IP, INVALID_APIKEY, EXPIRED_APIKEY, BLOCKED_CALL, COUNT_LIMIT_REACHED, VOLUME_LIMIT_REACHED, ALLOWED
		EMPTY_APIKEY, INVALID_APIKEY, EXPIRED_APIKEY, BLOCKED_CALL, COUNT_LIMIT_REACHED, VOLUME_LIMIT_REACHED, ALLOWED	
	}

	// a call limit is an api call that can be executed with this key
	// for a limited number of times
	// or with limited transfer volume
	public static class CallLimit implements Cloneable {
		public long counter = 0l;
		public long volume = 0l;
		public long volumeLimit = -1l;
		public long counterLimit = -1l;
		public String regexp;

		// this shouldn't be stored in the db
		@Transient
		public Pattern pattern;

		// give back copies of this ...
		public CallLimit clone() {
			try {
				CallLimit cl = (CallLimit) super.clone();
				cl.pattern = null;
				return cl;
			} catch (CloneNotSupportedException ce) {
				return null;
			}
		}

		public Pattern getPattern() {
			if (pattern == null) {
				pattern = Pattern.compile(regexp);
			}
			return pattern;
		}
	}

	@Id
	private ObjectId dbId;

	// the random key string
	private String keyString;

//	// a pattern of IP numbers, if matched, this key applies
//	// usually just for counting
//	private String ipPattern;

	// optional, when a call comes with this key, it has the rights of
	// this user (and others, if login is an allowed call)
	private ObjectId proxyUserId;
	
	// optional, an email account associated with this API key
	private String email;
	
	private ArrayList<CallLimit> callLimits = new ArrayList<CallLimit>();

	private Date created;

	// API keys can have limited lifetime
	private Date expires;

	// api keys are nicely accessed by name, like "WITH" or "Workshop Athens"
	private String name;
	
	// if the origin is set, requests from browsers have to have this origin
	private String origin;
	
	// this will just monitor the pattern
	public CallLimit addCall(int position, String pattern) {
		return addCall(position, pattern, -1l, -1l);
	}

	public CallLimit addCall(int position, String pattern, long counterLimit,
			long volumeLimit) {
		CallLimit cl = new CallLimit();
		cl.counterLimit = counterLimit;
		cl.volumeLimit = volumeLimit;
		cl.regexp = pattern;
		cl.pattern = Pattern.compile(pattern);

		callLimits.add(position, cl);
		return cl;
	}

	/**
	 * Generates new keyString.
	 */
	public void resetKey() {
		final char[] allChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
				.toCharArray();
		keyString = "";
		Random r = new Random();
		for (int i = 0; i < 30; i++)
			keyString += allChars[r.nextInt(allChars.length)];
	}

	/**
	 * Return cloned list off the limits for this API key
	 * 
	 * @param call
	 * @return
	 */
	public List<CallLimit> getLimits() {
		List<CallLimit> res = new ArrayList<CallLimit>();
		for (CallLimit cl : callLimits) {
			res.add(cl.clone());
		}
		return res;
	}

	private CallLimit getLimit(String call) {
		for (CallLimit cl : callLimits) {
			if (cl.getPattern().matcher(call).matches())
				return cl;
			return cl;
		}
		return null;
	}

	/**
	 * Checks if the API key allows for the call to happen and counts it in.
	 * 
	 * @param call
	 * @param volume
	 * @return
	 */
	public Response check(String call, long volume) {
		if (expires != null) {
			if (new Date().after(expires)) {
//				if (StringUtils.isEmpty(ipPattern)) {
					return Response.EXPIRED_APIKEY;
//				} else {
//					return Response.EXPIRED_IP;
//				}
			}
		}

		CallLimit cl = getLimit(call);
		if (cl == null) {
			return Response.BLOCKED_CALL;
		}

		Response res = null;

		// check the limits
		if ((volume > 0) && (cl.volumeLimit >= 0)
				&& (cl.volume + volume > cl.volumeLimit)) {
			res = Response.VOLUME_LIMIT_REACHED;
		}
		if ((cl.counterLimit >= 0) && (cl.counter + 1 > cl.counterLimit)) {
			res = Response.COUNT_LIMIT_REACHED;
		}

		if (res == null) {
			cl.counter += 1;
			cl.volume += volume;
			return Response.ALLOWED;
		} else {
			return res;
		}
	}

	/*
	 * If you only know the volume after the call use this to update the counter
	 */
	public void updateVolume(String call, long volume) {
		CallLimit cl = getLimit(call);
		if (cl != null)
			cl.volume += volume;
	}

	// getter setter section

	public ObjectId getDbId() {
		return dbId;
	}

	public void setDbId(ObjectId dbId) {
		this.dbId = dbId;
	}

	public String getKeyString() {
		return keyString;
	}

	public void setKeyString(String keyString) {
		this.keyString = keyString;
	}

	public ObjectId getProxyUserId() {
		return proxyUserId;
	}

	public void setProxyUserId(ObjectId proxyUserId) {
		this.proxyUserId = proxyUserId;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getExpires() {
		return expires;
	}

	public void setExpires(Date expires) {
		this.expires = expires;
	}

//	public String getIpPattern() {
//		return ipPattern;
//	}

//	public void setIpPattern(String ipPattern) {
//		this.ipPattern = ipPattern;
//	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

}
