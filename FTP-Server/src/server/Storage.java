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
	
	private final File artistFolder;
	private final File downloadFolder;
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
		if(OS.contains("windows")) mountPath = "C://MusicServer";
		
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
	
	/**
	 * Takes any residual files in the downloads folder and places them in the server's music database.
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
				File newLocation = new File(artistFolder.getAbsolutePath() + "/" + artist + "/" + album + "/" + title + extension);
				try {
					// Create directory for new file
					if(!newLocation.exists()) newLocation.mkdirs();
					
					// Move the file. If the file already exists, replace it.
					Path path = Files.move(song.toPath(), newLocation.toPath(), StandardCopyOption.REPLACE_EXISTING);
					log.debug("Created " + path);
				} catch (IOException e) {
					log.error("Error moving file " + song.getAbsolutePath() + " from downloads folder.", e);
				}
			} else {
				log.error("File " + song.getAbsolutePath() + " could not be found.");
			}
		}
	}
	
	public void loadDatabase() {
		// TODO:
		// Get artists - get albums - then create Track objects for each track under those artists and albums (just loop it)
		// Store these Tracks in an arraylist, then use that arraylist to create the binary tree
		// sort the list of Track objects alphabetically, then make the middle Track object the root of the tree
		// (then find an efficient way to store it from there).
		// When a client adds tracks, new Track objects will be created that are just added to the binary tree,
		// don't recreate the tree - this will take too long.
		// If the tree needs to be re-created, then just restart the program.
		
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
	 * Scrapes the music server's database for all the music files it has, and returns
	 * a list of each file's file path.
	 * @return
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
	
	private void populateTree(Track[] tracks) {
		// Creating binary search tree to store Track objects.
		// Since the the array of Tracks is in alphabetic order, a good way to populate the tree is
		// to put the middle track as the root, then take the left and right parts and make each "part's"
		// middle track as the sub-root, and so on and so forth until the tree is populated with every track.
		
		int midIndex = (int)(tracks.length / 2);
		tree.add(tracks[midIndex]);
		//  1 2 3 -4- 5 6
		Track[] leftTracks = new Track[midIndex]; // Creating a list of tracks that will be on the left side of the middle track
		Track[] rightTracks = new Track[tracks.length - (midIndex + 1)]; // Creating a list of tracks that will be on the right side of the middle track
		
		if(leftTracks.length > 0) {
			// Populating left tracks
			for(int n = 0; n < leftTracks.length; n++) leftTracks[n] = tracks[n];
			populateTree(leftTracks);
		}
		if(rightTracks.length > 0) {
			// Populating right tracks
			for(int n = 0, offset = midIndex + 1; n < rightTracks.length; n++) rightTracks[n] = tracks[n + offset];
			populateTree(rightTracks);
		}
	}
	
	/**
	 * Returns all artists currently located in the database.
	 */
	private String[] getArtists() {
		String[] artists = artistFolder.list();
		
		// Sort in alphabetical order
		Arrays.sort(artists);
		return artists;
	}
	
	/**
	 * Returns all albums currently in the database corresponding to the specified artist.
	 * 
	 * @param artist the artist to find albums for.
	 */
	private String[] getAlbums(String artist) {
		// Cleaning up whitespace from artist name
		artist = artist.trim();
		
		if(artist.isEmpty()) {
			log.error("Artist String name is empty. NULL Returned.");
			return null;
		}
		
		// Replacing artist String spaces with underscores (for folder names) and converting to all lower case
		artist = artist.replace(' ', '_').toLowerCase();
		File folder = new File(artistFolder.getAbsolutePath() + "/" + artist);
		String[] albums = folder.list();
		Arrays.sort(albums);
		return albums;
	}
	
	/**
	 * Returns all track currently in the database corresponding to the specified artist and album.
	 * 
	 * @param artist the artist to find albums for.
	 */
	private String[] getTracks(String artist, String album) {
		// Check to make sure that the artist actually has the album specified
		File albumFolder = new File(artistFolder.getAbsolutePath() + "/" + artist + "/" + album);
		if(!albumFolder.exists()) {
			log.error("Artist, " + artist + ", or album, " + album + 
					", does not exist in the database. Therefore " + albumFolder.getAbsolutePath() + " was not found to exist. NULL returned.");
			return null;
		}
		
		// The path exists, so return the track names...
		// File names contain file extensions, so those are removed
		String[] tracksWithExt = albumFolder.list();
		String[] tracks = new String[tracksWithExt.length];
		for(int n = 0; n < tracks.length; n++) {
			String fileName = tracksWithExt[n];
			int dotIndex = fileName.indexOf('.');
			String trackName = fileName.substring(0, dotIndex);
			tracks[n] = trackName.toLowerCase();
		}
		Arrays.sort(tracks);
		return tracks;
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
	
//	/**
//	 * Returns albums in the database for the specified artist that contain a given keyword.
//	 * 
//	 * @param artist the artist to find albums for
//	 * @param keyword the keyword the albums will be checked against
//	 * @return an array containing albums for the artist that contain the specified keyword
//	 */
//	public String[] searchAlbums(String artist, String keyword) {
//		String[] albums = getAlbums(artist);
//		ArrayList<String> refinedAlbums = new ArrayList<String>();
//		// Search for albums that contain they keyword specified
//		for(String album: albums){
//			if(album.contains(keyword)) refinedAlbums.add(album);
//		}
//		return (String[]) refinedAlbums.toArray();
//	}
	
//	public String[][][] getData() {
//		
//		String[] artists = getArtists();
//		
//		String[][][] data = new String[artists.length][0][0];
//		// Fill in artist data
//		for(int artist = 0; artist < data.length; artist++) {
//			data[artist] = artists[artist];
//			for(int album = 0; album < data[artist].length; album++) {
//				for(int track = 0; track < data[artist][album].length; track++) {
//					
//				}
//			}
//		}
//		
//		return null;
//	}
}
