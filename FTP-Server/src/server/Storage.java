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

public class Storage {
	
	private static final Logger log = LogManager.getLogger(Storage.class);
	
	private final File artistFolder;
	private final File downloadFolder;
	
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
	
	public Storage() {
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
	
	private void errorAndExit() {
		log.error("There were problems creating the required server folders. Possibly a permissions issue. Try running sudo or using root user.");
		log.debug("Exiting application.");
		System.exit(4);
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
	 * Takes any residual files in the downloads folder and sorts them into the correct artist and album folder
	 * under the server's "artist" folder. This essentially updates the database and cleans out the downloads folder.
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
	
	/**
	 * Returns all artist's in the database
	 */
	public String[] getArtists() {
		String[] artists = artistFolder.list();
		
		// Sort in alphabetical order
		Arrays.sort(artists);
		return artists;
	}
	
	/**
	 * Returns all albums in the database under the specified artist.
	 * 
	 * @param artist the artist to find albums for.
	 */
	public String[] getAlbums(String artist) {
		// Cleaning up whitespace from artist name
		artist = artist.trim();
		
		if(artist.isEmpty()) {
			log.error("Artist String name is empty. No albums Returned.");
			return null;
		}
		
		// Searching for the artist's folder in the database. If the artist is not found,
		// then the artist is either not spelled correctly or does not exist in the
		// database.
		String[] artists = getArtists();
		String artistFolderName = "";
		for(String currentFolderName: artists) {
			// Cleaning up whitespace from folder name
			currentFolderName = currentFolderName.trim();
			// Replacing underscores with spaces
			currentFolderName = currentFolderName.replace('_', ' ');
			
			if(currentFolderName.compareToIgnoreCase(artist) == 0) {
				artistFolderName = currentFolderName;
				break;
			}
		}
		// If the artist could not be found
		if(artistFolderName.equals("")) {
			log.error("Artist \"" + artist + "\" was not be found in database. No albums returned.");
			return null;
		} else {
			// Gathering album names (that exist in the database) for the artist
			// by looking at folder names in that artist's folder
			File artFolder = new File(artistFolder.getAbsolutePath() + "/" + artistFolderName + "/");
			log.debug("artist folder path: " + artFolder.getAbsolutePath());
			String[] albums = artFolder.list();
			if(albums.length == 0) log.debug("album list size is 0.");
			Arrays.sort(albums);
			return albums;
		}
	}
	
	public String[] getSongs(String s) {
		return null;
	}
	
	public String[] searchArtists() {
		return null;
	}
	
	/**
	 * Returns albums in the database for the specified artist that contain a given keyword.
	 * 
	 * @param artist the artist to find albums for
	 * @param keyword the keyword the albums will be checked against
	 * @return an array containing albums for the artist that contain the specified keyword
	 */
	public String[] searchAlbums(String artist, String keyword) {
		String[] albums = getAlbums(artist);
		ArrayList<String> refinedAlbums = new ArrayList<String>();
		// Search for albums that contain they keyword specified
		for(String album: albums){
			if(album.contains(keyword)) refinedAlbums.add(album);
		}
		return (String[]) refinedAlbums.toArray();
	}
	
	public String[] searchSongs() {
		return null;
	}
	
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
	
	public String getDownloadPath() { 
		return downloadFolder.getAbsolutePath();
	}
	
	public String getArtistPath() {
		return artistFolder.getAbsolutePath();
	}
}
