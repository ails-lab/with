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

import java.util.List;

/**
 * Created by stabenau on 11/3/16.
 * If you use a sequence, the thing that should be displayed in sequence is in the groups 
 * attribute. All other attributes are interpreted as representing this sequence. 
 * 
 * This could mean, that title and description is a headerslide for the groups inside, it could
 * mean that possible ambience elements are shown on every sub group. Its the players choice.
 * If you want a split into chapters, the recommended structure is to use a Sequence of Sequences.
 * 
 */
public class Sequence extends Group {

    // like is autoplay an option? Or do we need next events to proceed
    // if it can autoplay, the player should have some setup for delay
    // some groups will stop the flow, because they want an answer to a question
    public boolean canAutoplay;
    
    //
    // Convenience Methods below
    //
    
    public static Sequence create() { return new Sequence(); }
}
