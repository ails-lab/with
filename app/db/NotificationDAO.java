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

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;

import model.Notification;
import model.Notification.Activity;
import model.Rights.Access;
import play.Logger;
import play.Logger.ALogger;

public class NotificationDAO extends DAO<Notification> {

	public static final ALogger log = Logger.of(NotificationDAO.class);

	public NotificationDAO() {
		super(Notification.class);
	}

	public Notification getById(ObjectId id) {
		Query<Notification> q = this.createQuery().field("_id").equal(id);
		return this.findOne(q);
	}

	public List<Notification> getAllByReceiver(ObjectId receiverId) {
		Query<Notification> q = this.createQuery().field("receiver").equal(receiverId);
		return find(q).asList();
	}

	public List<Notification> getByReceiver(ObjectId receiverId) {
		Query<Notification> q = this.createQuery().field("receiver").equal(receiverId);
		q.and(q.criteria("open").equal(true));
		return find(q).asList();
	}

	public List<Notification> getGroupRelatedNotifications(ObjectId receiverId, ObjectId groupId, Activity activity) {
		Query<Notification> q = this.createQuery().field("receiver").equal(receiverId);
		q.and(q.criteria("open").equal(true), q.criteria("group").equal(groupId),
				q.criteria("activity").equal(activity));
		return find(q).asList();
	}

	public List<Notification> getCollectionRelatedNotifications(ObjectId receiverId, ObjectId collectionId,
			Activity activity, Access access) {
		Query<Notification> q = this.createQuery().field("receiver").equal(receiverId);
		q.and(q.criteria("open").equal(true), q.criteria("collection").equal(collectionId),
				q.criteria("activity").equal(activity), q.criteria("access").equal(access));
		return find(q).asList();
	}

	public List<Notification> getCollectionRelatedNotifications(ObjectId receiverId, ObjectId collectionId,
			Activity activity) {
		Query<Notification> q = this.createQuery().field("receiver").equal(receiverId);
		q.and(q.criteria("open").equal(true), q.criteria("collection").equal(collectionId),
				q.criteria("activity").equal(activity));
		return find(q).asList();
	}
}
