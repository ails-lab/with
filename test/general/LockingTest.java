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


import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;


import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.running;

import org.junit.Test;

import utils.Locks;

public class LockingTest {
	public static long HOUR = 3600000;

	static class Flag {
		boolean flag;
		public Flag( boolean flag ) {
			this.flag = flag;
		}
		public boolean is() {
			return flag;
		}
		public void set( boolean flag ) {
			this.flag = flag;
		}
	}
	@Test
	public void testLocks() {
		running( fakeApplication(), new Runnable() {
			@Override
			public void run() {
				final Flag gotLock = new Flag( false );
				
				try {
					Locks l = Locks.create()
							.read( "Obj #1")
							.read( "Obj #2")
							.write("Obj #3")
							.acquire();
					assertThat(l.acquired!=null );

					Locks l2 = Locks.create()
							.read( "Obj #1")
							.read( "Obj #2")
							.acquire();

					assertThat(l2.acquired!=null );

					Thread lockMe = new Thread( new Runnable() {
						public void run() {
							try {
							Locks l = Locks.create()
									.read( "Obj #1")
									.write( "Obj #2")
									.acquire();
							// now we are locked for a bit
							gotLock.set( true );
							} catch( Exception e ) {}
							finally {
								l.release();
							}
						}
					});
					
					assertThat( lockMe.getState() != Thread.State.RUNNABLE );
					l.release();
					assertThat( lockMe.getState() != Thread.State.RUNNABLE );
					l2.release();
					try {
						lockMe.join( 1000 );
						assertThat( gotLock.is() );
					} catch( Exception e ) {
						fail( "Lock not acquired ");
					}
					
				} catch( Exception e) {
					// nothing
				}
			}
		} );
	}
}
