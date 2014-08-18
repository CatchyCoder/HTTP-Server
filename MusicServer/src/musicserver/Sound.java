package musicserver;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class Sound {

	private static final String PATH = "/snd/";
	
	public static Sound BUMP = loadSound("HardKick.wav");
	
	private Clip clip;
	
	public static Sound loadSound(String fileName) {
		Sound sound = new Sound();
		try {
			AudioInputStream ais = AudioSystem.getAudioInputStream(Sound.class.getResource(PATH + fileName));
			Clip clip = AudioSystem.getClip();
			clip.open(ais);
			sound.clip = clip;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return sound;
	}
	
	public void play() {
		try {
			if(clip != null) {
				new Thread() {
					@Override
					public void run() {
						synchronized (clip) {
							clip.stop();
							clip.setFramePosition(0);
							clip.start();
						}
					}
				}.start();	
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}
