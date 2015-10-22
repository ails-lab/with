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


package espace.core.sources;

public enum RightsValues {
	Public("Attribution Alone"), Restricted("Restricted"),
	Permission("Permission"), Modify("Allow re-use and modifications"),
	Commercial("Allow re-use for commercial"),
	Creative_Commercial_Modify("use for commercial purposes modify, adapt, or build upon"),
	Creative_Not_Commercial("NOT Comercial"),
	Creative_Not_Modify("NOT Modify"),
	Creative_Not_Commercial_Modify("not modify, adapt, or build upon, not for commercial purposes"),
	Creative_SA("share alike"),
	Creative_BY("use by attribution"),
	Creative("Allow re-use"),
	RR("Rights Reserved"),
	RRPA("Rights Reserved - Paid Access"),
	RRRA("Rights Reserved - Restricted Access"),
	RRFA("Rights Reserved - Free Access"),
	UNKNOWN("Unknown");

	
	private final String text;

    private RightsValues(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    };
	
	

}
