package server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import server.binarytree.BinaryTree;
import server.binarytree.Node;

public class Storage {
	
	private static final Logger log = LogManager.getLogger(Storage.class);
		
	private final File databaseFolder; // The actual database
	private final File downloadFolder; // A staging folder before files are placed into the database
	private final BinaryTree tree;
	
	/*
	 * The "database" folder can never be manually modified, it should only be modified
	 * by the program and the program only. The program will take care of empty folders,
	 * misspelled artists, albums, and songs. So there should be no need to manually edit
	 * the server's "database" folder. If one wishes to add songs manually to the server,
	 * they can do so by moving song files to the server's "download" folder. This folder is where songs go
	 * that have been downloaded from clients and are waiting to be sorted in into the correct
	 * artist and album folders. By dropping files into this folder, the program will automatically
	 * sort them whenever it performs it's next sortFiles() method (usually right after a song is downloaded).
	 * This ensures that the "database" folder is never modified by hand. The program should also do periodic
	 * sweeps in the download folder to sort leftover music in there.
	 */
	
	public Storage() {
		tree = new BinaryTree();
		
		// The Raspberry Pi's mount path to the external drive
		String mountPath = "/mnt/ext500GB";
		// Main server folder, where all server files will be stored
		String mainFolder = "/server";
		
		// Full path to server's main folder
		String serverPath = mountPath + mainFolder;
		
		// Checking if external drive has been mounted. This assumes that the server folder
		// on the external drive has already been created (could be an empty folder or not).
		// If mounted then server folder is visible, else unmounted.
		if(!new File(mountPath).exists()) {
			// The mount path has not been created, therefore the external drive is not mounted.
			log.error("Mount path, " + mountPath +", for external drive could not be found. Exiting application");
			System.exit(2);
		}
		log.debug("Mount path, " + mountPath +", found.");
		if(!new File(serverPath).exists()) {
			// The mount path was created, but the external drive was not mounted.
			log.error("The external drive does not appear to be mounted (Main folder " + mainFolder + " does not exist). Exiting application");
			System.exit(3);
		}
		log.debug("External drive appears to be mounted.");
		
		// Creating required sub-folders for the server to store its files...
		databaseFolder = new File(serverPath + "/database");
		if(databaseFolder.exists() || databaseFolder.mkdirs()) log.debug(databaseFolder.getAbsolutePath() + " path either exists or was created successfully.");
		else {
			log.error("Error creating " + databaseFolder.getAbsolutePath() + ".");
			errorAndExit();
		}
		
		downloadFolder = new File(serverPath + "/download");
		if(downloadFolder.exists() || downloadFolder.mkdirs()) log.debug(downloadFolder.getAbsolutePath() + " path either exists or was created successfully.");
		else {
			log.error("Error creating " + downloadFolder.getAbsolutePath() + ".");
			errorAndExit();
		}
		
		log.debug("All required folders have been created successfully.");
		
		// Load the database and tree
		update();
		
//		// Sleep 45 seconds in order to give time to add new files to download folder
//		try {
//			log.debug("Now sleeping...");
//			Thread.sleep(1000 * 45);
//			log.debug("Done.");
//		} catch(Exception e) {
//			log.error("Could not sleep :/");
//		}
//		
//		update();
	}
	
	public synchronized void update() {
		updateDatabase();
		updateTree();
	}
	
	/**
	 * Takes any residual files in the downloads folder and places them in the server's music database. Files
	 * are only added to the database if the file passes a certain test by using <code>checkFile(File, String)</code>.
	 * This essentially updates the database and cleans out the downloads folder.
	 * 
	 * <p> NOTE: The file is only moved into the database folder. It will NOT be added to the binary search tree.
	 */
	public synchronized void updateDatabase() {
		log.debug("Updating database...");
		
		ArrayList<String> songPaths = new ArrayList<String>();
		// Populating list of song paths
		retrieveFiles(getDownloadPath(), songPaths, false);
		
		// Iterate through each file in the downloads folder and place them
		// in their corresponding artist folder.
		for(String songPath: songPaths) {
			File song = new File(songPath);
			if(song.exists()) {
				String fileName = song.getName();
				// Parse file name to find out extension type
				int dotIndex = fileName.lastIndexOf('.');
				String extension = "";
				if(dotIndex != -1) extension = fileName.substring(dotIndex, fileName.length());
				
				// Check file to make sure it is legitimate
				if(!checkFile(song, extension)) {
					log.debug("File " + song.getAbsolutePath() + " did not pass check test. File will NOT be added to database.");
					return;
				}
				
				// Retrieve artist, album, and name of downloaded song
				String artist = getField(FieldKey.ARTIST, song.getAbsolutePath());
				String album = getField(FieldKey.ALBUM, song.getAbsolutePath());
				String title = getField(FieldKey.TITLE, song.getAbsolutePath());
				
				// Replacing all spaces with underscores (for folder naming)
				// and converting to all lower case
				artist = artist.replace(' ', '_').toLowerCase();
				album = album.replace(' ', '_').toLowerCase();
				title = title.replace(' ', '_').toLowerCase();
				
				// The new location to move the file
				File newDirectory = new File(databaseFolder.getAbsolutePath() + "/" + artist + "/" + album);
				File newFile = new File(newDirectory.getAbsolutePath() + "/" + title + extension);
				try {
					// Create directory for new file
					if(!newDirectory.exists()) newDirectory.mkdirs();
					
					// Move the file. If the file already exists, replace it.
					Path path = Files.move(song.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					log.debug("Created " + path);
				} catch (IOException e) {
					log.error("Error moving file " + songPath + " from downloads folder.", e);
				}
				
				// NOTE: The file has been moved into the database, but NOT into the binary search tree.
				
			} else {
				log.error("File " + song.getAbsolutePath() + " could not be found.");
			}
		}
		log.debug("Done.");
		
		// All files have either been moved into the database or deleted. However there may be folders
		// left over that we will delete.
		
		// First check to make sure there are no files left, just directories
		ArrayList<String> files = new ArrayList<String>();
		// Populating files list
		retrieveFiles(getDownloadPath(), files, false);
		if(files.size() > 0) {
			log.error("Files still remained after cleaning downloads folder: ");
			for(String file: files) {
				log.error(file + " remained.");
			}
			log.error("Cleaning of downloads folder will be CANCELLED.");
			return;
		}
		
		log.debug("Removing empty folders in " + getDownloadPath());
		// If there are only directories left, remove them
		File downloadFolder = new File(getDownloadPath());
		String[] items = downloadFolder.list();
		
		for(String item: items) {
			// Delete each folder regardless of its contents
			deleteFolder(getDownloadPath() + "/" + item);
		}
		
		log.debug("Done.");
	}
	
	/**
	 * Recursively deletes a folder and any files or folders that it may contain.
	 * 
	 * @param directory
	 */
	private void deleteFolder(String directory) {
		File item = new File(directory);
		
		// Check if path is a file, if so delete it
		if(!item.isDirectory()) item.delete();
		else {
			// Remove any sub-folder and files
			String[] subItems = item.list();
			for(String itemName: subItems) {
				deleteFolder(item.getAbsolutePath() + "/" + itemName);
			}
			
			// Then delete the folder
			item.delete();
		}
	}
	
	/**
	 * Checks to make sure the specified file is not a directory, is a supported audio file format, and that
	 * there are no issues reading track data from the file. Returns false otherwise.
	 * 
	 * <p>If false is returned, this means that the file did not pass the test and has therefore been deleted.
	 * 
	 * @param file the file to be checked
	 * @param extension the extension for the specified file
	 * @returns true if and only if the file is NOT a directory, is supported, and its track data can be read properly. False otherwise.
	 */
	private boolean checkFile(File file, String extension) {
		// Checking if the file is a directory
		if(file.isDirectory()) {
			log.error("File " + file.getAbsolutePath() + " is a directory, and therefore cannot be moved into the database. Deleting folder");
			if(file.delete()) log.error("Done.");
			else log.error("There was a problem deleting the file.");
			return false;
		}
		
		// Checking to make sure that the file extension is a supported audio file format
		if(	extension.equals(".mp3") || // Mp3
				extension.equals(".mp4")  || // Mp4
				extension.equals(".m4p")  ||
				extension.equals(".m4a")  ||
				extension.equals(".flac") || // FLAC
				extension.equals(".ogg") || // Ogg Vorbis
				extension.equals(".oga") ||
				extension.equals(".wma") || // Wma
				extension.equals(".wav") || // Wav
				extension.equals(".ra") || // Real
				extension.equals(".ram")) {
			// The file is a supported format, attempt to retrieve song data from it
			
			// Retrieve artist, album, and name of downloaded song
			String artist = getField(FieldKey.ARTIST, file.getAbsolutePath());
			String album = getField(FieldKey.ALBUM, file.getAbsolutePath());
			String title = getField(FieldKey.TITLE, file.getAbsolutePath());
			
			// If anything could not be read, note that there were issues with the file and delete it
			if(artist == null || album == null || title == null) {
				log.debug("There was a problem getting track data from " + file.getAbsolutePath() + ". Deleting file...");
				if(file.delete()) log.error("Done.");
				else log.error("There was a problem deleting the file.");
				return false;
			}
			return true;
		}
		else {
			// Incorrect file format
			log.error("File " + file.getAbsolutePath() + " is not a supported audio file. Deleting file...");
			
			// Deleting file
			if(file.delete()) log.debug("Done.");
			else log.error("There was a problem deleting the file.");
			return false;
		}
	}
	
	/**
	 * TODO: UPDATE ME
	 * Loads the music database into the Binary Search Tree. This is done by first sorting residual files
	 * in the staging ("downloads") folder. Then the database is read using <code>retrieveFiles(String, ArrayList<String>, boolean)</code>.
	 * Those files are then used to create <code>Track</code> objects. Those <code>Track</code> objects are then added to the 
	 * binary search tree using <code>populateTree(Track[])</code>.
	 */
	public synchronized void updateTree() {
		log.debug("Updating binary search tree...");
		
		// Creating an array to store file paths for files currently in the database
		ArrayList<String> databasePaths = new ArrayList<String>();
		// Populating that array
		retrieveFiles(getDatabasePath(), databasePaths, false);
		
		// Creating an array to store file paths for files currently in the binary tree
		ArrayList<String> treePaths = new ArrayList<String>();
		// Populating that array
		for(Node node: tree.getNodes()) {
			treePaths.add(node.getTrack().getPath());
		}
		
		// Any paths that already exist in the binary tree are not needed, and therefore will be removed.
		// This leaves only the file paths that need to be added to the tree left.
		databasePaths.removeAll(treePaths);
		ArrayList<String> newPaths = databasePaths;
		System.out.println("new paths to add to tree: " + databasePaths);
		
		// Check if there are still paths to add to the tree
		if(newPaths.size() > 0) {
			// Now use those paths to create new Track objects
			ArrayList<Track> tracks = new ArrayList<Track>();
			for(String path: newPaths) {
				Track track = new Track(path);
				tracks.add(track);
			}
			Track[] trackArray = tracks.toArray(new Track[tracks.size()]);
			// Sorting in alphabetical order
			Arrays.sort(trackArray);
			
			// Now adding those tracks to the tree
			tree.add(trackArray);
		}
		log.debug("Done.");
	}
	
	/**
	 * Returns a list of file paths for each file located under the specified folder, and any sub-folders
	 * that may be located in there. Pass in true for getDirectories if you wish for directories to be included
	 * in the returned list.
	 * 
	 * @param rootFolder the folder with which to search for files
	 * @param filePaths the array to populate with found file paths
	 * @param getDirectories true if directories should be included in the list
	 * @return list of file-paths for files under the specified folder
	 */
	private void retrieveFiles(String rootFolder, ArrayList<String> filePaths, boolean getDirectories) {
		File folder = new File(rootFolder);
		String[] subItems = folder.list(); // Getting list of sub-directories, these could be files or folders
		
		// Look at each file/folder in the list. If the item is a file, add it to the file-path array.
		// If it is a folder, then only add it if getDirectories is true, and recursively retrieveFiles() from that folder.
		for(String itemName: subItems) {
			File item = new File(rootFolder + "/" + itemName);
			if(item.isDirectory()) {
				if(getDirectories) filePaths.add(item.getAbsolutePath());
				retrieveFiles(item.getAbsolutePath(), filePaths, getDirectories);
			}
			else filePaths.add(item.getAbsolutePath());
		}
	}
	
	public static String getField(FieldKey key, String filePath) {
		try {
			AudioFile song = AudioFileIO.read(new File(filePath));
			Tag tag = song.getTag();
			return tag.getFirst(key);
		} catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e) {
			log.error("Error getting field " + key + " for " + filePath, e);
		}
		return null;
	}
	
	private void errorAndExit() {
		log.error("There were problems creating the required server folders. Possibly a permissions issue. "
				+ "Try running with sudo or using root user.");
		log.debug("Exiting application.");
		System.exit(4);
	}
	
	public String getDownloadPath() { 
		return downloadFolder.getAbsolutePath();
	}
	
	public String getDatabasePath() {
		return databaseFolder.getAbsolutePath();
	}
}
