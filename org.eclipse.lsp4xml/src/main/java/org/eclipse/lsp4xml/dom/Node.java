/**
 *  Copyright (c) 2018 Angelo ZERR.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package org.eclipse.lsp4xml.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;

/**
 * XML node.
 *
 */
public abstract class Node implements org.w3c.dom.Node {

	boolean closed = false;

	private XMLNamedNodeMap<Attr> attributeNodes;
	private XMLNodeList<Node> children;

	final int start;
	int end;

	Node parent;
	private final XMLDocument ownerDocument;

	class XMLNodeList<T extends Node> extends ArrayList<T> implements NodeList {

		private static final long serialVersionUID = 1L;

		@Override
		public int getLength() {
			return super.size();
		}

		@Override
		public Node item(int index) {
			return super.get(index);
		}

	}

	class XMLNamedNodeMap<T extends Node> extends ArrayList<T> implements NamedNodeMap {

		private static final long serialVersionUID = 1L;

		@Override
		public int getLength() {
			return super.size();
		}

		@Override
		public T getNamedItem(String name) {
			for (T node : this) {
				if (name.equals(node.getNodeName())) {
					return node;
				}
			}
			return null;
		}

		@Override
		public T getNamedItemNS(String name, String arg1) throws DOMException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public T item(int index) {
			return super.get(index);
		}

		@Override
		public T removeNamedItem(String arg0) throws DOMException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public T removeNamedItemNS(String arg0, String arg1) throws DOMException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public T setNamedItem(org.w3c.dom.Node arg0) throws DOMException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public T setNamedItemNS(org.w3c.dom.Node arg0) throws DOMException {
			// TODO Auto-generated method stub
			return null;
		}

	}

	public Node(int start, int end, XMLDocument ownerDocument) {
		this.start = start;
		this.end = end;
		this.ownerDocument = ownerDocument;
	}

	public XMLDocument getOwnerDocument() {
		return ownerDocument;
	}

	@Override
	public String toString() {
		return toString(0);
	}

	private String toString(int indent) {
		StringBuilder result = new StringBuilder("");
		for (int i = 0; i < indent; i++) {
			result.append("\t");
		}
		result.append("{start: ");
		result.append(start);
		result.append(", end: ");
		result.append(end);
		result.append(", name: ");
		result.append(getNodeName());
		result.append(", closed: ");
		result.append(closed);
		if (children != null && children.size() > 0) {
			result.append(", \n");
			for (int i = 0; i < indent + 1; i++) {
				result.append("\t");
			}
			result.append("children:[");
			for (int i = 0; i < children.size(); i++) {
				Node node = children.get(i);
				result.append("\n");
				result.append(node.toString(indent + 2));
				if (i < children.size() - 1) {
					result.append(",");
				}
			}
			result.append("\n");
			for (int i = 0; i < indent + 1; i++) {
				result.append("\t");
			}
			result.append("]");
			result.append("\n");
			for (int i = 0; i < indent; i++) {
				result.append("\t");
			}
			result.append("}");
		} else {
			result.append("}");
		}
		return result.toString();
	}

	public Node findNodeBefore(int offset) {
		List<Node> children = getChildren();
		int idx = findFirst(children, c -> offset <= c.start) - 1;
		if (idx >= 0) {
			Node child = children.get(idx);
			if (offset > child.start) {
				if (offset < child.end) {
					return child.findNodeBefore(offset);
				}
				Node lastChild = child.getLastChild();
				if (lastChild != null && lastChild.end == child.end) {
					return child.findNodeBefore(offset);
				}
				return child;
			}
		}
		return this;
	}

	public Node findNodeAt(int offset) {
		List<Node> children = getChildren();
		int idx = findFirst(children, c -> offset <= c.start) - 1;
		if (idx >= 0) {
			Node child = children.get(idx);
			if (isIncluded(child, offset)) {
				return child.findNodeAt(offset);
			}
		}
		return this;
	}

	/**
	 * Returns true if the node included the given offset and false otherwise.
	 * 
	 * @param node
	 * @param offset
	 * @return true if the node included the given offset and false otherwise.
	 */
	public static boolean isIncluded(Node node, int offset) {
		if (node == null) {
			return false;
		}
		return isIncluded(node.start, node.end, offset);
	}

	public static boolean isIncluded(int start, int end, int offset) {
		return offset > start && offset <= end;
	}

	public Attr findAttrAt(int offset) {
		Node node = findNodeAt(offset);
		return findAttrAt(node, offset);
	}

	public Attr findAttrAt(Node node, int offset) {
		if (node != null && node.hasAttributes()) {
			for (Attr attr : node.getAttributeNodes()) {
				if (attr.isIncluded(offset)) {
					return attr;
				}
			}
		}
		return null;
	}

	/**
	 * Takes a sorted array and a function p. The array is sorted in such a way that
	 * all elements where p(x) is false are located before all elements where p(x)
	 * is true.
	 * 
	 * @returns the least x for which p(x) is true or array.length if no element
	 *          full fills the given function.
	 */
	private static <T> int findFirst(List<T> array, Function<T, Boolean> p) {
		int low = 0, high = array.size();
		if (high == 0) {
			return 0; // no children
		}
		while (low < high) {
			int mid = (int) Math.floor((low + high) / 2);
			if (p.apply(array.get(mid))) {
				high = mid;
			} else {
				low = mid + 1;
			}
		}
		return low;
	}

	public Attr getAttributeNode(String name) {
		if (!hasAttributes()) {
			return null;
		}
		for (Attr attr : attributeNodes) {
			if (name.equals(attr.getName())) {
				return attr;
			}
		}
		return null;
	}

	public String getAttribute(String name) {
		Attr attr = getAttributeNode(name);
		String value = attr != null ? attr.getValue() : null;
		if (value == null) {
			return null;
		}
		if (value.isEmpty()) {
			return value;
		}
		// remove quote
		char c = value.charAt(0);
		if (c == '"' || c == '\'') {
			if (value.charAt(value.length() - 1) == c) {
				return value.substring(1, value.length() - 1);
			}
			return value.substring(1, value.length());
		}
		return value;
	}

	public boolean hasAttribute(String name) {
		return hasAttributes() && getAttributeNode(name) != null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.w3c.dom.Node#hasAttributes()
	 */
	@Override
	public boolean hasAttributes() {
		return attributeNodes != null && attributeNodes.size() != 0;
	}

	public void setAttribute(String name, String value) {
		Attr attr = getAttributeNode(name);
		if (attr == null) {
			attr = new Attr(name, this);
			setAttributeNode(attr);
		}
		attr.setValue(value, -1, -1);
	}

	public void setAttributeNode(Attr attr) {
		if (attributeNodes == null) {
			attributeNodes = new XMLNamedNodeMap<Attr>();
		}
		attributeNodes.add(attr);
	}

	public List<Attr> getAttributeNodes() {
		return attributeNodes;
	}

	/**
	 * Returns the node children.
	 * 
	 * @return the node children.
	 */
	public List<Node> getChildren() {
		if (children == null) {
			return Collections.emptyList();
		}
		return children;
	}

	/**
	 * Add node child
	 * 
	 * @param child the node child to add.
	 */
	public void addChild(Node child) {
		child.parent = this;
		if (children == null) {
			children = new XMLNodeList<Node>();
		}
		getChildren().add(child);
	}

	/**
	 * Returns node child at the given index.
	 * 
	 * @param index
	 * @return node child at the given index.
	 */
	public Node getChild(int index) {
		return getChildren().get(index);
	}

	public boolean isClosed() {
		return closed;
	}

	public Element getParentElement() {
		Node parent = getParentNode();
		while (parent != null && parent != getOwnerDocument()) {
			if (parent.isElement()) {
				return (Element) parent;
			}
		}
		return null;
	}

	public boolean isComment() {
		return getNodeType() == Node.COMMENT_NODE;
	}

	public boolean isProcessingInstruction() {
		return (getNodeType() == Node.PROCESSING_INSTRUCTION_NODE)
				&& ((ProcessingInstruction) this).isProcessingInstruction();
	}

	public boolean isProlog() {
		return (getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) && ((ProcessingInstruction) this).isProlog();
	}

	public boolean isCDATA() {
		return getNodeType() == Node.CDATA_SECTION_NODE;
	}

	public boolean isDoctype() {
		return getNodeType() == Node.DOCUMENT_TYPE_NODE;
	}

	public boolean isElement() {
		return getNodeType() == Node.ELEMENT_NODE;
	}

	public boolean isAttribute() {
		return getNodeType() == Node.ATTRIBUTE_NODE;
	}

	public boolean isText() {
		return getNodeType() == Node.TEXT_NODE;
	}

	public boolean isCharacterData() {
		return isCDATA() || isText() || isProcessingInstruction() || isComment();
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#getLocalName()
	 */
	@Override
	public String getLocalName() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#getParentNode()
	 */
	@Override
	public Node getParentNode() {
		return parent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#getFirstChild()
	 */
	@Override
	public Node getFirstChild() {
		return this.children != null && children.size() > 0 ? this.children.get(0) : null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#getLastChild()
	 */
	@Override
	public Node getLastChild() {
		return this.children != null && this.children.size() > 0 ? this.children.get(this.children.size() - 1) : null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#getAttributes()
	 */
	@Override
	public NamedNodeMap getAttributes() {
		return attributeNodes;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#getChildNodes()
	 */
	@Override
	public NodeList getChildNodes() {
		return children;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#appendChild(org.w3c.dom.Node)
	 */
	@Override
	public org.w3c.dom.Node appendChild(org.w3c.dom.Node newChild) throws DOMException {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#cloneNode(boolean)
	 */
	@Override
	public org.w3c.dom.Node cloneNode(boolean deep) {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#compareDocumentPosition(org.w3c.dom.Node)
	 */
	@Override
	public short compareDocumentPosition(org.w3c.dom.Node other) throws DOMException {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#getBaseURI()
	 */
	@Override
	public String getBaseURI() {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#getFeature(java.lang.String, java.lang.String)
	 */
	@Override
	public Object getFeature(String arg0, String arg1) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getNamespaceURI() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#getNextSibling()
	 */
	@Override
	public Node getNextSibling() {
		Node parentNode = getParentNode();
		if (parentNode == null) {
			return null;
		}
		List<Node> children = parentNode.getChildren();
		int nextIndex = children.indexOf(this) + 1;
		return nextIndex < children.size() ? children.get(nextIndex) : null;
	}

	@Override
	public String getNodeValue() throws DOMException {
		return null;
	}

	@Override
	public String getPrefix() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#getPreviousSibling()
	 */
	@Override
	public Node getPreviousSibling() {
		Node parentNode = getParentNode();
		if (parentNode == null) {
			return null;
		}
		List<Node> children = parentNode.getChildren();
		int previousIndex = children.indexOf(this) - 1;
		return previousIndex >= 0 ? children.get(previousIndex) : null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#getTextContent()
	 */
	@Override
	public String getTextContent() throws DOMException {
		return getNodeValue();
	}

	@Override
	public Object getUserData(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#hasChildNodes()
	 */
	@Override
	public boolean hasChildNodes() {
		return children != null && !children.isEmpty();
	}

	@Override
	public org.w3c.dom.Node insertBefore(org.w3c.dom.Node arg0, org.w3c.dom.Node arg1) throws DOMException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isDefaultNamespace(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isEqualNode(org.w3c.dom.Node arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSameNode(org.w3c.dom.Node arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSupported(String arg0, String arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String lookupNamespaceURI(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String lookupPrefix(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void normalize() {
		// TODO Auto-generated method stub

	}

	@Override
	public org.w3c.dom.Node removeChild(org.w3c.dom.Node arg0) throws DOMException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public org.w3c.dom.Node replaceChild(org.w3c.dom.Node arg0, org.w3c.dom.Node arg1) throws DOMException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setNodeValue(String arg0) throws DOMException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPrefix(String arg0) throws DOMException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setTextContent(String arg0) throws DOMException {
		// TODO Auto-generated method stub

	}

	@Override
	public Object setUserData(String arg0, Object arg1, UserDataHandler arg2) {
		// TODO Auto-generated method stub
		return null;
	}

}