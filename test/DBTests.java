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


import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import model.Search;
import model.User;

import org.junit.Test;

import play.twirl.api.Content;
import db.DB;


/**
*
* Simple (JUnit) tests that can call all parts of a play app.
* If you are interested in mocking a whole application, see the wiki for more details.
*
*/
public class DBTests {

    @Test
    public void userStorage() {
    	User testUser= new User();
    	testUser.setName("test_user");
    	List<Search> searchHistory = new ArrayList<Search>();
    	Search s1 = new Search();
    	s1.setSearchDate(new Date());
    	searchHistory.add(s1);
    	Search s2 = new Search();
    	s2.setSearchDate(new Date());
    	searchHistory.add(s2);
    	testUser.setSearcHistory(searchHistory);

    	DB.initialize();
    	DB.getDs().ensureIndexes(User.class);
    	//DB.getDs().ensureIndexes(User.class, false);
    	DB.getDs().save(testUser);

    	List<User> l = DB.getUserDAO().listByName("test_user");
    	assertThat( l.size()).isGreaterThanOrEqualTo(1);
    	assertThat( l.size()).isEqualTo(0);
    	
//    	int count = DB.getUserDAO().removeAll("obj.name='Tester'" );
//    	assertThat( count )
//    	.overridingErrorMessage("Not removed enough Testers")
//    	.isGreaterThanOrEqualTo(1 );
    }

    @Test
    public void renderTemplate() {
        Content html = views.html.index.render("Your new application is ready.");
        assertThat(contentType(html)).isEqualTo("text/html");
        assertThat(contentAsString(html)).contains("Your new application is ready.");
    }


}
