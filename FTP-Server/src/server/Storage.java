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

public class Storage {
	
	private static final Logger log = LogManager.getLogger(Storage.class);
	
	private final Server SERVER;
	
	private final File artistFolder; // The actual database
	private final File downloadFolder; // A staging folder before files are placed into the database
	private BinaryTree tree;
	
	/*
	 * The "artist" folder can never be manually modified, it should only be modified
	 * by the program and the program only. The program will take care of empty folders,
	 * misspelled artists, albums, and songs. So there should be no need to manually edit
	 * the server's "artist" folder. If one wishes to add songs manually to the server,
	 * they can do so by moving song files to the server's "download" folder. This folder is where songs go
	 * that have been downloaded from clients and are waiting to be sorted in into the correct
	 * artist and album folders. By dropping files into this folder, the program will automatically
	 * sort them whenever it performs it's next sortFiles() method (usually right after a song is downloaded).
	 * This ensures that the "artist" folder is never modified by hand. The program should also do periodic
	 * sweeps in the download folder to sort leftover music in there.
	 */
	
	public Storage(Server server) {
		this.SERVER = server;
		
		// The Raspberry Pi's mount path to the external drive
		String mountPath = "/mnt/ext500GB";
		// Main server folder, where all server files will be stored
		String mainFolder = "/server";
		
		// Detecting if using Windows operating system, for debugging purposes
		final String OS = System.getProperty("os.name").toLowerCase();
		if(OS.contains("windows")) mountPath = "C:/Users/owner1/Documents/MusicServer";
		
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
		artistFolder = new File(serverPath + "/artist");
		if(artistFolder.exists() || artistFolder.mkdirs()) log.debug(artistFolder.getAbsolutePath() + " path either exists or was created successfully.");
		else {
			log.error("Error creating " + artistFolder.getAbsolutePath() + ".");
			errorAndExit();
		}
		
		downloadFolder = new File(serverPath + "/download");
		if(downloadFolder.exists() || downloadFolder.mkdirs()) log.debug(downloadFolder.getAbsolutePath() + " path either exists or was created successfully.");
		else {
			log.error("Error creating " + downloadFolder.getAbsolutePath() + ".");
			errorAndExit();
		}
		
		log.debug("All required folders have been created successfully.");
	}
	
	/**
	 * Takes any residual files in the downloads folder and places them in the server's music database. Files
	 * are only added to the database if the file passes a certain test by using <code>checkFile(File, String)</code>.
	 * This essentially updates the database and cleans out the downloads folder.
	 */
	public synchronized void sortFiles() {
		String[] songNames = downloadFolder.list();
		
		// Iterate through each file in the downloads folder and place them
		// in their corresponding artist folder.
		for(String songName: songNames) {
			File song = new File(downloadFolder.getAbsolutePath() + "/" + songName);
			if(song.exists()) {
				// Parse file name to find out extension type
				int dotIndex = songName.lastIndexOf('.');
				String extension = "";
				if(dotIndex != -1) extension = songName.substring(dotIndex, songName.length());
				
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
				File newLocation = new File(artistFolder.getAbsolutePath() + "/" + artist + "/" + album);
				File newFile = new File(newLocation.getAbsolutePath() + "/" + title + extension);
				try {
					// Create directory for new file
					if(!newLocation.exists()) newLocation.mkdirs();
					
					// Move the file. If the file already exists, replace it.
					Path path = Files.move(song.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					log.debug("Created " + path);
				} catch (IOException e) {
					log.error("Error moving file " + song.getAbsolutePath() + " from downloads folder.", e);
				}
				
				// The file has been moved into the database, now it will be added to the tree
				tree.add(new Track(newFile.getAbsolutePath()));
			} else {
				log.error("File " + song.getAbsolutePath() + " could not be found.");
			}
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
	 * Loads the music database into the Binary Search Tree. This is done by first sorting residual files
	 * in the staging ("downloads") folder. Then the database is read using <code>retrieveFiles(String, ArrayList<String>)</code>.
	 * Those files are then used to create <code>Track</code> objects. Those <code>Track</code> objects are then added to the 
	 * binary search tree using <code>populateTree(Track[])</code>.
	 */
	public synchronized void loadDatabase() {		
		// Make sure to move any residual download files into the database
		sortFiles();
		
		ArrayList<Track> tracks = new ArrayList<Track>();
		ArrayList<String> filePaths = new ArrayList<String>();
		// Populating filePaths array
		retrieveFiles(getArtistPath(), filePaths);
		
		// Sorting in alphabetical order
		String[] paths = filePaths.toArray(new String[filePaths.size()]);
		Arrays.sort(paths);
		
		// Creating Track objects
		for(String filePath: paths) {
			Track track = new Track(filePath);
			tracks.add(track);
		}
		
		// Populate the binary tree with created Track objects
		tree = new BinaryTree();
		populateTree(tracks.toArray(new Track[tracks.size()]));
		
		tree.traverse();
	}
	
	/**
	 * Returns a list of file paths for each file located under the specified folder, and any sub-folders
	 * that may be located in there.
	 * 
	 * @param rootFolder the folder with which to search for files
	 * @param filePaths the array to populate with found file paths
	 * @return list of file-paths for files under the specified folder
	 */
	private void retrieveFiles(String rootFolder, ArrayList<String> filePaths) {
		File folder = new File(rootFolder);
		String[] subItems = folder.list(); // Getting list of sub-directories, these could be files or folders
		
		// Look at each file/folder in the list. If the item is a file, add it to the file-path array.
		// If it is a folder, then recursively retrieveFiles() from that folder.
		for(String itemName: subItems) {
			File item = new File(rootFolder + "/" + itemName);
			if(item.isDirectory()) retrieveFiles(item.getAbsolutePath(), filePaths);
			else filePaths.add(item.getAbsolutePath());
		}
	}
	
	/**
	 * Creates a binary search tree to store the specified Track objects.
	 * Since the the array of Tracks is in alphabetic order, a good way to populate the tree is
	 * to put the middle track as the root. This leaves two track arrays, the tracks on the left of the index,
	 * and those on the right of the index. The left and right tracks are also split in the middle (using the
	 * middle track as the parent node). This process
	 * is repeated until all tracks have populated the tree.
	 * 
	 * @param tracks the tracks to populate the tree with
	 */
	private void populateTree(Track[] tracks) {
		// Finding the middle track in the current track array
		int midIndex = (int)(tracks.length / 2);
		// Adding the middle track to the tree
		tree.add(tracks[midIndex]);
		
		// Creating a list of tracks that will be on the left side of the middle track
		Track[] leftTracks = new Track[midIndex];
		// Creating a list of tracks that will be on the right side of the middle track
		Track[] rightTracks = new Track[tracks.length - (midIndex + 1)]; 
		
		if(leftTracks.length > 0) {
			// Populating left tracks
			for(int n = 0; n < leftTracks.length; n++) leftTracks[n] = tracks[n];
			// Continue to break the track array up
			populateTree(leftTracks);
		}
		if(rightTracks.length > 0) {
			// Populating right tracks
			for(int n = 0, offset = midIndex + 1; n < rightTracks.length; n++) rightTracks[n] = tracks[n + offset];
			// Continue to break the track array up
			populateTree(rightTracks);
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
		SERVER.stop();
		System.exit(4);
	}
	
	public String getDownloadPath() { 
		return downloadFolder.getAbsolutePath();
	}
	
	public String getArtistPath() {
		return artistFolder.getAbsolutePath();
	}
}
