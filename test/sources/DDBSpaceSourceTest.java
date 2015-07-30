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


package sources;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

import espace.core.CommonQuery;
import espace.core.sources.DDBSpaceSource;

/**
 *
 * Simple (JUnit) tests that can call all parts of a play app. If you are
 * interested in mocking a whole application, see the wiki for more details.
 *
 */
public class DDBSpaceSourceTest {

	@Test
	public void renderTemplate() {
		DDBSpaceSource src = new DDBSpaceSource();
		CommonQuery q = new CommonQuery();
		q.searchTerm = "Romanorum et Germanorum";
		System.out.println(src.getResults(q));
	}

}
