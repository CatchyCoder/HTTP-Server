package server;

import org.jaudiotagger.tag.FieldKey;

public class Track implements Comparable<Track> {

	private final String title;
	private final String album;
	private final String artist;
	
	// The artist, album, and track name are all used to identify a song.
	// Concatenating these values into one String gives the song an "ID value".
	// By checking the ID against other songs, the Song object can be properly
	// stored into the binary tree.
	private final String ID;
	
	private final String PATH;
	
	public Track(final String filePath) {
		this.PATH = filePath;
		title = Storage.getField(FieldKey.TITLE, filePath);
		album = Storage.getField(FieldKey.ALBUM, filePath);
		artist = Storage.getField(FieldKey.ARTIST, filePath);
		
		// Since Song objects in the binary tree are stored alphabetically by ID,
		// changing the order of the ID concatenation will change the order of the tree.
		String multiCaseID = artist + album + title;
		ID = multiCaseID.toLowerCase();
	}

	public String getTitle() {
		return title;
	}

	public String getAlbum() {
		return album;
	}

	public String getArtist() {
		return artist;
	}

	public String getID() {
		return ID;
	}
	
	public String getPath() {
		return PATH;
	}

	@Override
	public int compareTo(Track track) {
		String ID1 = getID();
		String ID2 = track.getID();
		return ID1.compareTo(ID2);
	}
}
