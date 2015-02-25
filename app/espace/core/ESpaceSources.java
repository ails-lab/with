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


package espace.core;

import java.util.ArrayList;
import java.util.List;

public class ESpaceSources {

	public static List<ISpaceSource> esources;

	static void init() {
		esources = new ArrayList<ISpaceSource>();
		esources.add(new ESpaceSource());
		esources.add(new DSpaceSource());
		System.out.println("inittttttttttttttttttt");
	}

	public static List<ISpaceSource> getESources() {
		if (esources == null) {
			init();
		}
		return esources;

	}

	public static List<SourceResponse> fillResults(CommonQuery q) {
		ArrayList<SourceResponse> srcs = new ArrayList<SourceResponse>();
		for (ISpaceSource src : ESpaceSources.getESources()) {
			srcs.add(src.getResults(q));
		}
		return srcs;
	}

}
