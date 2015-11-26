package server.binarytree;

public class Test {

	public static void main(String[] args) {
		BinaryTree tree = new BinaryTree();
		
		// Adding values
		tree.add(25); // Root
		tree.add(10);
		tree.add(30);
		tree.add(40);
		tree.add(20);
		tree.add(5);
		tree.add(50);
		tree.add(35);
		tree.add(45);
		tree.add(15);
		
		tree.traverse();
		tree.delete(25);
		System.out.println("AFTER DELETE");
		tree.traverse();
	}
}
