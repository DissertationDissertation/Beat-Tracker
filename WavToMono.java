package audio;

// wav to mono can process up to 24 bit music
// it can handle any amount of channels
public class WavToMono {
	
	final int numberOfChannels;
	final int sampleSizeInBytes;
	
	int channelInt;
	// create an integer that is used as a placeholder for byte data to be loaded into
	
	/**
	 * This object contains a method to transfer small-endian audio in a byte buffer
	 * into a large endian integer array. It can handle an unlimited number of channels 
	 * and up to 24 bit audio
	 * @param numberOfChannels the number of channels present in the audio
	 * @param sampleSizeInBits the number of bits for one sample in one of the channels
	 */
	WavToMono(int numberOfChannels, int sampleSizeInBits){
		
		// do a quick check to make sure that the sample size in bits is
		// an integer multiple of 8 and than it is 24 bits or below,
		// since the method will only be able to handle up to 24 bit audio
		if ((sampleSizeInBits > 24) || (sampleSizeInBits % 8 != 0)) {
			throw new IllegalArgumentException("The WavToMono method cannot process " + sampleSizeInBits + "bit audio");
		}
		
		// if all is good with the sampleSize, convert it to size in bits and then
		// store it in the object variable sampleSizeInBytes
		sampleSizeInBytes = sampleSizeInBits / 8;
		
		// store the number of channel into the object variable numberOfChannels
		this.numberOfChannels = numberOfChannels;
				
	}
	
	
	
	/**
	 * This method can be used to translate raw small-endian, signed audio in a byte buffer
	 * into a large-endian integer array. It can handle an unlimited number of channels 
	 * and up to 24 bit audio
	 * @param byteData the raw data from the wav file
	 * @return a new integer array of mono audio values, one for each frame of audio
	 */
	public int[] convertToMono(byte[] byteData){
		
		// the byteData must be divisible by (number of channels * sample size in bytes)
		// if it's not, that means the data is incomplete and the method cannot work out a mono signal
		if (byteData.length % (numberOfChannels * sampleSizeInBytes) != 0) {
			throw new IllegalArgumentException("The byte array passed to this method did not include every byte for a " + numberOfChannels + " channel, " + sampleSizeInBytes * 8 + "bit signal");
		}
		
		// create an integer vector which will store the mono values of all the channels combined
		// it will be smaller than the byte array since the number of channels collapses to 1
		int[] intMonoData = new int[byteData.length / (numberOfChannels * sampleSizeInBytes)];
		
		// most of what happens from here on out is a carbon copy of what happens in "HowMonoWorks" in
		// "TestingThings"
		// confused about something? try looking there to see what's up
		for (int f = 0; f < byteData.length / (numberOfChannels * sampleSizeInBytes); ++f) {
			
			for (int c = 0 ; c < numberOfChannels * sampleSizeInBytes; c = c + sampleSizeInBytes) {
				
				channelInt = byteData[(f * numberOfChannels * sampleSizeInBytes) + (c + sampleSizeInBytes - 1)] << 8 * (sampleSizeInBytes - 1);
				
				for (int i = 0; i < sampleSizeInBytes - 1; ++i) {
					
					channelInt = channelInt | ((byteData[(f * numberOfChannels * sampleSizeInBytes) + (c + i)] & 0xFF) << i * 8);
				}
				
				intMonoData[f] += channelInt;
			}
			
			intMonoData[f] = intMonoData[f] / numberOfChannels;
			// divide by the number of channels to get an average for the frame
			
		}
		
		return intMonoData;
		
	}
	
	

}
