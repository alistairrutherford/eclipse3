/*
 * Copyright (c) 2014, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.dart.tools.core.analysis.model;

import org.dartlang.analysis.server.protocol.HighlightRegion;

/**
 * Used by {@link AnalysisServerData} to notify clients that new {@link HighlightRegion}s are ready.
 * 
 * @coverage dart.tools.core.model
 */
public interface AnalysisServerHighlightsListener {
  /**
   * Called when {@link HighlightRegion}s for a particular file are ready.
   */
  void computedHighlights(String file, HighlightRegion[] highlights);
}
