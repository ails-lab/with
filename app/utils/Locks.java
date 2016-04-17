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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.bson.types.ObjectId;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.UntypedActor;
import db.DB;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Akka;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
/**
 * Simplified Lock implementation
 * Read lock can be acquired on an object if there is no write lock on it
 * Write lock can be acquired if there are no other locks on the object
 * Locks have to be acquired together or the caller will wait
 * Locks can not be re-acquired, there is no counter!!
 * All request locks need to be acquired at once, so if you need extra locks, you should release and re-acquire
 * 
 * Acquire Locks like this
 * Locks myLock = Locks.owner( "doesnt really matter, choose thread id maybe" )
 *  .read( "record #xxxx" )
 *  .write( "collection #xxxx )
 *  .acquire()
 *  
 * As a convenience, if an object implement Lockable .getLockname(), you can give the object as parameter to read and write 
 * (not a made up string)
 *  
 * @author Arne Stabenau
 *
 */
public class Locks implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1891801861567135813L;
	
	public static final ALogger log = Logger.of(Locks.class);
	public static class Lock {
		public String lockedObject;
		public boolean write; 
	}
	
	// here the threads wait for the locks, so far no timeout
	public static HashMap<String, Semaphore> threadPark = new HashMap<String, Semaphore>();

	/**
	 * The proxy actor is kinda private to the Locks class. It serves like an Inbox that can do callbacks on 
	 * Locks class.
	 */
	public static ActorRef proxy;
	public static ActorRef globalLockManagerActorRef;
	
	static {
		proxy = Akka.system().actorOf( Props.create( ProxyLocks.class ), "proxyLocks");
	};
	
	
	// need to get this on startup from Application
	public static void setLockManagerActorRef(ActorRef lockManager) {
		globalLockManagerActorRef = lockManager;
	}
	

	
	public interface Lockable {
		public String getLockname();
	}
	
	public List<Lock> locks = new ArrayList<Lock>();
	public Date acquired;
	public String owner;
	
	// helps with identifying the thing after network messages
	public String lockId;
	
	
	// user side of things
	
	private Locks() {
		lockId = ObjectId.get().toString();
	}
	
	public static Locks owner( String owner ) {
		Locks res = new Locks();
		res.owner = owner;
		return res;
	}
 
	/**
	 * System selects a UUID for this locks object as owner
	 * @return
	 */
	public static Locks create() {
		String uuid = ObjectId.get().toHexString();
		return owner( uuid );
	}
	
	private Locks addLock( String object, boolean write ) {
		Lock l = new Lock();
		l.lockedObject = object;
		l.write = write;
		locks.add( l );
		return this;		
	}
	public Locks read( String object ) {
		return addLock( object, false );
	}
	
	public Locks write( String object ) {
		return addLock( object, true );
	}
	
	public Locks read( Lockable object ) {
		return addLock( object.getLockname(), false );
	}
	
	public Locks write( Lockable object ) {
		return addLock( object.getLockname(), true );
	}
	
	public Locks acquire() throws Exception {
		// wait some to get the lock
		// either return this or throw
		
		if( DB.getConf().getBoolean("locks.disabled")) return this;
		Semaphore waitHere = new Semaphore(0);
		threadPark.put( lockId, waitHere);	
		try {
			proxy.tell( new RequestLock( this), ActorRef.noSender());
			waitHere.acquire();
			this.acquired = new Date();
		} catch(Exception e){
			log.info( "Exception while waiting for lock", e );
			throw new Exception( "Failed to get the lock!!");
		} finally {
			// thread not parked any more
			threadPark.remove(lockId);
		}
		return this;
	}
	
	public Locks acquire(int millis ) throws Exception {
		// wait some to get the lock
		// either return this or throw
		Semaphore waitHere = new Semaphore(0);
		threadPark.put( lockId, waitHere);	
		try {
			proxy.tell( new RequestLock( this), ActorRef.noSender());
			boolean success = waitHere.tryAcquire(millis, TimeUnit.MILLISECONDS );
			if( !success) throw new Exception();
			this.acquired = new Date();
		} catch(Exception e){
			// I don't hold the lock ...
			// it could happen that there is a second release send for this lock, this should not 
			// be a problem
			proxy.tell( new ReleaseLock(lockId), ActorRef.noSender());
			
			log.info( "Exception while waiting for lock", e );
			throw new Exception( "Failed to get the lock!!");
			
		} finally {
			// thread not parked any more
				threadPark.remove(lockId);		
		}
		return this;
	}
	
	
	
	/**
	 * Always succeeds ... whether it always actually releases the locks is up to the 
	 * correctness of the code to come
	 */
	public Locks release() {
		proxy.tell( new ReleaseLock(lockId), ActorRef.noSender());
		return this;
	}

	// 
	// Here is, how its working with the actors
	//
	

	/**
	 * This method is not meant to be called from outside. 
	 * @param lockId
	 */
	// release the thread waiting for this lock
	// if its already timed out maybe log the fact that the grant came to late
	private static void grant( String lockId ) {
		Semaphore s = threadPark.get( lockId );
		if( s != null ) {
			s.release();
		} else {
			log.warn( "Got a lock that nobody wanted ... ");
			// need to release it
			proxy.tell( new ReleaseLock(lockId), ActorRef.noSender());
		}
	}
	
	// some messages to talk to the LocksActor
	public static class RequestLock {
		public Locks locks;
		public RequestLock( Locks locks ) {
			this.locks = locks;
		}
	}
	
	public static class ReleaseLock {
		public String lockId;
		public ReleaseLock( String lockId ) {
			this.lockId = lockId;
		}
	}
	
	public static class GrantLock {
		public String lockId;
		public GrantLock( String lockId ) {
			this.lockId = lockId;
		}
	}
	
	// proxy actor talks to the main actor and call the local Locks method to grant locks and 
	// release threads from prison. 
	public static class ProxyLocks extends UntypedActor {

		ActorRef mainLockActor;
		
		@Override
		public void onReceive(Object msg) throws Exception {
			if( msg instanceof GrantLock ) {
				String lockId = ((GrantLock)msg).lockId;
				Locks.grant( lockId );
			} else 
				Locks.globalLockManagerActorRef.tell(msg, getSelf());
		}	
	}	
}
