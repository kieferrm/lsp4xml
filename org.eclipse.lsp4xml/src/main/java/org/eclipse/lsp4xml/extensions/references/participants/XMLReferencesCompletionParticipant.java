/**
 *  Copyright (c) 2018 Angelo ZERR
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package org.eclipse.lsp4xml.extensions.references.participants;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4xml.dom.Node;
import org.eclipse.lsp4xml.dom.XMLDocument;
import org.eclipse.lsp4xml.extensions.references.XMLReferencesManager;
import org.eclipse.lsp4xml.services.extensions.CompletionParticipantAdapter;
import org.eclipse.lsp4xml.services.extensions.ICompletionRequest;
import org.eclipse.lsp4xml.services.extensions.ICompletionResponse;
import org.eclipse.lsp4xml.utils.XMLPositionUtility;

public class XMLReferencesCompletionParticipant extends CompletionParticipantAdapter {

	@Override
	public void onXMLContent(ICompletionRequest request, ICompletionResponse response) throws Exception {
		int offset = request.getOffset();
		final Node node = getNodeAt(request.getNode(), offset);	
		if (node != null) {			
			XMLReferencesManager.getInstance().collect(node, n -> {
				XMLDocument doc = n.getOwnerDocument();
				Range range = XMLPositionUtility.createRange(node.getStart(), node.getEnd(), doc);
				String oldValue = node.getNodeValue();
				String label = n.getNodeValue();
				CompletionItem item = new CompletionItem();
				item.setLabel(label);
				String insertText = label;
				item.setKind(CompletionItemKind.Property);
				item.setDocumentation(Either.forLeft(label));
				item.setFilterText(insertText);
				item.setTextEdit(new TextEdit(range, insertText));
				item.setInsertTextFormat(InsertTextFormat.PlainText);
				response.addCompletionItem(item);
			});
		}
	}

	private Node getNodeAt(Node node, int offset) {
		if (node == null) {
			return null;
		}
		if (node.hasChildNodes()) {
			for (Node child : node.getChildren()) {
				if (Node.isIncluded(child, offset + 1)) {
					return getNodeAt(child, offset + 1);
				}
			}
		}
		return node;
	}

}
