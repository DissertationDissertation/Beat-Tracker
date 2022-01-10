package fourier;

public class DFTSearch {


	/**
	 * Attempts to find a repetitive pattern in the signal (interval) given to it. The period of
	 * the pattern will only be between the start and end bins given to the method. It first does
	 * a spread out search with n intervals (input argument) and then does a binary search m
	 * times (also input argument) to home in on the pattern's period.
	 * @param window is the signal or interval of impulse data to be processed to find a pattern
	 * @param numOfIntervals the number of initial searches in the spread out search before binary
	 * searching commences
	 * @param binarySearches the number of binary searches carried out to home in on the correct
	 * period
	 * @param startBin the start bin to search for
	 * @param endBin the end bin to search for
	 * @return the period which most correlates to the data. The period is as a fraction of the
	 * window/signal length given to it. (ie, a period of 14.65 means there are 14.65 cycles of
	 * the repeated pattern in the data
	 */
	public double search(double[] window, int numOfIntervals, int binarySearches, double startBin, double endBin) {

		// We first want to process the data to remove any zero values
		// We take out the zero values by making an array which is already the size of the window.
		// We make it equal in size, just in case of the (pretty much impossibility) of there being
		// no zero values in he given window
		double[] smallWindow = new double[window.length];
		// We need to make another array, telling us of the original indexes of the values we picked
		// out from the window
		int[] smallWindowIndexes = new int[window.length];
		// We also need a value to tell us how many relevant values exist within the small window
		int smallWindowLength = 0;

		// now we iterate through the window randomly pick out the non-zero values, adding them
		// sequentially to our smallWindow and increasing it's 'length' by one. We also add a
		// Black-man harris window as we do this
		for (int i = 0; i < window.length; ++i) {
			if (window[i] > 0) {
				smallWindow[smallWindowLength] = window[i] * (0.54 - 0.46 * Math.cos((2 * Math.PI * i) / window.length));
				smallWindowIndexes[smallWindowLength] = i;
				++smallWindowLength;

			}
		}

		// We want to find out the range, which we get in the following way
		double range = endBin - startBin;

		// We want to search between the range at regular intervals. Say the amount of intervals used as
		// an argument for this method = 6. We will actually do 7 initial DFT searches like so:
		// 
		//	V						V       <--- location of start and end bins
		//	|	|	|	|	|	|	|       <--- searches
		//    1   2   3   4   5   6			<--- intervals
		//
		// So in this case, we need to create an array of length 7 to store all of the bins we will
		// do DFTs for

		double[] binIntervals = new double[numOfIntervals + 1];

		for (int i = 0; i < numOfIntervals + 1; ++i) {
			binIntervals[i] = startBin + (i * range) / numOfIntervals;
		}

		// Now we do the DFT's for every single binInterval and place the power responses in a new array,
		// where each index's value in the new array corresponds to the same index's value in binInterval
		double[] binPowers = new double [numOfIntervals + 1];

		// To compute the DFT, we also need a double[] to the imaginary and real parts of the DFT equations.
		// It is only a temporary storage variable that it overwritten with each new DFT
		// [0] is real, [1] is imaginary
		double[] imagNum = new double[2];

		for (int i = 0; i < binIntervals.length; ++i) {

			// this is a constant used for every loop of the DFT, and so it's easier and faster
			// if we just work it before going into the loop, rather than in every loop of the DFT
			double constant = binIntervals[i] * 2 * Math.PI / smallWindow.length;

			// reset imagNum
			imagNum[0] = 0;
			imagNum[1] = 0;

			// Here is the main part of the DFT
			for (int j = 0; j < smallWindowLength; ++j) {
				imagNum[0] += smallWindow[j] * Math.cos(constant * smallWindowIndexes[j]);
				imagNum[1] += smallWindow[j] * Math.sin(constant * smallWindowIndexes[j]);
			}
			// We don't bother square rooting as it's just extra processing that actually won't
			// affect which power is the maximum, which is all we are interested in. We don't care
			// about what specifically each power is
			binPowers[i] = Math.pow(imagNum[0], 2) + Math.pow(imagNum[1], 2);
		}

		// We now have 2 arrays: binIntervals and binPowers. The indexes in both correspond to each other

		// We iterate through binPowers and find the maximum value in it. We store this is a new variable
		// and put the bin it corresponds to in another variable
		double bestPower = 0;
		double bestBin = 0;

		for (int i  = 0; i < binPowers.length; ++i) {
			if (binPowers[i] > bestPower) {
				bestPower = binPowers[i];
				bestBin = binIntervals[i];
			}
		}

		// Now we have these vales, we can move onto a binary search
		// An full explanation for how this is done is on the iPad notes somewhere

		// We need to work out the span of the binary search first
		// Initially, we work out the span in the following way

		double span = range / (numOfIntervals * 2);

		// We now start the binary search loop.
		for (int i = 0 ; i < binarySearches; ++i) {

			// We create two contender bins, either side of the best bin, to challenge the best bin
			// and see either of their power responses are larger than the best bin's power response. The
			// one with the largest power response becomes the new best bin and best power

			double contender1Bin = bestBin - span;
			double contender2Bin = bestBin + span;

			double contender1Power = 0;
			double contender2Power = 0;

			// Do a DFT for each contender

			double constant;
			// reset imagNum
			imagNum[0] = 0;
			imagNum[1] = 0;

			// contender 1 DFT
			constant = contender1Bin * 2 * Math.PI / smallWindow.length;
			for (int j = 0; j < smallWindowLength; ++j) {
				imagNum[0] += smallWindow[j] * Math.cos(constant * smallWindowIndexes[j]);
				imagNum[1] += smallWindow[j] * Math.sin(constant * smallWindowIndexes[j]);
			}
			contender1Power = Math.pow(imagNum[0], 2) + Math.pow(imagNum[1], 2);

			// reset imagNum
			imagNum[0] = 0;
			imagNum[1] = 0;

			// contender 2 DFT
			constant = contender2Bin * 2 * Math.PI / smallWindow.length;
			for (int j = 0; j < smallWindowLength; ++j) {
				imagNum[0] += smallWindow[j] * Math.cos(constant * smallWindowIndexes[j]);
				imagNum[1] += smallWindow[j] * Math.sin(constant * smallWindowIndexes[j]);
			}
			contender2Power = Math.pow(imagNum[0], 2) + Math.pow(imagNum[1], 2);

			// We can now decide which of three bins has the largest power response. Whichever one
			// does, it becomes the bestBin, and it's power, the bestPower. If the original bestBin
			// has the best power, it maintains being the best bin. We then repeat, halving the span
			// each time

			if (contender1Power > bestPower) {
				bestPower = contender1Power;
				bestBin = contender1Bin;
			} else if (contender2Power > bestPower) {
				bestPower = contender2Power;
				bestBin = contender2Bin;
			}

			span /= 2;
		}

		// once the binary search is complete, we now have a bin which should (most of the time)
		// be the bin that holds the largest power response within the range given. (actually, it could be
		// a little outside of the range, but that's actually quite useful)

		return bestBin;

	}



}
