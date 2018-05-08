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


package actors;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;

import play.libs.Json;
import akka.actor.UntypedActor;
import db.DB;
import model.usersAndGroups.User;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TokenLoginActor extends UntypedActor {

	private static int TOKENTIMEOUT = 10000; // milisec

	private static List<JsonNode> tokenIndex;

	@Override
	public void preStart() throws Exception {

		tokenIndex = new ArrayList<JsonNode>();

		super.preStart();
	}

	public static class GetUserIdMessage {
		public long token;

		public GetUserIdMessage(String token) {
			this.token = Long.parseLong(token);
		}

		public GetUserIdMessage(long token) {
			this.token = token;
		}

	}

	public static class UserIdResponseMessage {
		public ObjectId userId;

		public UserIdResponseMessage(String userId) {
			this.userId = new ObjectId(userId);
		}

		public ObjectId getUserId() {
			return userId;
		}

		public String getStringUserId() {
			return userId.toString();
		}

	}

	public static class TokenCreateMessage {
		public ObjectId userId;

		public TokenCreateMessage(ObjectId userId) {
			this.userId = userId;
		}

	}

	public static class TokenResponseMessage {
		public long token;

		public TokenResponseMessage(long token) {
			this.token = token;
		}

	}

	@Override
	public void onReceive(Object msg) throws Exception {
		// Use this message to get the userId of a token
		if (msg instanceof GetUserIdMessage) {
			GetUserIdMessage message = (GetUserIdMessage) msg;
			sender().tell(new UserIdResponseMessage(parseToken(message.token)), getSelf());

		}

		// Use this message to get a new (or existing) token, from a userId
		if (msg instanceof TokenCreateMessage) {
			TokenCreateMessage message = (TokenCreateMessage) msg;
			sender().tell(new TokenResponseMessage(createToken(message.userId)), getSelf());
		}
	}

	private static void cleanIndex() {
		long date = new Date().getTime();
		for (JsonNode node : tokenIndex) {
			long timestamp = node.get("timestamp").asLong();

			if (date > (timestamp + (TOKENTIMEOUT * 360 * 24 /*
																 * 24 hours
																 */))) {
				tokenIndex.remove(node);
			}
		}
	}

	private static long createToken(ObjectId id) {

//		for (JsonNode node : tokenIndex) {
//			if (StringUtils.equals(node.get("user").asText(), id.toString()))
//				return node.get("token").asLong();
//		}

		ObjectNode node = Json.newObject();
		node.put("user", id.toString());
		node.put("timestamp", new Date().getTime());
		// should all be different
		long token = Math.abs(new Random().nextLong());
		node.put("token", token);
		tokenIndex.add(node);
		return token;
	}

	private static String parseToken(long token) {

		for (JsonNode node : tokenIndex) {
			if (node.get("token").asLong() == token) {

				long timestamp = node.get("timestamp").asLong();

				if (new Date().getTime() > (timestamp
						+ (TOKENTIMEOUT * 360 * 24 /*
													 * 24 hours
													 */))) {
					cleanIndex();
					return "token expired";

				}

				return node.get("user").asText();

			}
		}

		return "token does not exist";

	}

}

