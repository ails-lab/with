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


package org.mupop.model.group;

/**
 * Created by stabenau on 11/4/16.
 * When you need to point to a Group, in a specific context (because otherwise it would be
 * ambiguous), then use this class.
 */
public class Path extends Group {
    // groups[0] is the actual Group you want to reference,
    // group[1..] are parent contexts
}
