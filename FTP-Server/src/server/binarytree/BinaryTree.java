package server.binarytree;

public class BinaryTree {

	// Node -> Key (data)
	//		-> children (left, right child)
	
	public Node root;
	
	public void add(int value) {
		Node nodeToAdd = new Node(value);
		
		// Check if root node exists
		if(root == null) root = nodeToAdd;
		else traverseAndAddNode(root, nodeToAdd);
	}
	
	private void traverseAndAddNode(Node currentNode, Node nodeToAdd) {
		if(nodeToAdd.data < currentNode.data) {
			// If leftChild does not exist
			if(currentNode.leftChild == null) {
				nodeToAdd.parentNode = currentNode;
				currentNode.leftChild = nodeToAdd;
			}
			// Traverse the left child
			else traverseAndAddNode(currentNode.leftChild, nodeToAdd);
		}
		else if(nodeToAdd.data > currentNode.data) {
			// If leftChild does not exist
			if(currentNode.rightChild == null) {
				nodeToAdd.parentNode = currentNode;
				currentNode.rightChild = nodeToAdd;
			}
			// Traverse the right child
			else traverseAndAddNode(currentNode.rightChild, nodeToAdd);
		}
	}
	
	public void traverse() {
		// pre-order, in-order, post-order
		inOrderTraversal(root);
	}
	
	private void inOrderTraversal(Node node) {
		// Keep going to the left child of every node until the
		// bottom-left of the tree is hit
		if(node.leftChild != null) {
			// This will print the leftChild's data
			inOrderTraversal(node.leftChild);
		}
		
		// Print out this nodes data
		System.out.println(node.data);
		
		if(node.rightChild != null) {
			// This will print the leftChild's data
			inOrderTraversal(node.rightChild);
		}
	}
	
	public boolean delete(int data) {
		
		Node nodeToDelete = find(data);
		if(nodeToDelete == null) {
			System.out.println("Could not delete node with data " + data + ", does not exist.");
			return false;
		}
		
		// case 1: node has no children
		else if(nodeToDelete.leftChild == null && nodeToDelete.rightChild == null) {
			deleteNoChild(nodeToDelete);
		}
		// case 2: node has two children
		else if(nodeToDelete.leftChild != null && nodeToDelete.rightChild != null) {
			deleteTwoChildren(nodeToDelete);
		}
		// case 3: node has one child
		else if(nodeToDelete.leftChild != null) {
			deleteOneChild(nodeToDelete);
		}
		else if(nodeToDelete.rightChild != null) {
			deleteOneChild(nodeToDelete); // Same?
		}
		return false;
	}
	
	private void deleteNoChild(Node nodeToDelete) {
		Node parent = nodeToDelete.parentNode;
		// If nodeToDelete is right child
		if(nodeToDelete.equals(parent.leftChild)) parent.leftChild = null;
		// If nodeToDelete is right child
		else if(nodeToDelete.equals(parent.rightChild)) parent.rightChild = null;
	}
	
	private void deleteOneChild(Node nodeToDelete) {
		// Delete by making the nodeToDelete's parent node point to the
		// nodeToDelete's child nodes
		Node parent = nodeToDelete.parentNode;
		// If nodeToDelete is left child
		if(nodeToDelete.equals(parent.leftChild)) {
			// Unsure if left or right child is null, so must check
			if(nodeToDelete.leftChild != null) {
				// Making parent node point to child of deleted node
				parent.leftChild = nodeToDelete.leftChild;
				// Making child of deleted node have correct parent
				parent.leftChild.parentNode = parent;
			}
			else {
				// Making parent node point to child of deleted node
				parent.leftChild = nodeToDelete.rightChild;
				// Making child of deleted node have correct parent
				parent.leftChild.parentNode = parent;
			}
		}
		// If nodeToDelete is right child
		else if(nodeToDelete.equals(parent.rightChild)) {
			// Unsure if left or right child is null, so must check
			if(nodeToDelete.leftChild != null) {
				// Making parent node point to child of deleted node
				parent.rightChild = nodeToDelete.leftChild;
				// Making child of deleted node have correct parent
				parent.rightChild.parentNode = parent;
			}
			else {
				// Making parent node point to child of deleted node
				parent.rightChild = nodeToDelete.rightChild;
				// Making child of deleted node have correct parent
				parent.rightChild.parentNode = parent;
			}
		}
	}
	
	private void deleteTwoChildren(Node nodeToDelete) {
		// Delete by looking at the nodeToDelete's right child, and then
		// traversing all the way down to the left, and then replace nodeToDelete 
		// with the node found at the bottom (Essentially the node with the value closest to the nodeToDelete)
		// After this the tree should still be correct
		
		// This is the node found at the bottom of the right child
		Node minNode = minLeftTraversal(nodeToDelete.rightChild);
		
		// Now delete that bottom node
		deleteOneChild(minNode);
		
		Node parent = nodeToDelete.parentNode;
		// Copy all properties
		minNode.parentNode = parent;
		minNode.leftChild = nodeToDelete.leftChild;
		minNode.rightChild = nodeToDelete.rightChild;
		
		// Special case, nodeToDelete is the root and therefore has noS parent
		if(nodeToDelete.parentNode == null) {
			root = minNode;
		}
		else {
			// If nodeToDelete is left child
			if(nodeToDelete.equals(parent.leftChild)) {
				// switch the node
				parent.leftChild = minNode;
			}
			// If nodeToDelete is right child
			else if(nodeToDelete.equals(parent.rightChild)) {
				// Now switch the node
				parent.rightChild = minNode;
			}
		}
	}
	
	private Node minLeftTraversal(Node node) {
		// Go to the left-most node under the node passed in
		if(node.leftChild == null) return node;
		return minLeftTraversal(node.leftChild);
	}
	
	public Node find(int data) {
		return findNode(root, new Node(data));
	}
	
	private Node findNode(Node search, Node nodeToFind) {
		if(search == null) return null;
		
		if(nodeToFind.data < search.data) return findNode(search.leftChild, nodeToFind);
		if(nodeToFind.data > search.data) return findNode(search.rightChild, nodeToFind);
		// search and node have equal data
		return search; // NOTE: must return search, NOT nodeToFind
	}
}
