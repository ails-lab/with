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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import model.User;

import org.junit.Test;

import play.twirl.api.Content;


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
    	
    	EntityManagerFactory emf = Persistence.createEntityManagerFactory("with_persist");
    	EntityManager em = emf.createEntityManager();
    	em.persist(testUser);
    	em.close();

    	em = emf.createEntityManager();
    	Query q = em.createQuery("select u from User u where u.name='Tester'");
    	com.impetus.kundera.query.Query kq = (com.impetus.kundera.query.Query) q;
    	kq.iterate()
    	List<?> userList = q.getResultList();
    	assertThat( userList.size()).isEqualTo(1);
    	em.close();
    	emf.close();   	
    }

    @Test
    public void renderTemplate() {
        Content html = views.html.index.render("Your new application is ready.");
        assertThat(contentType(html)).isEqualTo("text/html");
        assertThat(contentAsString(html)).contains("Your new application is ready.");
    }


}
