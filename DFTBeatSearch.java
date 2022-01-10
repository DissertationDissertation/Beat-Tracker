package fourier;

/**
 * Used to do a binary search between two bins to quickly find a peak value
 * @author Clement
 *
 */
public class DFTBeatSearch {

	int windowLength;

	double twoPiOverOne = 1 / (Math.PI * 2);

	// used to keep a constant so it doesn't keep on having to be worked out
	double twoPiOverWindowLength;

	// used to keep a different constant so it doesn't keep on being needed to be worked out
	double freqBinxtwoPiOverWindowLength;

	double[] fourierOutput = new double[2];

	double[] intialBins;

	double[] initialBinsResult;

	int intitialNumberofBins;

	double range;

	int indexOfMaxValue;

	double[] searchBetweenBins = new double[2];

	double[] window;



	DFTBeatSearch(int initialNumberofDFTs){


		// we ensue the array used for storing the first part of the search is large
		// enough to store the values for the initial DFT
		intialBins = new double[initialNumberofDFTs];
		initialBinsResult = new double[initialNumberofDFTs];

		this.intitialNumberofBins = initialNumberofDFTs;

	}

	public double doSearch(double[] window, double[] binRange, int numberOfIterations) {

		this.searchBetweenBins[0] = binRange[0];
		this.searchBetweenBins[1] = binRange[1];

		this.window = window;

		// work out the constants that will be used in the search
		windowLength = window.length;
		twoPiOverWindowLength = 2 * Math.PI / windowLength;

		for (int i = 0; i < numberOfIterations; ++i) {
			initialSearch();
		}


		return intialBins[indexOfMaxValue];

	}


	private void initialSearch(){

		// find the range of bins we need to search for
		range = searchBetweenBins[1] - searchBetweenBins[0];

		// we fill up the double array that stores which bins we will initially
		// do DFTs for
		for (int i = 0; i < intitialNumberofBins; ++i) {
			//	edge				edge
			//	V					V
			//  |	|	|	|	|						we don't want out searches here
			//	|	 |	  |	   |	|					we want our searches here
			//	^					^
			intialBins[i] = searchBetweenBins[0] + (i * (range) / (intitialNumberofBins - 1));
		}


		// We now do a DFT for each bin inside of the intialBins array
		for (int i = 0; i < intitialNumberofBins; ++i) {

			// this is a constant used for DFTs that will change every time a new bin is considered
			freqBinxtwoPiOverWindowLength = twoPiOverWindowLength * intialBins[i];

			// reset the fourierOutput
			fourierOutput[0] = 0;
			fourierOutput[1] = 0;

			// we now do a DFT
			// double[0] is real, double[1] is imaginary
			for (int j = 0; j < windowLength; ++j) {
				if (window[j] != 0) {
					fourierOutput[0] += window[j] * Math.cos(freqBinxtwoPiOverWindowLength * j);
					fourierOutput[1] += window[j] * Math.sin(freqBinxtwoPiOverWindowLength * j);
				}
			}

			// we store the result of the DFT in the initialBinsResult array
			// we don't much care for the phase of the output and we don't care about square rooting the
			// result either, as this takes time and it isn't very useful at this point. The max
			// value will still be the max value, even if they're all not square rooted results

			initialBinsResult[i] = Math.pow(fourierOutput[0], 2) + Math.pow(fourierOutput[1], 2);
		}

		// we rest the index of the maxValue
		indexOfMaxValue = 0;

		// once our result array has been filled, we go through it and find the maximum value
		for (int i = 0; i < intitialNumberofBins; ++i) {

			// if the bin result is larger than the already largest bin result, we use it's index to be
			// the newest index of the largest value in the initialBinsResult array
			if(initialBinsResult[i] >= initialBinsResult[indexOfMaxValue]) {
				indexOfMaxValue = i;
			}

		}

		// we then find the larger values of the two indexes either side of the large peak's index
		// and then use these two bins as our new search area
		int secondMaxValueIndex;
		if (indexOfMaxValue == 0){
			secondMaxValueIndex = indexOfMaxValue + 1;
		} else if(indexOfMaxValue == intitialNumberofBins - 1) {
			secondMaxValueIndex = indexOfMaxValue - 1;
		} else if(initialBinsResult[indexOfMaxValue - 1] > initialBinsResult[indexOfMaxValue + 1]) {
			secondMaxValueIndex = indexOfMaxValue - 1;
		} else {
			secondMaxValueIndex = indexOfMaxValue + 1;
		}

		if (secondMaxValueIndex < indexOfMaxValue) {
			searchBetweenBins[0] = intialBins[secondMaxValueIndex];
			searchBetweenBins[1] = intialBins[indexOfMaxValue];

		} else {
			searchBetweenBins[0] = intialBins[indexOfMaxValue];
			searchBetweenBins[1] = intialBins[secondMaxValueIndex];
		}

	}

}
