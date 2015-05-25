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


package general;

import java.util.Date;

import model.ApiKey;

import org.junit.Test;

import db.DB;

public class Temp {


	@Test
	public void test1() {
		// create an ApiKey
		ApiKey k = new ApiKey();
		// should cover localhost
		k.setIpPattern("((0:){7}.*)|(127.0.0.1)");
		// set it to expired
		k.setExpires(new Date(new Date().getTime()));
		k.addCall(0, ".*");

		// store it
		DB.getApiKeyDAO().makePermanent(k);
	}
}
