package mic;

import java.io.ByteArrayOutputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

/**
 * @author Clement Peter Evans, 2021
 *
 */
public class MicInput {

	static VisualiserFrameMic main;
	static TargetDataLine targetLine;

	/**
	 * Main method begins recording audio and then starts a new thread for sending
	 * signals to the visualiser
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		main = new VisualiserFrameMic();
		captureAudio();
		Thread t1 = new Thread(new MicInput().new sendAudio());
		t1.start();

	}

	/**
	 * 
	 * A thread is opened that 
	 *
	 */
	private class sendAudio implements Runnable {

		public void run() {
			
			int incrementor = 0;
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int numBytesRead;
			byte[] data = new byte[1024];
			
			// Here, stop is determined by the other thread
			while (true) {
				// Read the next chunk of data from the TargetDataLine.
				numBytesRead = targetLine.read(data, 0, data.length);
				// System.out.println(data.length);
				// Save this chunk of data
				out.write(data, 0, numBytesRead);

				circleSize = Math.round(getLevel(data) * 8);
				main.visualiserPanel.circleHeight = circleSize;
				main.visualiserPanel.circleWidth = circleSize;
				maxCircleSize = Math.max(circleSize, prevCircleSize);
				main.visualiserPanel.repaint(main.visualiserPanel.PANEL_WIDTH / 2 - maxCircleSize / 2,
						main.visualiserPanel.PANEL_HEIGHT / 2 - maxCircleSize / 2, maxCircleSize, maxCircleSize);
				prevCircleSize = circleSize;
				
				System.out.println(incrementor);
			      if (incrementor > 2000) {
			    	  incrementor = 0;
			      } else {
			    	  ++incrementor;
			      }
				
			}
		}
	}

	static int circleSize;
	static int prevCircleSize;
	static int maxCircleSize;

	/**
	 * This method sets up a line from the mixer and begins capturing audio
	 */
	public static void captureAudio() {

		// Set out the audio format we want the microphone to record to
		final AudioFormat audioFormat = new AudioFormat(110000, // sample rate
				16, // sample size in bits
				1, // channels
				true, // signed
				true); // big endian

		// Instantiate the TargetDataLine and create an info object for the DataLine
		// with the chosen audio format
		targetLine = null;
		DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

		// Query whether or not the computer audio system can support a line with the
		// given format
		if (!AudioSystem.isLineSupported(dataLineInfo)) {
			// For the user's sake, print that the system doesn't have the capability to
			// support the specified audio format
			System.out.println("The audio system you are using does not support the audio format:\n" + audioFormat);
		} else {
			try {
				// Define the TargetDataLine and then try to open it
				targetLine = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
				targetLine.open(audioFormat);
			} catch (LineUnavailableException e) {
				e.printStackTrace();
			}

		}

		// Begin audio capture.
		targetLine.start();
	}

	/**
	 * This method receives the byte array taken from the source buffer and returns
	 * the audio level for the chunk of data it just received using RMS (root mean
	 * squared)
	 * 
	 * @param data The raw audio data taken from the line as a byte array
	 * @return float The level of the audio data
	 */
	public static float getLevel(byte[] data) {
		long lSum = 0;
		for (int i = 0; i < data.length; i++)
			lSum = lSum + Math.abs(data[i]);

		float averageMeanSquare = (float) lSum / (float) data.length;

		return (averageMeanSquare);

	}
}