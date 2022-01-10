package rollingmemory;

/**
 * This rolling integer works in much the same way as the rolling array memory
 * The rolling memory will start to loop and overwrite data as it is filled up
 * The main difference is that arrays of integers (smaller than the memory) can
 * be placed into the memory and taken out of the memory, unlike in the rolling
 * array memory, where 2D arrays cannot be placed or taken out of the memory
 * 
 * Lets see how a rolling integer memory works:
 * ' ^ ' is the "ghost index" or max accessible index
 * ' v ' is the actual index being pointed to or last added index
 * size of memory = 6
 * 	0	0	0	0	0	0
 * maxIndexAccessible = -1
 * minIndexAccessible = -6
 * revolutions = 0
 * 
 * An outside thread adds in an integer:
 * 	v
 * 	3	0	0	0	0	0
 * 	^
 * maxIndexAccessible = 0
 * minIndexAccessible = -5
 * revolutions = 0
 * 
 * 	An outside thread adds in another integer:
 * 		v
 * 	3	8	0	0	0	0
 * 		^
 * maxIndexAccessible = 1
 * minIndexAccessible = -4
 * revolutions = 0
 * 
 * An outside thread adds in an array of 4 integers:
 * 						v
 * 	3	8	7	9	9	7
 * 						^
 * maxIndexAccessible = 5
 * minIndexAccessible = 0
 * revolutions = 0
 * 
 * An outside thread adds in one more integer:
 * 	v
 * 	7	8	7	9	9	7
 * 							^
 * maxIndexAccessible = 6
 * minIndexAccessible = 1
 * revolutions = 1
 * 
 * 	An outside thread adds an array of 3 integers:
 * 				v
 * 	7	1	1	1	9	7
 * 										^
 * maxIndexAccessible = 9
 * minIndexAccessible = 4
 * revolutions = 1
 * 
 * An outside thread adds an array of 5 integers:
 * 			v
 * 	8	8	8	1	8	8
 * 															^
 * maxIndexAccessible = 14
 * minIndexAccessible = 9
 * revolutions = 1
 * 
 */

public class RollingIntMemory {


	private int sizeOfMemory;

	// read the above to understand what these are
	// briefly, they give the span of accessible indexes available for reading
	private int maxIndexAccessible;
	private int minIndexAccessible;

	// the actual index tells us which index to put a new integer into
	// it resets back to 0 once it has reaches the size of the memory
	private int actualIndex = 0;

	// the retrieve index tells us which index to retrieve from
	// it is calculated every time a new request for an array is made
	private int retrieveIndex;

	private int revolutions = 0;

	private int[] rollingInteger;

	/**
	 * Creates a rolling memory for integers of the requested size
	 * @param sizeOfMemory the maximum number of integers to be temporarily stored at any one time
	 */
	public RollingIntMemory(int sizeOfMemory){

		this.sizeOfMemory = sizeOfMemory;

		// create a rolling memory with the size specified
		rollingInteger = new int[sizeOfMemory];

		// maxIndexAccessible is set to -1 to ensure no data is retried when the rolling memory is empty
		maxIndexAccessible = -1;
		minIndexAccessible = -sizeOfMemory;
		// because minIndexAccessible only becomes relevant after one revolution or wrap of the rolling
		// memory, we set it to become relevant (when it's > 0) once a revolution has been completed by
		// negating the size of one revolution, which is the number of integers stored in the
		// rolling memory

	}

	/**
	 * Method for adding a single new integer to the rolling memory. 
	 * Synchronised with other methods in this object to make the memory thread safe
	 * @param saveInteger the integer to be saved to this temporary memory
	 * @see addIntArray
	 */
	synchronized public void addInt(int saveInteger) {

		// copy the value of the integer into the array at the correct index
		rollingInteger[actualIndex] = saveInteger;

		// increase the maxIndexAccessible and the minIndexAccessible, as the span has
		// shifted over by one
		++maxIndexAccessible;
		++minIndexAccessible;

		// reset the actual index back to 0 once it reaches the end of the array,
		// ready for the next time a row is inserted
		// also increase the revolutions count as a wrap has happened on the array
		++actualIndex;
		if (actualIndex == sizeOfMemory) {

			actualIndex = 0;

			++revolutions;
		}


	}


	/**
	 * Method for adding an array of integers to the rolling memory. 
	 * The array is duplicated in the rolling memory
	 * Synchronised with other methods in this object to make the memory thread safe
	 * @param saveIntegers an integer array to be saved into memory
	 * @throws IllegalArgumentException if the inputed array is larger than the memory, whcih would lead 
	 * to the array overwriting itself as it's added in
	 * @see addInt
	 */
	synchronized public void addIntArray(int[] saveIntegers) {

		// check if the array is smaller than the rolling integer memory size
		// if it isn't throw an exception
		if (saveIntegers.length > sizeOfMemory) {
			throw new IllegalArgumentException("Array of size " + saveIntegers.length + " is too large for memory of length " + sizeOfMemory);
		}

		// if all is good, start adding the arrays, using the method addIntArray
		for (int i = 0; i < saveIntegers.length; ++i) {
			addInt(saveIntegers[i]);
		}

	}

	/**
	 * Method for retrieving a in integer, at the index requested, from the rolling memory
	 * Synchronised with other methods in this object to make the memory thread safe
	 * @param requestedIndex the index of the integer wanted from this temporary memory
	 * @return the integer in memory at the index requested
	 * @throws IllegalArgumentException if the requested index has been overwritten or not yet written
	 * @see getIntArray
	 */
	synchronized public int getInt(int requestedIndex) {

		// make sure the index is available
		checkVaildIndex(requestedIndex);

		retrieveIndex = requestedIndex - revolutions * sizeOfMemory;
		 if(retrieveIndex < 0) {
			 retrieveIndex += sizeOfMemory;
		 }

		return rollingInteger[retrieveIndex];

	}


	/**
	 * Method for retrieving an array of integers from the memory. It returns a copy of the array
	 * in the rolling memory. 
	 * Synchronised with other methods in this object to make the memory thread safe
	 * @param initialIndex the initial (first) index of the array wanted from this rolling memory
	 * @param arraySize the array size wanted out from the memory
	 * @return an array of integers at the requested size starting at the initial index requested
	 * @throws IllegalArgumentException if any of the indexes in the wanted array have been overwritten 
	 * or have not yet been written. It also throws this if the array requested is larger than the array
	 * memory
	 * @see getInt
	 * @see getIntPrivate
	 */
	synchronized public int[] getIntArray(int initialIndex, int arraySize) {

		// make sure the initial index is available
		checkVaildIndex(initialIndex);

		// then check to make sure the end index of the array wanted exists
		checkVaildIndex(initialIndex + arraySize - 1);

		// if both indexes are valid, create a new integer array an then return that array

		int[] returnArray = new int[arraySize];

		// set the retrieve index to the correct start point in the actual array for the
		// first value in the array to be returned
		retrieveIndex = initialIndex - revolutions * sizeOfMemory;
		if(retrieveIndex < 0) {
			retrieveIndex += sizeOfMemory;
		}

		for (int i = 0; i < arraySize; ++i) {
			// uses the private version of getInt where the indexes are not checked
			// since we already know they are valid for both boundaries
			returnArray[i] = rollingInteger[retrieveIndex];

			// add on 1 to the read actual index to read the next integer in the memory
			++retrieveIndex;

			// if the readActualIndex goes out of bounds of the memory array, set it back to
			// zero
			if (retrieveIndex == sizeOfMemory) {
				retrieveIndex = 0;
			}
		}

		return returnArray;

	}


	/**
	 * This method is only accessed by other methods in this object. 
	 * It will throw an error based on if the requested index has been overwritten or not
	 * 
	 * @param requestedIndex the index of the integer wanted from this temporary memory
	 * @throws IllegalArgumentException if the requested index has been overwritten or not yet written
	 * @see getIntArray
	 * @see getInt
	 */
	synchronized private void checkVaildIndex(int requestedIndex) {

		// check the index requested has not been overwritten in the rolling array
		// throw an error if it has been already overwritten
		if (requestedIndex < minIndexAccessible) {
			throw new IllegalArgumentException("Requested index " + requestedIndex + " has been overwritten (not in the range of " + minIndexAccessible + " - " + maxIndexAccessible + ")");
		}

		// check if the index requested has actually been added to the rolling array
		// if it's out of bounds by being over, throw an error message
		if (requestedIndex > maxIndexAccessible) {
			throw new IllegalArgumentException("Requested index " + requestedIndex + " is not yet written (not in the range of " + minIndexAccessible + " - " + maxIndexAccessible + ")");
		}

		// if all is good the code that called this method will continue without throwing an error

	}


	/**
	 * Returns the index of the last integer inputed into the rolling memory
	 * Synchronised with other methods in this object to make the memory thread safe
	 * @return the index of the last integer added to this rolling array
	 */
	synchronized public int getLastAddedIndex() {
		return maxIndexAccessible;
	}


	/**
	 * Returns the minimum index available in the rolling memory. 
	 * Any array indexes lower than the value returned here have been overwritten
	 * Synchronised with other methods in this object to make the memory thread safe
	 * @return the index of the last integer added to this rolling array
	 */
	synchronized public int getMinIndexAvailable() {
		return minIndexAccessible;
	}


	/**
	 * Returns the max amount of integers the rolling memory can store before it overwrites. 
	 * Synchronised with other methods in this object to make the memory thread safe
	 * @return the maximum number of integers that can be stored before overwriting occurs
	 */
	synchronized public int getMemorySize() {
		return sizeOfMemory;
	}


}




