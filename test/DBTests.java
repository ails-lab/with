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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.Query;

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
    	testUser.name = "Tester";
 
    	DB.getUserDAO().persist( testUser );
    	DB.flush();

    	List<User> l = DB.getUserDAO().list( "obj.name = 'Tester' ");
    	assertThat( l.size()).isGreaterThanOrEqualTo(1);
    	
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
