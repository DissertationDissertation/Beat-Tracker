package audio;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import fourier.SignalProcessor;
import rollingmemory.RollingArrayMemory;

public class StreamAudioExample{

	public static void main(String[] argv) throws UnsupportedAudioFileException, IOException, LineUnavailableException {

		AudioInputStream stream = AudioSystem.getAudioInputStream(new File(
				"inAudio/Curbi-51.WAV"));

		AudioFormat format = stream.getFormat();
		System.out.println(format.getSampleRate());

		SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class, stream.getFormat(),
				64);
		SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
		line.open(stream.getFormat(), 4000);
		line.start();
		
		System.out.println("audio buffer is " + line.getBufferSize());

		int numberOfChannels = format.getChannels();
		int sampleSizeInBytes = format.getSampleSizeInBits() / 8;
		float sampleRate = format.getSampleRate();
		int numRead;
		byte[] buf = new byte[64 * numberOfChannels * sampleSizeInBytes];
		int[] monoAudio;
		int samplesInBuf = buf.length / (numberOfChannels * sampleSizeInBytes);
		float timeForLoopMilli = buf.length / (numberOfChannels * sampleSizeInBytes * sampleRate);
		System.out.println("The maximum time a loop can take is: " + timeForLoopMilli * 1000 + "milliseconds");

		RollingArrayMemory rollingArray = new RollingArrayMemory(64, 1000);

		WavToMono mono = new WavToMono(numberOfChannels, sampleSizeInBytes * 8);
		SignalProcessor signalProcess = new SignalProcessor(buf.length / 4);

		long startTime;
		long endTime;
		double averageTime = 0;
		int incrementor = 0;

		while ((numRead = stream.read(buf, 0, buf.length)) != -1) {

			startTime = System.nanoTime();

			monoAudio = mono.convertToMono(buf);

			rollingArray.addArray(signalProcess.hammingFFT(monoAudio));

			while (line.getFramePosition() / samplesInBuf < rollingArray.getLastAddedIndex() - rollingArray.getMemorySize() / 2) {
				try {
					Thread.sleep(1);
					//System.out.println(line.getFramePosition() / 64 + " < " + rollingArray.getMinIndexAvailable() + rollingArray.getMemorySize() / 2);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			//convert back to byte and small-endian
			//this will only work for 2 channel, 16 bit audio
			for (int i = 0; i < monoAudio.length; ++i) {
				buf[4 * i] = (byte) (monoAudio[i]);
				buf[4 * i + 1] = (byte) (monoAudio[i] >> 8);
				buf[4 * i + 2] = (byte) (monoAudio[i]);
				buf[4 * i + 3] = (byte) (monoAudio[i] >> 8);
			}

			endTime = System.nanoTime();
			averageTime = (averageTime + (endTime - startTime)) / 2;

			if (incrementor > (int) (numberOfChannels * sampleSizeInBytes * sampleRate) / buf.length) {
				System.out.println("Loop is taking on average " + averageTime / 1000000 + " milliseconds");
				incrementor = 0;
				averageTime = 0;
				//System.out.println(line.getFramePosition() / samplesInBuf);
				//System.out.println(rollingArray.getLastAddedIndex());
				//System.out.println(rollingArray.getMinIndexAvailable() + (rollingArray.getMemorySize() / 2));
				
			}

			incrementor++;



			int offset = 0;
			while (offset < numRead) {
				offset += line.write(buf, offset, numRead - offset);
			}


		}


		line.drain();
		line.stop();
	}

}