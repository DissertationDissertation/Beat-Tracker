package audio;

import javax.sound.sampled.SourceDataLine;

import fourier.DFTSearch;
import fourier.SignalProcessor;
import rollingmemory.RollingDoubleMemory;

public class BeatTracker extends DFTSearch{

	public boolean isBeat = false;

	// we work out how many windows large the rolling average will be, based off
	// of the window rate
	private int movingAverageSize;

	// this variable holds the value for the number of expected windows between any two beats
	private double expectedWindowsBetweenBeats;

	private int maxNumberofJumps;

	private RollingDoubleMemory denoisedImpulseRollingMemory;

	private RollingDoubleMemory volumeRollingMemory;

	private SignalProcessor signalProcess;
	
	private double difference;


	/**
	 * The beat tracker will create a signal processor that handles the size of interval
	 * inputed into this constructor
	 * @param windowsInMovAverage the number of windows we want in the moving average
	 * @param expectedWindowsBetweenBeats the number of windows that we expect will
	 * pass by before a new beat is found
	 */
	public BeatTracker(int windowsInMovAverage, double expectedWindowsBetweenBeats, int numOfBeatsInInterval, RollingDoubleMemory denoisedImpulseRollingMemory, RollingDoubleMemory volumeRollingMemory, int numOfWindowsInInterval){

		this.movingAverageSize = windowsInMovAverage;

		this.expectedWindowsBetweenBeats = expectedWindowsBetweenBeats;

		this.denoisedImpulseRollingMemory = denoisedImpulseRollingMemory;

		this.volumeRollingMemory = volumeRollingMemory;

		maxNumberofJumps = numOfBeatsInInterval;

		this.signalProcess = new SignalProcessor(numOfWindowsInInterval);

		int doubled = 1;
		while(doubled < numOfWindowsInInterval)
			doubled *= 2;

		this.difference = doubled / (double)numOfWindowsInInterval;
		
		}


	public double getVolume(double[] window) {

		double volume = 0;

		for (int i = 0; i < window.length; ++i) {
			volume += window[i];
		}

		return volume;
	}


	public double getLowFreqVolume(double[] window, double audioFrameRate) {

		int frequencyIncrements = (int) Math.ceil(audioFrameRate / window.length);

		double lowFreqVolume = 0;

		// only get volume lower than 300 Hz
		for (int i = 0; (i + 1) * frequencyIncrements < 800; ++i) {
			lowFreqVolume += window[i];
		}

		return lowFreqVolume;
	}


	public double getRMSVolume(int[] window) {

		double RMSVolume = 0;

		// only get volume lower than 300 Hz
		for (int i = 0; i < window.length; ++i) {
			RMSVolume += Math.pow(window[i], 2);
		}

		RMSVolume /= window.length;

		RMSVolume = Math.sqrt(RMSVolume);

		return RMSVolume;
	}


	/**
	 * This gets the impulse from two FFT windows and returns a double value telling the
	 * total impulse between the two windows. This is effectively differentiating the
	 * FFT windows, disregarding any negative differences, and then totalling up the sum
	 * @param newFFTWindow the FFT window being processed
	 * @param prevFFTWindow the last FFT window to be processed
	 * @return the impulse of the signal between the two windows
	 */
	public double getImpulse(double[] newFFTWindow, double[] prevFFTWindow) {

		double totalImpulse = 0;
		double impulse;

		for (int i = 0; i < newFFTWindow.length / 3; ++i) {

			impulse = newFFTWindow[i] - prevFFTWindow[i];
			if (impulse > 0) {
				totalImpulse += impulse;
			}

		}

		return totalImpulse;

	}



	public double trailedMovingAverage(double[] impulseArray) {

		double average = 0;
		double returnedImpulse;

		for (int i = impulseArray.length - movingAverageSize; i < impulseArray.length; ++i) {
			average += impulseArray[i];
		}
		average /= impulseArray.length;

		returnedImpulse = impulseArray[impulseArray.length - 1] - average * 1.2;

		return (returnedImpulse > 0)? returnedImpulse : 0;
	}





	/**
	 * This tracks the beat of the music, and returns a double[] array with useful information about
	 * what the beat could be, and then values pertaining to the confidence in the value given
	 * @param interval (the impulses of each window)
	 * @return a double[] array. [0] is the number of windows between each beat. [1] is the confidence
	 * of the beat tracker in the tempo just given. [2] is the amount of times that the tempo given
	 * hits an impulse when traced backwards from the current window
	 */
	public double[] trackBeat(double[] intervalImpulses, double[] intervalVolumes) {

		// decide how many cycles there will be in the interval if we use the value given in
		// 'expectedWindowsBetweenBeats'.
		double numOfCyclesInInterval = intervalImpulses.length / expectedWindowsBetweenBeats;

		// we want to create bins to search between which encompass the value above. we want the lower
		// bound to be half the size of the upper bound as well
		double lowerBin = numOfCyclesInInterval * 3.0 / 4;
		double upperBin = numOfCyclesInInterval * 3.0 / 2;

		// do DFT search between the bins to start off with
		// we have doubled the the lower and upper bins sizes because they
		double jump = search(intervalImpulses, 31, 5, lowerBin, upperBin);

		// To start off with, we want to convert the answer the DFT search spits
		// out into windows per cycle rather than cycles per interval
		// we multiply it by two since we doubled the size of the interval given to the search
		// when we mirrored the interval
		jump = (intervalImpulses.length) / jump;

		//System.out.format("%.1f BPM", 60000 / (jump * 3));

		// do it again for different harmonies of the tempo
		jump = search(intervalImpulses, 31, 5, lowerBin * 2, upperBin + lowerBin);

		jump = (intervalImpulses.length * 2) / jump;
		//System.out.format("	%.1f BPM \n", 60000 / (jump * 3));





		int newlowerBin = (int)(lowerBin * difference);

		// Harmonic FFT timing

		int numOfInputPoints = newlowerBin;
		int startingIndex = newlowerBin;
		int newArraySize = 700;

		double d = numOfInputPoints / (double)newArraySize;
		double[] outputHarmonicFFT = new double[newArraySize];

		double constant = 0;


		double[] answer = signalProcess.blackmanHarrisFFT(intervalImpulses);

		for (int j = 0; j < 6; j++) {
			for (int k = 0; k < newArraySize; ++k) {
				constant = startingIndex + d*k;
				outputHarmonicFFT[k] += ((answer[(int)constant + 1] - answer[(int)constant]) * (constant - (int)constant) +  answer[(int)constant]) * (j * 0.2 + 1);
			}
			startingIndex *= 2;
			numOfInputPoints *= 2;
			d = numOfInputPoints / (double) newArraySize;
		}

		double maxValue = 0;
		int maxValueIndex = 0;

		for (int j = 0; j < newArraySize; ++j) {
			if (outputHarmonicFFT[j] > maxValue){
				maxValue = outputHarmonicFFT[j];
				maxValueIndex = j;
			}
		}

		double ans = (maxValueIndex * (double)newlowerBin / outputHarmonicFFT.length + newlowerBin);



		jump = intervalImpulses.length * difference / ans;
		System.out.format("	%.1f BPM \n", 60000 / (jump * 3));



		// We should then also decide the initial error allowance for the first jump
		// we take. It is derived as a percentage of the number of windows there are in a jump
		double error = 0.05 * jump;

		// ensure the error is not below 1
		if (error < 1)
			error = 1.1;

		// create an accumulation value for of all the impulses from every jump we take
		// we initialise it with the impulse of the starting impulse already added
		double accumImpulses = intervalImpulses[intervalImpulses.length - 1];

		// we are jumping through the impulse array and we start at the end and jump backwards
		// therefore the first index we start at is this one
		int startJump = intervalImpulses.length - 1;

		// create a variable that decides where we land
		int landing;
		// create a variable that counts how many jumps we have done
		int numJumps = 0;
		// create a variable that counts how many hits we have (how many times we land on/around
		// an impulse
		int hits = 0;
		// create a variable that tallies up how may consecutive empty jumps we have made
		int emptyJumps = 0;


		while(true){

			// we decide where we land on our jump
			landing = startJump - (int) jump;

			// if we jump out of bounds of the array, or we have jumped onto empty spaces in the
			// array one too many times, we exit the loop
			// we also exit the loop if w have done the max number of jumps allowed
			if (landing < 0 || emptyJumps > 2 || numJumps >= maxNumberofJumps)
				break;

			// we have done another jump, so increment the number of jumps variable
			++numJumps;

			double maxImpulse = -10;
			int maxImpulseIndex = 0;

			// we search either side of the landing site for the maximum impulse within the error range
			for (int i = 0; i < error; ++i) {

				// checking the windows above the landing
				if (intervalImpulses[landing + i] > maxImpulse) {
					maxImpulse = intervalImpulses[landing + i];
					maxImpulseIndex = landing + i;
				}

				// checking the windows below the landing
				// we want to ensure that we don't go out of bounds of the array
				// when negating from indexes, hence the extra 'if' statement
				if (landing - i >= 0) {
					if (intervalImpulses[landing - i] > maxImpulse) {
						maxImpulse = intervalImpulses[landing - i];
						maxImpulseIndex = landing - i;
					}
				}

			}

			// there are two things that can happen, either an impulse is found,
			// or no impulse is found between the error bars of the landing site.
			// this 'if' statement decides between the two
			if (maxImpulse > 0) {
				// this means that there is an impulse within the error bars


				// we add the impulse to the accumulated impulse
				// accumImpulses += maxImpulse;

				// we add the volume at the impulse to add to the confidence value
				accumImpulses += intervalVolumes[maxImpulseIndex];

				// change the jump size in an attempt to make it more accurate
				jump = (intervalImpulses.length - 1 - maxImpulseIndex) / (double)numJumps;


				// now we have potentially made the jump more accurate, we can reduce the
				// error size by a set amount
				error *= 0.6;
				// we don't want the error to be below 1 however, so we increase it if
				// it does fall below 1
				if (error < 1)
					error = 1.1;

				// we set the new start of the jump to be the max impulse we found within
				// the error bar
				startJump = maxImpulseIndex;

				// we landed on or around an impulse, and that counts as a hit,
				// tally up accordingly
				++hits;

				// and finally, we could have broken a string of landing on nothing, so we 
				// set the empty jumps back to 0
				emptyJumps = 0;

			} else {
				// this means there was no impulse found within the error bars

				// we simply start jumping from where we landed
				startJump = landing;

				// we also acknowledge we landed on an empty patch of the impulse data
				++emptyJumps;
			}

		}


		// before we hand back the confidence value, we divide it by the number of windows in the interval
		// by doing this. This 'corrects' the confidence for the window size and means that we can fairly compare
		// two unequal sized windows, meaning the interval size can become a dynamic variable
		// accumImpulses /= intervalImpulses.length;

		return new double[]{jump, accumImpulses, hits, numJumps, intervalImpulses.length};
	}


	// a tally for how many failed predictions have occurred in a row
	int failedPredictions = 0;

	// a boolean describing if too many failed predictions have occurred in a row
	// if this is true, the confidence of the current tempo will start to fall with every failed
	// prediction
	boolean declineInConfidence = false;

	// create a value that saves what confidence the algorithm originally had in the
	// tempo when it discovered it
	double origionalTempoConfidence = 0;

	// create a value which holds how sure the maximum confidence the algorithm has achieved
	// in a tempo it has decided upon
	double tempoConfidence = 0;

	double intervalSizeForTempo = 1;

	int jumpsMadeForTempo = 1;

	// creates a value which tells of the index of the next window projected to have a beat in it
	double nextBeat = 1000000000;

	double prevBeat = 0;

	double windowsBetweenBeats = 0;

	/**
	 * This is should be called immediately after using 'trackbeat'
	 * The inputed arguments to this method must be exactly the same as the arguments
	 * returned by the method 'trackbeat'. It decides whether or not the most recent
	 * window is on a beat or not
	 * @return
	 */
	public boolean getBeat(double tempo[], int totalWindowsProcessed) {

		// System.out.format(" %.1f with %d hits and %d jumps\n", 60000 / (tempo[0] * 3), (int)tempo[2], (int)tempo[3]);

		// Reduces the confidence in misses by multiplying by this value
		// Increases confidence in hits by dividing by this value
		double confidenceVarier = 0.95;

		// this is returned by the method. It indicated if it is indeed a beat or not
		boolean isBeat = false;

		// if a new tempo comes along that has a higher confidence value than the one currently being
		// used, we immediately replace the old one with the new one
		// in order for a tempo to replace and older one, it must have a higher confidence value and must
		// have had a hit rate of at least 2 in the beat tracking
		if (tempo[1] > tempoConfidence && tempo[2] > 1) {
			System.out.format("-----new tempo = %.1f with %d hits-----\n", 60000 / (tempo[0] * 3), (int)tempo[2]);
			System.out.format("--confidence: %.1f\n", tempo[1] / 100);

			// we want to save the original confidence the beat tracker had in it's reading. The tempoConfidence
			// variable can decrease if we start to incorrectly guess the next beat
			windowsBetweenBeats = tempo[0];
			origionalTempoConfidence = tempo[1];
			jumpsMadeForTempo = (int) tempo[3];
			intervalSizeForTempo = tempo[4];

			tempoConfidence = origionalTempoConfidence;

			// forecast when the next beat will be
			nextBeat = totalWindowsProcessed + windowsBetweenBeats;

			// we set this beat to be a new beat only if the last window set to be a beat occurred over
			// 2 thirds of a a beat ago
			if (totalWindowsProcessed - windowsBetweenBeats * 2 / 3 > prevBeat) {
				isBeat = true; 
				prevBeat = totalWindowsProcessed;
			}

			// because a new tempo has been found, we need to set the failed predictions back to 0
			// and ensure that the confidence in the new tempo doesn't decline
			failedPredictions = 0;
			declineInConfidence = false;
		}


		// we set the allowable error either side of a prediction to be  derived as as fraction of the number
		// of windows between beats
		int errorInWindows = (int)(0.01 * windowsBetweenBeats);

		// we don't ever want this value to be lower than 1. If it is, we move it back up to 1
		if (errorInWindows < 1)
			errorInWindows = 1;

		//System.out.println(errorInWindows);

		// if the window where a beat is predicted to land comes, we check either side of the prediction
		// to see if an impulse does indeed occur. If not, the failed prediction tally goes up by one
		if ((int)nextBeat + errorInWindows <= totalWindowsProcessed)	{

			// go through the error bars (either side of the prediction) and check for the closest impulse
			double contenderImpulse = 0;
			double maxImpulse = 0;
			int impulseIndex = 0;
			for (int i = 0; i < (errorInWindows * 2) + 1; ++i) {
				contenderImpulse = denoisedImpulseRollingMemory.getDouble(totalWindowsProcessed - i);
				if (contenderImpulse > maxImpulse) {
					maxImpulse = contenderImpulse;
					impulseIndex = i;
				}

			}

			// if there were no impulses, that means that the prediction is wrong
			if (maxImpulse > 0) {
				// there was an impulse

				// we tweak where we jump from slightly, to be the closest impulse found within the error range
				// we then jump another 'beat' (windowsBetweenBeats)
				nextBeat = totalWindowsProcessed + windowsBetweenBeats - impulseIndex;

				prevBeat = totalWindowsProcessed - impulseIndex;

				// we reset the failed prediction tally and turn the decline in confidence off
				failedPredictions = 0;
				declineInConfidence = false;

				// we multiply the confidence by 1.5
				tempoConfidence += volumeRollingMemory.getDouble(totalWindowsProcessed - impulseIndex);

				// the confidence of a tempo can never exceed its original value given however
				if (tempoConfidence > origionalTempoConfidence) {
					tempoConfidence = origionalTempoConfidence;
					//System.out.format("confidence restored: %.1f\n", (tempoConfidence / 100));
				} else {
					if (tempoConfidence > 0.01) {} 
					System.out.format("confidence increase: %.1f\n", (tempoConfidence / 100));
				}



			} else {
				// there is no impulse within the error

				maxImpulse = 0;
				impulseIndex = 0;

				// go back and find the maximum impulse within a beat of the guess
				for (int i = 0; i < windowsBetweenBeats / 2; ++i) {
					if (denoisedImpulseRollingMemory.getDouble(totalWindowsProcessed - i) > maxImpulse) {
						maxImpulse = denoisedImpulseRollingMemory.getDouble(totalWindowsProcessed - i);
						impulseIndex = i;
					}
				}

				// if no impulse is found, we will

				// we tally on another failed prediction
				++failedPredictions;

				// we then set the next beat and hope for the best
				nextBeat += windowsBetweenBeats - errorInWindows;

				prevBeat = totalWindowsProcessed - errorInWindows;

				if (failedPredictions > 0) {
					declineInConfidence = true;
				}


				if (declineInConfidence) {
					if (maxImpulse > 0) {
						tempoConfidence -= volumeRollingMemory.getDouble(totalWindowsProcessed - impulseIndex);
					}
					if (tempoConfidence > 0.01) {}
					System.out.format("in decline: %.1f\n", (tempoConfidence / 100));

				}

			}

		}

		if ((int) nextBeat == totalWindowsProcessed) {
			isBeat = true;
			
			
		}

		return isBeat;

	}


}
