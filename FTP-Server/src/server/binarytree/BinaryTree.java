package server.binarytree;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import server.Track;

public class BinaryTree {
	
	private static final Logger log = LogManager.getLogger(BinaryTree.class);
	
	private Node root;
	
	public void add(Track track) {
		Node nodeToAdd = new Node(track);
		
		// Check if root node exists
		if(root == null) root = nodeToAdd;
		else traverseAndAddNode(root, nodeToAdd);
	}
	
	private void traverseAndAddNode(Node currentNode, Node nodeToAdd) {
		if(compare(nodeToAdd, currentNode) < 0) {
			// If leftChild does not exist
			if(currentNode.getLeftChild() == null) {
				nodeToAdd.setParent(currentNode);
				currentNode.setLeftChild(nodeToAdd);
			}
			// Traverse the left child
			else traverseAndAddNode(currentNode.getLeftChild(), nodeToAdd);
		}
		else if(compare(nodeToAdd, currentNode) > 0) {
			// If rightChild does not exist
			if(currentNode.getRightChild() == null) {
				nodeToAdd.setParent(currentNode);
				currentNode.setRightChild(nodeToAdd);
			}
			// Traverse the right child
			else traverseAndAddNode(currentNode.getRightChild(), nodeToAdd);
		}
		else {
			// Node already exists in database
			log.debug("Data, " + nodeToAdd.getTrack().getID() + ", already exists in database. No action needed.");
		}
	}
	
	public void traverse() {
		// pre-order, in-order, post-order
		inOrderTraversal(root);
	}
	
	private void inOrderTraversal(Node node) {
		// Keep going to the left child of every node until the
		// bottom-left of the tree is hit
		if(node.getLeftChild() != null) {
			// This will print the leftChild's data
			inOrderTraversal(node.getLeftChild());
		}
		
		// Print out this nodes data
		System.out.println(node.getTrack().getID());
		
		if(node.getRightChild() != null) {
			// This will print the leftChild's data
			inOrderTraversal(node.getRightChild());
		}
	}
	
	public boolean delete(Track track) {
		Node nodeToDelete = find(track);
		if(nodeToDelete == null) {
			log.debug("Node with data " + track + " was not found. No action needed.");
			return false;
		}
		
		// case 1: node has no children
		else if(nodeToDelete.getLeftChild() == null && nodeToDelete.getRightChild() == null) {
			deleteNoChild(nodeToDelete);
		}
		// case 2: node has two children
		else if(nodeToDelete.getLeftChild() != null && nodeToDelete.getRightChild() != null) {
			deleteTwoChildren(nodeToDelete);
		}
		// case 3: node has one child
		else deleteOneChild(nodeToDelete);
		return false;
	}
	
	private void deleteNoChild(Node nodeToDelete) {
		Node parent = nodeToDelete.getParent();
		// If nodeToDelete is right child
		if(nodeToDelete.equals(parent.getLeftChild())) parent.setLeftChild(null);
		// If nodeToDelete is right child
		else if(nodeToDelete.equals(parent.getRightChild())) parent.setRightChild(null);
	}
	
	private void deleteOneChild(Node nodeToDelete) {
		// Delete by making the nodeToDelete's parent node point to the
		// nodeToDelete's child nodes
		Node parent = nodeToDelete.getParent();
		// If nodeToDelete is left child
		if(nodeToDelete.equals(parent.getLeftChild())) {
			// Unsure if left or right child is null, so must check
			if(nodeToDelete.getLeftChild() != null) {
				// Making parent node point to child of deleted node
				parent.setLeftChild(nodeToDelete.getLeftChild());
				// Making child of deleted node have correct parent
				parent.getLeftChild().setParent(parent);
			}
			else {
				// Making parent node point to child of deleted node
				parent.setLeftChild(nodeToDelete.getRightChild());
				// Making child of deleted node have correct parent
				parent.getLeftChild().setParent(parent);
			}
		}
		// If nodeToDelete is right child
		else if(nodeToDelete.equals(parent.getRightChild())) {
			// Unsure if left or right child is null, so must check
			if(nodeToDelete.getLeftChild() != null) {
				// Making parent node point to child of deleted node
				parent.setRightChild(nodeToDelete.getLeftChild());
				// Making child of deleted node have correct parent
				parent.getRightChild().setParent(parent);
			}
			else {
				// Making parent node point to child of deleted node
				parent.setRightChild(nodeToDelete.getRightChild());
				// Making child of deleted node have correct parent
				parent.getRightChild().setParent(parent);
			}
		}
	}
	
	private void deleteTwoChildren(Node nodeToDelete) {
		// Delete by looking at the nodeToDelete's right child, and then
		// traversing all the way down to the left, and then replace nodeToDelete 
		// with the node found at the bottom (Essentially the node with the value closest to the nodeToDelete)
		// After this the tree should still be correct
		
		// This is the node found at the bottom of the right child
		Node minNode = minLeftTraversal(nodeToDelete.getRightChild());
		
		// Now delete that bottom node
		deleteOneChild(minNode);
		
		Node parent = nodeToDelete.getParent();
		// Copy all properties
		minNode.setParent(parent); // Assign parent
		minNode.setLeftChild(nodeToDelete.getLeftChild()); // Assign left child
		minNode.setRightChild(nodeToDelete.getRightChild()); // Assign right child
		minNode.getLeftChild().setParent(minNode); // Assign left child's parent
		minNode.getRightChild().setParent(minNode); // Assign right child's parent
		
		// Special case, nodeToDelete is the root and therefore has no parent
		if(nodeToDelete.getParent() == null) {
			root = minNode;
		}
		else {
			// If nodeToDelete is left child
			if(nodeToDelete.equals(parent.getLeftChild())) {
				// switch the node
				parent.setLeftChild(minNode);
			}
			// If nodeToDelete is right child
			else if(nodeToDelete.equals(parent.getRightChild())) {
				// Now switch the node
				parent.setRightChild(minNode);
			}
		}
	}
	
	private Node minLeftTraversal(Node node) {
		// Go to the left-most node under the node passed in
		if(node.getLeftChild() == null) return node;
		return minLeftTraversal(node.getLeftChild());
	}
	
	public Node find(Track track) {
		return findNode(root, new Node(track));
	}
	
	private Node findNode(Node search, Node nodeToFind) {
		if(search == null) return null;
		
		if(compare(nodeToFind, search) < 0) return findNode(search.getLeftChild(), nodeToFind);
		if(compare(nodeToFind, search) > 0) return findNode(search.getRightChild(), nodeToFind);
		// search and node have equal data
		return search; // NOTE: must return search, NOT nodeToFind (as search has the full data for the Node object)
	}
	
	private int compare(final Node node1, final Node node2) {
		String ID1 = node1.getTrack().getID();
		String ID2 = node2.getTrack().getID();
		return ID1.compareTo(ID2);
	} 
}
