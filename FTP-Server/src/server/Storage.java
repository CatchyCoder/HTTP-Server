package server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
	 * Need to rewrite how the storage works.
	 *  
	 * The program should not have to rely on the folder structure
	 * in order to determine the names of artists and albums. Instead,
	 * when a song is added, it should be put it in the correct folder. Then when
	 * a field in a song is updated (say the album name), later on the song could be adjusted to the
	 * right folder(s) by using a verify() method that checks if each song is under
	 * the correct album/artist folders. If not then it will be moved. That way
	 * the file structure could be edited manually, or even corrupted, but the program
	 * would still work.
	 * 
	 * could have a downloads folder in the server folder where downloaded songs are sent
	 * to before they are sent to the correct directory.
	 * 
	 * When a file is downloaded, the storage class should be notified to sort any files in
	 * the download folder.
	 */
	
	public Storage() {
		// Main server folder, where all server files will be stored
		String serverFolderPath = "";
		
		// Detecting operating system
		final String OS = System.getProperty("os.name").toLowerCase();
		if(OS.contains("windows")) serverFolderPath = "C:/MusicServer";
		else if(OS.contains("linux")) serverFolderPath = "/MusicServer";
		else {
			// OS not supported
			log.error("Sorry, your operating system, " + OS + ", is not supported. Exiting application.");
			System.exit(1);
		}
		log.debug(OS + " detected.");
		
		// Creating required sub-folders for the server to store its files...
		artistFolder = new File(serverFolderPath + "/artist");
		if(artistFolder.exists() || artistFolder.mkdirs()) log.debug(artistFolder.getAbsolutePath() + " path either exists or was created successfully.");
		else log.error("Error creating " + artistFolder.getAbsolutePath() + ".");
		
		downloadFolder = new File(serverFolderPath + "/download");
		if(downloadFolder.exists() || downloadFolder.mkdirs()) log.debug(downloadFolder.getAbsolutePath() + " path either exists or was created successfully.");
		else log.error("Error creating " + downloadFolder.getAbsolutePath() + ".");
	}
	
	public String getField(FieldKey key, String filePath) {
		try {
			AudioFile song = AudioFileIO.read(new File(filePath));
			Tag tag = song.getTag();
			return tag.getFirst(key);
		} catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e) {
			log.error("Error getting field " + key + ".", e);
		}
		return null;
	}
	
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
	
	public String[] getArtists() {
		String[] artists = artistFolder.list();
		
		// Sort in alphabetical order
		Arrays.sort(artists);
		return artists;
	}
	
	public String getDownloadPath() { 
		return downloadFolder.getAbsolutePath();
	}
	
	public String getArtistPath() {
		return artistFolder.getAbsolutePath();
	}
	
	/*
	public String[] getAlbums(String artist) {
		if(artist.isEmpty()) {
			log.error("Artist String name is empty. No albums Returned.");
			return null;
		}
		
		// Cleaning up whitespace from String
		// then converting to all lower case
		artist = artist.trim();
		artist = artist.toLowerCase();
		// Now converting spaces to underscores.
		// (All folder names have underscores instead of spaces so it must match that format)
		artist = artist.replace(' ', '_');
		
		// Searching for the artist's folder in the database. If the artist is not found,
		// then the artist is either not spelled correctly or does not exist in the
		// database.
		String[] artists = getArtists();
		String artistFolderName = "";
		for(String currentFolderName: artists) {
			// Cleaning up whitespace from String
			// then converting to all lower case
			currentFolderName = currentFolderName.trim();
			currentFolderName = currentFolderName.toLowerCase();
			
			if(currentFolderName.equals(artist)) {
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
			// by looking at folder names in artist's folder
			File artistFolder = new File(serverFolder.getAbsolutePath() + "/" + artistFolderName + "/");
			log.debug("artist folder path = " + artistFolder.getAbsolutePath());
			String[] albums = artistFolder.list();
			if(albums.length == 0) log.debug("album list size is 0");
			log.debug("returning album - should be good");
			return albums;
		}
	}*/
}
