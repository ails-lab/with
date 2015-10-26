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

import com.fasterxml.jackson.databind.JsonNode;

import actors.NotificationActor;
import play.Logger;
import play.Logger.ALogger;
import play.mvc.Controller;
import play.mvc.WebSocket;

public class NotificationController extends Controller {
	public static final ALogger log = Logger.of(NotificationController.class);
	
	public static WebSocket<JsonNode> socket() {
	    return WebSocket.withActor(NotificationActor::props);
	}

}
