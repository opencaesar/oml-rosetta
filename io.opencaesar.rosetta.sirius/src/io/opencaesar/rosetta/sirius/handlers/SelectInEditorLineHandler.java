/**
 * 
 * Copyright 2019-2021 California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package io.opencaesar.rosetta.sirius.handlers;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.sirius.table.metamodel.table.DLine;

import io.opencaesar.rosetta.sirius.utils.UIServices;

/**
 * Open the editor of the first selected line element and select it in the editor
 */
public class SelectInEditorLineHandler extends AbstractLineHandler {

	@Override
	protected void execute(ExecutionEvent event, List<DLine> lines) {
		if (lines.size() > 0) {
			var firstLine = lines.get(0);
			if (firstLine.getSemanticElements().size() > 0) {
				var firstElement = firstLine.getSemanticElements().get(0);
				UIServices.openAndSelectInEditor(firstElement);
			}
		}
	}
}
