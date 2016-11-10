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


package org.mupop.model;

/**
 * Created by stabenau on 11/2/16.
 * A class that is Referenceable will get an id in serialization so that it can be
 * included in Groups. If the group has a tag and it is unique, this should be used
 * during serialization.
 * The serializer is otherwise free to choose ids ... they disappear after deserialization
 *
 * It means as well that classes that are not referenceable are meant to be embedded in serialization
 *
 *
 * In order to reference a certain point in an exhibition, its usually not enough to just reference the group
 * you are going to. The group might appear more than once. If you are referencing a point in a sequence or generally
 * a potentially ambiguous object, a path reference is needed. It specifies the object reference you are pointing to,
 * together with the objects that represent how you arrived there.
 *
 * A Path might be needed, when you want to make jumps inside a Sequence (or to different Sequences) possible.
 */
public interface Referenceable {
}
