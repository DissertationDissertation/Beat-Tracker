package fourier;

public class FiguringOutTheOrder {

	public static void main(String[] args) {

		String[] alphabet = {"a", "b", "c", "d", "e", "f", "g", "h"}; // "i", "j", "k", "l", "m", "n", "o", "p"};
		// alphabet represents an input signal that has a square number of samples
		// an alphabet is used here instead of actual numbers to make understanding the code slightly easier

		String[] twiddleFactors = { "W0", "W1", "W2", "W3", "W4", "W5", "W6", "W7" };
		// the twiddle factor vector is always half the size of the 'alphabet'(input) vector
		// it is best and more efficient to compute this vector only once at the beginning
		// (as long has the input maintains a constant number of samples)

		System.out.println("Input is:");
		for (String i : alphabet) {
			System.out.print(i + "   ");
		}
		

		int step = 1;
		// this indicates the size of step taken in the vector for each increment
		// for instance this is a step size = 4
		//	|           |           |           |
		//	a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p
		//
		// this is also a step size = 4 with an offset = 1
		//	   |           |           |           |
		//	a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p
		//
		// this is a step size = 1
		//	|  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
		//	a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p

		int loop = alphabet.length / step;
		// this variable indicates is how many times the step can be iterated through the vector before it's out of bounds
		// for instance, when step size = 1
		//	|  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
		//	a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p
		// the step can be iterated a maximum 16 times before we go out of bounds
		//
		// when step size = 4
		//     |           |           |           |           | << out of bounds step     
		//	a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p
		// we can do a maximum of 4 iterations of the step before we go out of bounds here
		//
		// when step size = 8
		//                    |                       |                       | << out of bounds step     
		//	a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p
		// we can do a maximum of 2 iterations of the step before we go out of bounds here
		//
		// the offset of the steps doesn't affect this value (so long as offset < step size (which it always is))
		// a fuller name for this variable would be 'maximum number of step iterations possible'

		int log2ofN = (int) (Math.log(alphabet.length) / Math.log(2));
		// this variable tells how many 'stages' of butterfly calculation there are

		int r1;
		int r2;
		int tw;
		// because we're going to reference two positions in the 'alphabet' vector for each "calculation",
		// we will create variables for them, which will be continually updated
		// we will also create a variable for referencing values in the 'twiddleFactors' vector

		for (int stage = 0; stage < log2ofN; ++stage) {

			System.out.println("\n\nStage " + (stage + 1) + ":");

			// iterate through the vector 'alphabet' at different offsets. The offset cannot be larger than a step size
			// if the step  = 1, this loop will only run through once, as no more offsets are needed
			for (int offset = 0; offset < step; offset++) {

				// iteratively step through vector 'alphabet' until  the boundary of the array is met
				// for example, if step = 4 and offset = 2 then the following 'for' loop will iteratively evaluate the 4 values
				// c, g, k, o in the vector 'alphabet'
				//        |           |           |           |
				//  a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p
				//
				// THE FOLLOWING FOR LOOP IS A BIT TRICKIER THAN THAT
				// We actually want to reference values in the 'alphabet' vector two at a time
				// Taking the example given above, we actually want to reference c, g together and k, o together,
				// in order to calculate something new based off of their values
				// We don't actually want to iterate through them respectively
				// This means we effectively want to double the step size between each loop iteration. Then, within each loop,
				// add on a step size to find the second reference index
				// Because we double the step size, we want to half the amount of iterations there are, hence 'loop / 2'
				// Looking a different example
				// step = 2 offset = 1
				//     |     |     |     |     |     |     |     |
				//     |           |           |           |
				//  a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p
				//           ^           ^           ^           ^
				// The top row of lines indicates the step value inputed
				// The second row of lines shows actually how much we skip ahead per iteration of the 'for' loop
				// The small arrows below show which indexes are referenced in the for loop by simply by adding a step to the previous index
				for (int stepIteration = 0; stepIteration < loop / 2; ++stepIteration) {

					// the values used in "calculation" are found in the 'alphabet' vector at these indexes
					r1 = offset + stepIteration * step * 2;
					r2 = offset + step + stepIteration * step * 2;

					// the correct twiddle factor for these "calculations" is found at the following index of the 'twiddleFactor' vector
					tw = offset * loop / 2;

					// the "calculations" that are being eluded to in the comments above are as follows
					//
					// alphabet[r2] = alphabet[r1] - alphabet[r2] * twiddleFactors[tw]
					// alphabet[r1] = alphabet[r1] + alphabet[r2] * twiddleFactors[tw]
					//
					// in the second calculation, the old value of alphabet[r2] is used, not the updated one
					// a good way of implementing these calculations is to work out alphabet[r2] * twiddleFactors[tw]
					// and then save the value in an intermediate variable
					// this intermediate variable can then be added and subtracted to alphabet[r1] to find the two new values
					//
					// the order of calculations (finding alphabet[r2] first) is important
					// doing the equations the other way round is wrong
					// alphabet[r1] = alphabet[r1] + alphabet[r2] * twiddleFactors[tw]        WRONG
					// alphabet[r2] = alphabet[r1] - alphabet[r2] * twiddleFactors[tw]         WAY
					// this would use the updated version of alphabet[r1] to calculate out alphabet[r2]
					// which would give an incorrect result

					System.out.print(alphabet[r1]  + "=" + alphabet[r1] + "+" + alphabet[r2] + twiddleFactors[tw] + "  ");
					System.out.print(alphabet[r2]  + "=" + alphabet[r1] + "-" + alphabet[r2] + twiddleFactors[tw] + "  ");

				}
			}
			step = step * 2; // double the step value
			loop = alphabet.length / step; // doubling the step value must half the loop value
			// look in the code for where loop value is instantiated for it's full definition
		}

	}

}
