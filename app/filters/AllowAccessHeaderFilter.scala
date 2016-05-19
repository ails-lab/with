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


/*
 *  Copyright 2015 Daniel W. H. James
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package filters

import scala.concurrent.Future
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Filter
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import scala.collection.immutable
import play.api.http.{ HeaderNames, HttpVerbs, MimeTypes }

/**
 * Add the CORS Header to be able to use API results from other webapps
 */
class AllowAccessHeaderFilter extends Filter {
  val log = Logger(this.getClass())

  private val HttpMethods = {
    import HttpVerbs._
    immutable.HashSet(GET, POST, PUT, PATCH, DELETE, HEAD)
  }

  def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader) = {
    next(rh).map { result => result.withHeaders(HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> "http://localhost:9000", HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> "http://localhost:1337", HeaderNames.ACCESS_CONTROL_ALLOW_METHODS -> HttpMethods.mkString(", "), HeaderNames.ACCESS_CONTROL_ALLOW_HEADERS -> "content-type", HeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true") }
  }

}