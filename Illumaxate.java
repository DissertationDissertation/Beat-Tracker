package audio;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

import fourier.SignalProcessor;
import rollingmemory.RollingArrayMemory;
import rollingmemory.RollingDoubleMemory;
import rollingmemory.RollingIntMemory;
import visual.BeatsPanel;
import visual.FFTPanel;
import visual.ImpulsePanel;
import visual.LongBeatsPanel;
import visual.PausePanel;
import visual.SpectrogramPanel;
import visual.VisualiserFrame;
import visual.VisualiserJPanel;
import visual.VolumePanel;

/**
 * This is the main part of the code that instantiates objects and pairs them
 * together The purpose of this code is to firstly perform live beat detection
 * on music It should then preempt when a chorus will occur and output a signal
 * once this happens indicating that the song has moved onto it's chorus This
 * algorithm is mostly to be used for the EDM genre
 * 
 * @author Clement Evans
 *
 */
public class Illumaxate {

	// The following variables are tweaking factors that can be changed by the user
	// before running the code to optimise performance / accuracy

	static String filename = "Deorro-FiveHours";
	
	// Firstly, set the file we want to play
	static File music = new File("inAudio/" + filename + ".wav");

	/*
	 * This sets the minimum frequency we want to detect in the music It should be
	 * considered that the lower the minimum frequency we want to detect in the
	 * music, the larger the FFT window we process. This means more computing power
	 * is used and it also means that the algorithm may not be quite as fast to
	 * react to the music. However, the resolution of the results we increases with
	 * a larger FFT window, so we can more precisely detect frequencies. A balance
	 * must be stuck for the best payoff between these things. It should also be
	 * noted that the minimum frequency stated here is likely not going to be the
	 * actual minimum frequency detected. The real value will probably be
	 * (potentially much) lower than the value inputed here. This is because we must
	 * find a window with size 2^n. The code will print the real minimum frequency
	 * detected to the console when it is run. The window size in frames will also
	 * be printed out onto the console.
	 * Use 23.43 to get a window of 2048
	 */
	static double minFrequency = 23.43; // in Hz

	/*
	 * This variable sets the minimum reaction time the output should have to the
	 * audio in milliseconds. This is to say, it must react *this* fast, or in fact
	 * faster. It is likely the code will decide on an approximation of the value
	 * chosen here, but it likely can't use the exact value due to rounding errors.
	 * The minimum reaction rate essentially decides on the overlap between each
	 * window being processed. A smaller reaction to the music requires a larger
	 * overlap. The overlap being used by the algorithm will be printed to the
	 * System console when the code is first run.
	 * A smaller reaction time to the music does not always improve the quality of
	 * results but may simply increase the processing power required to run the
	 * code. Also, if this value is too small, the program audio will start to
	 * stutter. This is because the main loop that passes the raw data bytes to the
	 * speaker has to loop more times (per second of audio). This means
	 * the calculations in the loop are done more times too and at a certain
	 * point, if the min reaction is too low, it can't keep up with the play back.
	 * The actual reaction time to the music will be printed to the system console
	 * when the code if first run
	 * The time is in milliseconds, so 6000 = 6 seconds
	 */
	static double minReaction = 3;	// in milliseconds

	/**
	 * This value is a rough estimate of what the BPM will be. This does not tell the
	 * algorithm the exact BPM, it doesn't really even in any way suggest what the
	 * BPM is. It instead creates  
	 * a window within which the algorithm looks for the
	 * correct BPM. For example, if a song has a BPM of 75, the algorithm won't know
	 * if it should output 75 BPM or double that at 150 BPM. The number here points
	 * towards whether we want a faster BPm or a slower one
	 */
	static int expectedBPM = 130;

	/**
	 * This is how many beats we want in the interval. The higher this value is, the
	 * larger the interval is. This value should never be less than 4, else a tempo
	 * will never be found, since a tempo must *hit* at least 4 impulses in the
	 * interval before it is even considered.
	 */
	static int numOfBeatsInInterval = 7;

	/**
	 * This variable is used for the beat tracking to negate any noise from the impulse
	 * data. This variable should not be larger than the length of an interval use for
	 * beat tracking. If it is, it will be made equal to the interval length. It should
	 * also not be smaller than the minReaction either, as otherwise, it will just. The final value for this variable will be printed on the console at the
	 * start. Due to rounding errors, the final number will be slightly different that
	 * the one inputed. They should be fairly close together though.
	 * The effect of this variable is slightly untested. If it's too small, there
	 * will be too much noise present in the impulse data to get a clear beat tracking.
	 * If it's too large, it may iron out and remove important impulses in the impulse
	 * data, leading to worse beat tracking. This variable is most certainly a
	 * twiddle factor. To reiterate, if this variable is the same as the minReaction,
	 * it will obliterate the impulse data, a correct value is needed
	 */
	static int movingAverageMillis = 100; // in milliseconds

	/**
	 * This variable determines the cap on how fast the visualisers will be updated
	 * in FPS. There are obvious down sides to setting this too high. The code
	 * translates this value into milliseconds per (visualiser)frame refresh. If
	 * this translated value is smaller than the minimum reaction to the audio, set
	 * above, it will automatically make them equal each other as there is no point
	 * in refreshing the data faster than it's being created as it will result in
	 * multiple draws of the same info. This value may be referred to as the
	 * 'refresh rate' in milliseconds in further variables and commenting to avoid
	 * confusion between the two meanings of the word 'frame', one meaning an audio
	 * frame and the other meaning a visual frame. The final refresh rate to be used
	 * will be printed to the console when the code if first run
	 */
	static int visualiserMaxFPS = 60; // in FPS


	public static void main(String[] args) throws UnsupportedAudioFileException, IOException, LineUnavailableException {

//				JFileChooser openDiag = new JFileChooser();
//		
//				File selected = null;
//		
//				if(JFileChooser.APPROVE_OPTION == openDiag.showOpenDialog(new JFrame())) {
//					selected = openDiag.getSelectedFile();
//				}

		// We now create an audio stream that effectively acts as a buffer for the whole song
		// and drip feeds the raw audio data in bytes when we ask to read() from it
		AudioInputStream musicData = AudioSystem.getAudioInputStream(music);

		// If it's recognised the file as an audio file it can read, we then want to extract
		// the details from the file
		AudioFormat musicFormat = musicData.getFormat();

		/*
		 * We now create a source data line info object, which we can then use the query
		 * the system's hardware to see if there is any speaker (port) that can play the
		 * music in the format that it's in. We also include a desired buffer size
		 * (which is more than likely not met). Here, we set the desired buffer size to be
		 * large enough to store the whole music file (which it won't meet)
		 */
		SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class, musicFormat,
				(int) (musicData.getFrameLength() * musicFormat.getFrameSize()));
		SourceDataLine speakerPort = (SourceDataLine) AudioSystem.getLine(info);
		// We query the hardware with the info object. If a suitable port doesn't exist,
		// a line unavailable error is thrown

		//-----------------------------------------------------------------------------
		// We now start to create some useful constants to be used throughout the code
		// many of these constant are self explanatory
		//-----------------------------------------------------------------------------
		int numberOfChannels = musicFormat.getChannels();
		int sampleSizeInBytes = musicFormat.getSampleSizeInBits() / 8;
		float audioFrameRate = musicFormat.getFrameRate();
		// int portBufferSizeInFrames = speakerPort.getBufferSize() / (numberOfChannels * sampleSizeInBytes);

		// Calculate the window size to be used, based off of
		// minimum frequency we want to detect in the music
		// @see minFrequency
		int returnedWindowSizeInFrames = 1;
		while(returnedWindowSizeInFrames < (int)(audioFrameRate / minFrequency)) {
			// continue multiplying by two until the window size is larger than the frame
			// rate divided by the minimum frequency. We multiply by two to ensure that the
			// window size is of size 2^n
			returnedWindowSizeInFrames *= 2;
		}
		
		// This gives the size of the array that will be given to the Signal Processor object
		int windowSizeInFrames = (int) (audioFrameRate / minFrequency);

		// Calculate the offset from window to window (can be translated into overlap)
		// use the minimum reaction time requested to work out this value
		// As a note, the offset also describes the number of frames given to the
		// speaker port for every byte buffer passed to the write() method
		int windowOffsetInFrames =  (int) ((audioFrameRate * minReaction) / 1000);

		// We want, at the least, each window to be adjacent to each other, so, if the
		// offset is worked out to be larger than the window size, we will set the
		// offset to equal the window size. This will also change the minimum reaction
		if (windowOffsetInFrames > windowSizeInFrames)
			windowOffsetInFrames = windowSizeInFrames;

		// We can now work out the time it takes for one window to be fully played
		// (in milliseconds)
		float timeForWindowMillis = (float) windowSizeInFrames / (float) audioFrameRate;

		// if the step above does actually change the window offset, the minimum reaction
		// time to the music will change too, so we create a variable to store the new
		// reaction time. If the window offset is not changed by the step above, then
		// the actualReactionTime will equal the inputed minReaction
		float actualReactionTime = (((float) windowOffsetInFrames / (float) audioFrameRate)) * 1000;

		// convert the BPM into seconds per beat
		double expectedSPB = (double)60 / expectedBPM;

		// convert the seconds per beat into window offsets per beat. * 1000 because we need to
		// convert to milliseconds first. This value is used when the beat tracker is first
		// initialised
		double windowOffsetsPerBeat = 1000 * expectedSPB / actualReactionTime;

		// we want to calculate the initial interval size in windows used by the beat tracker.
		// We set the initial interval of a size that is able to hold at as many beats as the expected
		// BPM. We actually set it to be a bit bigger to account for if the BPM is lower than expected,
		// and so otherwise less beats would fit into the interval
		int windowsInInterval = (int) (windowOffsetsPerBeat * numOfBeatsInInterval * 1.5);

		// we also want to know how long this interval is in seconds for the stats at the start
		float initialIntervalSize = windowsInInterval * actualReactionTime / 1000;

		// decide on how large the interval will be for the tempogram. This is a completely
		// separate interval to the one used in the actual beat tracking. This is done
		// purely for visualisation purposes only. We need to ensure that there are only
		// 2^n number of windows
		int tempogramIntervalSizeInWindows = 1;
		while(tempogramIntervalSizeInWindows <= (windowOffsetsPerBeat * numOfBeatsInInterval)) {
			// continue multiplying by two until the amount of windows in the interval
			// is larger than the required size to be of 2^n.
			tempogramIntervalSizeInWindows *= 2;
		}

		// This is the actual interval size to be used by the code. This value is written in seconds,
		// not milliseconds
		float minTempogramInterval = (tempogramIntervalSizeInWindows * windowOffsetInFrames) / audioFrameRate;

		// we ensure that the moving average length (size) in milliseconds is
		// longer than the min reaction time
		if (movingAverageMillis < actualReactionTime) {
			movingAverageMillis = (int) Math.ceil(actualReactionTime);
		}

		// we can now find out the number of windows that will be in each moved average
		int windowsInMovAverage = (int) (movingAverageMillis / actualReactionTime);

		// we can now find the actual length of the moving average size in milliseconds
		float actualRollAvrgMillis = windowsInMovAverage * actualReactionTime;

		// Calculate the refresh rate of the visualisers, in milliseconds
		int refreshRate = 1000 / visualiserMaxFPS;

		// There is no point in the time for a refresh (in milliseconds) to be smaller
		// than the minimum reaction to the music. If the refresh rate (in milliseconds)
		// is lower, make them equal
		if (refreshRate < actualReactionTime)
			refreshRate = (int) actualReactionTime;

		// Print out any variables useful to know that may have been changed by the code
		System.out.println("Window size in frames = " + windowSizeInFrames);
		System.out.println("Window size in with padding frames = " + returnedWindowSizeInFrames);
		System.out.println("Minimum frequency to be detected = " + (int) Math.ceil(audioFrameRate / windowSizeInFrames) + " Hz");
		System.out.format("Offset of frames = %d (%.1f%% overlap)\n", windowOffsetInFrames, (100 - (100 * windowOffsetInFrames) / (float) returnedWindowSizeInFrames));
		System.out.format("Reaction time to music = %.1f milliseconds\n", actualReactionTime );
		System.out.format("Time for one window = " + "%.1f" + " milliseconds\n", timeForWindowMillis * 1000);
		System.out.format("Initial beat tracking interval = %.1f seconds\n", initialIntervalSize);
		System.out.format("Tempogram interval in seconds = %.1f seconds\n", minTempogramInterval);
		System.out.format ("Moving average = %.1f milliseconds\n", actualRollAvrgMillis);
		System.out.println("Number of windows in moving average = " + windowsInMovAverage);
		System.out.format("Refresh rate cap for visualiser = %.0f FPS (%d milliseconds)\n", (1000 / (float) refreshRate), refreshRate);

		// find the maximum value one frame can take. This is used to guide the visualisers in the
		// values they show
		int maxValueOfSample = 0;
		for (int i = 0; i < sampleSizeInBytes; ++i) {
			maxValueOfSample = maxValueOfSample | 0xFF << 8 * i;
		}
		maxValueOfSample = maxValueOfSample / 2 + 1;



		/*
		 * Create a byte buffer. This is what will be used to transport small snippets
		 * of raw audio data from the musicData stream to the port (speaker) buffer
		 * where it is then played.
		 * We want the size of the buffer to be able to carry the offset (in frames) each
		 * time to the speaker port. Since there are numberOfChannels * sampleSizeInBytes
		 * bytes in each frame, we multiply this by the offset size to get the size of
		 * the buffer
		 *
		 * Although this is briefly explained above at windowOffsetInFrames, to
		 * reiterate, the byte buffer's size will holds as much data as there is
		 * audio data in the offsets between windows
		 * For more intuitive code comprehension, I will create a variable for
		 * the byte buffer's size in frames, even though they are the same thing
		 */	
		int bufSizeInFrames = windowOffsetInFrames;
		int bufSizeInBytes = bufSizeInFrames * numberOfChannels * sampleSizeInBytes;
		byte[] buf = new byte[bufSizeInBytes];

		// we want to find out how many frames are in 10 seconds of audio. This will inform
		// how big we want out our rolling array that is used by the visualiser panels to pick
		// out audio information that has already been processed but is only just being played.
		// This is equivalent to how many loops of buf transfer we have to do for the speaker
		// port to transport 10 seconds of audio;
		int tenSecsOfWindows = (int) ((audioFrameRate * 10) / bufSizeInFrames);

		// set up a rolling array memory for temporarily storing the data processed using
		// the FFT. This array holds the FFT spectrum windows
		// The number of arrays it stores will be large enough to fit around 10 seconds of audio.
		RollingArrayMemory fftSpectRollingArray = new RollingArrayMemory(returnedWindowSizeInFrames, tenSecsOfWindows);

		// set up another rolling array memory for temporarily storing the data processed using
		// the beat tracker. This array holds the tempogram data.
		// The number of array it stores will also be large enough to fit 10 seconds of audio
		RollingArrayMemory tempoRollingArray = new RollingArrayMemory(tempogramIntervalSizeInWindows, tenSecsOfWindows);

		// create a rolling array to store the impulse of the music, which is found by the beat tracker object
		// this memory need only hold a moving average number of windows, since this is all it's used for before
		// the data is placed inside the denoised impulse array for longer term storage. No panels should read off
		// of this rolling memory and should read the denoisedImpulseRollingMemory instead
		RollingDoubleMemory impulseRollingMemory = new RollingDoubleMemory(windowsInMovAverage);

		// create a rolling array to store the denoised impulse of the music for the FFT spectrogram, which is also used by the
		// beat tracker object. It needs to accommodate 10 seconds of data and/or all the interval data needed to create an FFT
		// of the interval for a tempogram. It also needs to accommodate the interval used for the actual beat tracking
		// hence we choose the bigger of these three values
		int maxValue = (tenSecsOfWindows > tempogramIntervalSizeInWindows)? ((windowsInInterval*4 > tenSecsOfWindows)?
				windowsInInterval*4 : tenSecsOfWindows) :
					((windowsInInterval*4 > tempogramIntervalSizeInWindows)? windowsInInterval*4 : tempogramIntervalSizeInWindows);
		RollingDoubleMemory denoisedImpulseRollingMemory = new RollingDoubleMemory(maxValue);
		// the reason why quadruple the windows in interval variable is because this variable changes dynamically as the code is
		// being run, and so we want to ensure that the rolling array can store a full interval, even if it the interval becomes =
		// larger. The interval will most likely never ever balloon to x4 it's original size

		// This rolling memeory is created for the impulse panel to read off of. Th impulse panel will only show an impulse
		// when the value in this rolling array changes
		RollingDoubleMemory impulseRollingMemoryReading = new RollingDoubleMemory(maxValue);

		// set up a rolling array memory for the volume data, to be used by the beat tracker for confidence values
		RollingDoubleMemory volumeRollingMemory = new RollingDoubleMemory(maxValue);

		// set up a rolling array memory for the low volume data, to be used by the beat JPanel
		RollingDoubleMemory lowFreqVolumeRollingMemory = new RollingDoubleMemory(tenSecsOfWindows);

		// set up a rolling array memory for the rms volume data, to be used by the beat JPanel
		RollingDoubleMemory RMSVolumeRollingMemory = new RollingDoubleMemory(tenSecsOfWindows);

		// set up a rolling array memory for temporarily storing the beats predicted to occur within the audio
		// This memory will only be used by the visualiser panels and so only needs to fit around 10 seconds of audio.
		RollingDoubleMemory beatsRollingMemory = new RollingDoubleMemory(tenSecsOfWindows);


		// set up an integer rolling memory for storage of the mono samples created by the
		// WavToMono object every loop
		// it will be read to create an fft every turn
		RollingIntMemory rollingInt = new RollingIntMemory(windowSizeInFrames);

		// create a wave to mono object that will translate the small-endian bytes in the
		// buffer into a large-endian integer array, one integer for each frame
		WavToMono mono = new WavToMono(numberOfChannels, sampleSizeInBytes * 8);

		// create an signal processor object to perform FFTs on the mono audio arrays, and
		// to do other useful calculations on the audio data
		SignalProcessor signalProcess = new SignalProcessor(windowSizeInFrames);

		// create another signal processor. This is made in case we need to have a look at an FFT
		// of the impulse data to draw a tempogram, otherwise, this object will not be used
		SignalProcessor impulseProcess = new SignalProcessor(tempogramIntervalSizeInWindows);

		// create a beat tracker object to track the beats from impulse data. We give it the rolling array to read off
		// of in order to make decisions
		BeatTracker beatTracker = new BeatTracker(windowsInMovAverage, windowOffsetsPerBeat, numOfBeatsInInterval, denoisedImpulseRollingMemory, RMSVolumeRollingMemory, windowsInInterval);

		// Create a panel for visualising the volume from the FFT
		VisualiserJPanel volumePanel = new VolumePanel(30, 150, RMSVolumeRollingMemory);
		// Create all the JPanels, which are panels used to visualise the data created
		// The FFT panel is used to visualise any FFT data
		// We must also tell the FFT panel which rolling memory it will be reading off of
		VisualiserJPanel fftPanel = new FFTPanel(440, 150, fftSpectRollingArray);
		// Create a panel that is useful for visualising any beats the algorithm
		// believes have occurred
		VisualiserJPanel beatsPanel = new BeatsPanel(70, 150, beatsRollingMemory);

		VisualiserJPanel pausePanel = new PausePanel(290, 80, speakerPort, filename);
		// Create another spectrogram panel but this one it to be used to display the
		// the tempogram (tempo data) rather than the FFT data
		VisualiserJPanel tempogramPanel = new SpectrogramPanel(100, 150, tempoRollingArray, 0.05, 20, true);
		// The impulse panel is used to visualise the impulse of the audio over time
		// we tell the panel which memory it will be polling to get values
		VisualiserJPanel impulsePanel = new ImpulsePanel(480, 100, impulseRollingMemoryReading);

		VisualiserJPanel longBeatsPanel = new LongBeatsPanel(480, 10, beatsRollingMemory);
		// The spectrogram panel is used to visualise FFT data over time
		// We must also tell the spectrogram panel which rolling memory it will be reading off of
		VisualiserJPanel spectrogramPanel = new SpectrogramPanel(480, 250, fftSpectRollingArray, 40, 2.5, false);

		// Store the JPanels in an array that can then be sent to the JFrame. Any panels
		// omitted from this array will not be added to the JFrame
		VisualiserJPanel[] addPanels = {volumePanel, fftPanel, beatsPanel, pausePanel, tempogramPanel, impulsePanel, longBeatsPanel, spectrogramPanel};

		// create the visualiser JFrame and pass the array of JPanels to add to it
		// pass also the speaker line to query about which frame it is playing,
		// the buffer size in frames to ascertain the correct index in the rolling
		// array, and finally, the refresh rate wanted
		VisualiserFrame visuals = new VisualiserFrame(addPanels, speakerPort, bufSizeInFrames, refreshRate);

		// create a harmonic beat search plotter
		HarmonicTempoPlotter tempoHarmonies = new HarmonicTempoPlotter();
		
		// for writing where the beats are
		PrintStream ps = new PrintStream(new File(filename + " beat predictions.txt"));
		
		ps.println("minfrequency = " + minFrequency);
		ps.println("minReaction = " + minReaction);
		ps.println("expectedBPM = " + expectedBPM);
		ps.println("numOfBeatsInInterval = " + numOfBeatsInInterval);
		
	      

		// create an int that gives the number of bytes read from the music data into
		// the byte buffer every loop
		// Unless the music is just about to end, this value should always be equal
		// to bufSizeInBytes
		int numBytesRead;

		// a counter for how many frames have been read by the buffer
		int totalFramesRead = 0;

		// this tells the window of the most recent impulse peak
		double maxImpulse = 0;

		int maxImpulseWindow = 0;

		double prevValue = 0;

		// a counter for how many windows have been processed
		// (this is basically how many times we have gone through the loop)
		int totalWindowsProcessed = 0;

		// number to add to beat tracker. This is arbitrary and should eventually
		// be replaced by the amplitude of the bass frequencies in the certain window
		double numForBeats = 0;

		// finally, we get the (speaker) port ready to receive data by opening it
		speakerPort.open(musicFormat);

		// show the visualisers and start the thread that is used for timing each refresh
		visuals.show();

		// now we get on to the audio loop and logic behind the code
		// To start with we read a buffer's worth of information from the audioData
		// This buffer should hold as many frames as there are in the window offset
		while ((numBytesRead = musicData.read(buf, 0, buf.length)) != -1) {

			// add on to the counter the new frames that have just been read by the byte buffer
			totalFramesRead += bufSizeInFrames;

			// create mono integers from the multi-channel byte data and store them in the
			// rolling integer array
			rollingInt.addIntArray(mono.convertToMono(buf));

			// add the RMS volume for the window to the rolling memory
			RMSVolumeRollingMemory.addDouble(
					beatTracker.getRMSVolume(
							rollingInt.getIntArray(totalFramesRead - windowSizeInFrames, windowSizeInFrames)));


			// now this is a complicated one. Were I to do some optimising, I might start here.
			// what we are doing is reading the mono values for the past window from the rolling
			// integer array, we are then using the hamming window FFT method from the
			// signal processing object and then we add the processed data into a large rolling array	
			double[] doubleArray = signalProcess.log(
					signalProcess.blackmanHarrisFFT(
							rollingInt.getIntArray(totalFramesRead - windowSizeInFrames, windowSizeInFrames)));	

			fftSpectRollingArray.addArray(doubleArray);

			volumeRollingMemory.addDouble(
					beatTracker.getVolume(
							doubleArray));

			lowFreqVolumeRollingMemory.addDouble(
					beatTracker.getLowFreqVolume(
							doubleArray, audioFrameRate));

			// find the new impulse number for the window and add it to the impulse rolling memory
			impulseRollingMemory.addDouble(
					beatTracker.getImpulse(
							doubleArray, fftSpectRollingArray.getArrayPointer(
									fftSpectRollingArray.getLastAddedIndex() - 1)));

			denoisedImpulseRollingMemory.addDouble(
					beatTracker.trailedMovingAverage(
							impulseRollingMemory.getDoubleArray(totalWindowsProcessed - windowsInMovAverage + 1, windowsInMovAverage)));


			double currentImpulse = denoisedImpulseRollingMemory.getDouble(totalWindowsProcessed);


			//						if (currentImpulse == 0) {
			//							maxImpulse = 0;
			//							maxImpulseWindow = 0;
			//							
			//						}else if(maxImpulseWindow == 0){
			//							maxImpulse = currentImpulse;
			//							maxImpulseWindow = totalWindowsProcessed;
			//							
			//						} else if (currentImpulse > maxImpulse) {
			//							denoisedImpulseRollingMemory.overwriteDouble(maxImpulseWindow, 0.0);
			//							maxImpulse = currentImpulse;
			//							maxImpulseWindow = totalWindowsProcessed;
			//			
			//						}else{
			//							denoisedImpulseRollingMemory.overwriteDouble(totalWindowsProcessed, 0.0);
			//						}

			if (currentImpulse != 0)
				impulseRollingMemoryReading.addDouble(currentImpulse);
			else
				impulseRollingMemoryReading.addDouble(impulseRollingMemoryReading.getDouble(totalWindowsProcessed - 1));



			//lastImpulseValue;



			// create a new column for the tempogram array which can be read and visualised by one of
			// the JPanels later on
			tempoRollingArray.addArray(
					impulseProcess.sqrt(
							impulseProcess.blackmanHarrisFFT(
									denoisedImpulseRollingMemory.getDoubleArray(
											totalWindowsProcessed - tempogramIntervalSizeInWindows + 1, tempogramIntervalSizeInWindows))));


			double[] tempo;


			if (denoisedImpulseRollingMemory.getDouble(totalWindowsProcessed) > 0) {
				tempo = beatTracker.trackBeat(
						denoisedImpulseRollingMemory.getDoubleArray(
								totalWindowsProcessed - windowsInInterval + 1, windowsInInterval),
						RMSVolumeRollingMemory.getDoubleArray(totalWindowsProcessed - windowsInInterval + 1, windowsInInterval));
			} else {
				tempo = new double[5];
			}

			if(beatTracker.getBeat(tempo, totalWindowsProcessed)) {
				numForBeats = lowFreqVolumeRollingMemory.getDouble(totalWindowsProcessed);
				ps.println((int)((totalWindowsProcessed + 1) * actualReactionTime));
			}

			beatsRollingMemory.addDouble(numForBeats);
			
			if (denoisedImpulseRollingMemory.getDouble(totalWindowsProcessed) > 0) {
				tempoHarmonies.getTempoHarmonies(denoisedImpulseRollingMemory.getDoubleArray(
						totalWindowsProcessed - windowsInInterval + 1, windowsInInterval),
						RMSVolumeRollingMemory.getDoubleArray(totalWindowsProcessed - windowsInInterval + 1, windowsInInterval),
						(double)(actualReactionTime * totalWindowsProcessed), windowOffsetsPerBeat);
			}




			// before we finally give the buf to the speaker port, we want to acknowledge that
			// we have processed another window
			++totalWindowsProcessed;

			// We cannot let the rollingArray roll over values that pertain to samples that still
			// haven't been played yet. If we do this, the visualisers will not be able to retrieve
			// data for the frames as they are being played. Because of this, we check which frame
			// the speakerPort has just played and ensure that the code is only ever 1 / 4 of the 
			// rolling array memory's size ahead of this frame. In this way, it won't process too far
			// ahead compared to the play back
			while(speakerPort.getFramePosition() / bufSizeInFrames < fftSpectRollingArray.getLastAddedIndex() - (tenSecsOfWindows / 4)) {

				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				// if the speaker port isn't active, we wait for a longer amount of time
				if (!speakerPort.isActive()) {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

			} 
			// the while statement is asking:
			// if the speaker port were counting how many buffer's worth of data it's storing, is this value lower
			// than 3 / 4 of the available indexes in the rolling array. If it isn't, this loop gives a chance for the
			// speaker to catch up


			// give the byte buffer, containing the raw audio data, to the speaker port's buffer to 
			// be played (most likely at a later time than when it is given, to allow smooth playback)
			int offset = 0;
			while (offset < numBytesRead) {
				offset += speakerPort.write(buf, offset, numBytesRead - offset);
			}
		}

		speakerPort.drain();
		speakerPort.stop();
		ps.close();

		tempoHarmonies.plotTempoHamonies();

	}


}
