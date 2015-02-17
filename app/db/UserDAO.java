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


package db;

import java.util.List;

import model.User;

import org.mongodb.morphia.query.Query;

import play.Logger;
import play.Logger.ALogger;

public class UserDAO extends DAO<User> {
	public static final ALogger log = Logger.of( UserDAO.class);

	public UserDAO() {
		super( User.class );
	}

	public User getByEmail(String email) {
		return this.getDs().find(User.class, "email", email).get();
	}

	public User getByLogin(String name) {
		return this.getDs().find(User.class, "name", name).get();
	}

	public User getByLoginPassword(String name, String pass) {
		Query<User> q = DB.getDs().createQuery(User.class);
		q.and(
			q.criteria("name").equal(name),
			q.criteria("password").equal(pass)
		);
		return find(q).get();
	}

	public List<User> listByName( String name ) {
		return list("name", name);
	}
}
