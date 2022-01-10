package rollingmemory;

public class rollingArrayTester {

	public static void main(String[] args) {

		// a 6 x 4 array of the alphabet
		double[][] numbers = {	{0, 0, 0, 0, 0},
							{1, 1, 1, 1, 1},
							{2, 2, 2, 2, 2}, 
							{3, 3, 3, 3, 3},
							{4, 4, 4, 4, 4},
							{5, 5, 5, 5, 5},
							{6, 6, 6, 6, 6},
							{7, 7, 7, 7, 7},
							{8, 8, 8, 8, 8},
							{9, 9, 9, 9, 9},
							{10, 10, 10, 10, 10},
							{11, 11, 11, 11, 11},
							{12, 12, 12, 12, 12},
							{13, 13, 13, 13, 13},
							{14, 14, 14, 14, 14} };


		RollingArrayMemory rollingArray = new RollingArrayMemory(5, 10);
		
		for (int i = 0; i < numbers.length; ++i) {
			rollingArray.addArray(numbers[i]);
		}

		double[]forprinting = rollingArray.getArrayCopy(10);

		for (int i = 0; i < forprinting.length; ++i) {

			System.out.print(forprinting[i] + " ");

		}
		
		System.out.println("\n");
		
		
		double[] number = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
		
		RollingDoubleMemory rollingDouble = new RollingDoubleMemory(number.length);
		
		rollingDouble.addDoubleArray(number);
		
		System.out.println(rollingDouble.getDouble(5));
		
		rollingDouble.addDouble(20.0);
		rollingDouble.addDouble(21.0);
		rollingDouble.addDouble(22.0);
		rollingDouble.addDouble(23.0);
		rollingDouble.addDouble(24.0);
		
		rollingDouble.addDoubleArray(new double[] {25, 26, 27, 28, 29});
		
		rollingDouble.overwriteDouble(19, 300.0);
		
		
		double[] printMe1 = rollingDouble.getDoubleArray(10, 10);
		
		for (double i : printMe1) {
			System.out.print(i + ", ");
		}
				
		
	}

}
