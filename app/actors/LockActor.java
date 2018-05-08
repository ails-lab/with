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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import play.Logger.ALogger;
import play.Logger;
import utils.Locks;
import utils.Locks.ReleaseLock;
import utils.Locks.RequestLock;

/**
 * Primitive Locking Actor (no timeouts, no security for failed message delivery)
 * Receive RequestLocks request and grants them or queues them
 * Receive ReleaseLocks request and releases Locks and tries waiting ones to grant
 * 
 * Locks work like in Locks.class described. Locked object is a string, Locks.lockId needs to be universal unique.
 * Locks can only be granted together.  
 * @author Arne Stabenau
 *
 */

public class LockActor extends UntypedActor {
	public static final ALogger log = Logger.of( LockActor.class);


	public Set<String> objectsWriteLocked = new HashSet<String>();
	public HashMap<String, Set<String>> objectsReadOwners = new HashMap<>(); 
	
	public List<Pair<Locks, ActorRef>> waitingLocks = new ArrayList<>();
	public Map<String, Pair<Locks, ActorRef>> grantedLocks = new HashMap<String, Pair<Locks,ActorRef>>();
	
	@Override
	public void onReceive(Object msg) throws Exception {
		if( msg instanceof Locks.RequestLock ) {
			RequestLock rl = (RequestLock) msg;
			if( checkLocks( rl.locks )) {
				aquireLocks(rl.locks );
				sender().tell( new Locks.GrantLock(rl.locks.lockId), self()); 
				grantedLocks.put( rl.locks.lockId, Pair.of(rl.locks, sender()));
			} else {
				waitingLocks.add( Pair.of( rl.locks, sender()));
			}
		} else if ( msg instanceof Locks.ReleaseLock) {
			ReleaseLock rl = (ReleaseLock) msg;
			Pair<Locks,ActorRef> lockP = grantedLocks.get( rl.lockId);
			if( lockP == null ) {
				log.info( "Received a release of a lock that is not hold.");
			} else {
				releaseLocks(lockP.getLeft());
			}
			// see if any waiting on locks can receive a grant.
			tryWaiting();
		}
		
	}
	
	// there is no concurrency in this, for good or bad :-(
	
	/**
	 * Can this Locks aquire?
	 * @param l
	 * @return
	 */
	public boolean checkLocks( Locks locks ) {
		for( Locks.Lock lock: locks.locks ) {
			if( objectsWriteLocked.contains( lock.lockedObject)) return false;
			if( lock.write) {
				if( objectsReadOwners.containsKey(lock.lockedObject)) return false;
			}
		}
		return true;
	}
	
 	public Locks aquireLocks( Locks locks ) {
		for( Locks.Lock lock: locks.locks ) {
			if( lock.write) {
				objectsWriteLocked.add( lock.lockedObject );
			} else {
				Set<String> owners = objectsReadOwners.get( lock.lockedObject );
				if( owners == null ) {
					owners = new HashSet<String>();
					objectsReadOwners.put( lock.lockedObject, owners );
				}
				owners.add( locks.owner);
			}
		}
		locks.acquired = new Date();
		return locks;
 	}
 	
 	
 	/**
 	 * Update internal structures for this locks release
 	 * @param locks
 	 */
 	public void releaseLocks( Locks locks ) {
		for( Locks.Lock lock: locks.locks ) {
			if( lock.write) {
				objectsWriteLocked.remove(lock.lockedObject);
			} else {
				objectsReadOwners.get( lock.lockedObject ).remove( locks.owner );
				if( objectsReadOwners.get(lock.lockedObject).isEmpty() ) 
					objectsReadOwners.remove(lock.lockedObject);
			}
		}
 	}
 	
 	/**
 	 * Iterate over waiting locks, grant them and remove from waiting list
 	 */
 	public void tryWaiting() {
 		Iterator<Pair<Locks, ActorRef>> waitingIter = waitingLocks.iterator();
 		while( waitingIter.hasNext()) {
 			Pair<Locks, ActorRef> pair = waitingIter.next();
 			if( checkLocks( pair.getLeft())) {
				aquireLocks( pair.getLeft());
				pair.getRight().tell( new Locks.GrantLock(pair.getLeft().lockId), self()); 
				grantedLocks.put( pair.getLeft().lockId, pair );
				waitingIter.remove();
 			}
 		}
 	}
}
