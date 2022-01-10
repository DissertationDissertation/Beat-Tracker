package audio;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import fourier.DFTSearch;

public class HarmonicTempoPlotter extends DFTSearch{

	double expectedWindowsBetweenBeats = 6;

	// create a data series to store first Harmonic tempo
	XYSeries firstHarmSeries = new XYSeries("First Harmonic", false);

	// create a data series to store first Harmonic tempo
	XYSeries secondHarmSeries = new XYSeries("Second Harmonic", false);

	// create a data series to store fourth Harmonic tempo
	XYSeries thirdHarmSeries = new XYSeries("Third Harmonic", false);

	// create a data series to store eighth Harmonic tempo
	XYSeries fourthHarmSeries = new XYSeries("Fourth Harmonic", false);

	// create a data series to store eighth Harmonic tempo
	XYSeries fifthHarmSeries = new XYSeries("Fifth Harmonic", false);
	
	// create a data series to store eighth Harmonic tempo
	XYSeries sixthHarmSeries = new XYSeries("Sixth Harmonic", false);
	
	// create a data series to store eighth Harmonic tempo
	XYSeries seventhHarmSeries = new XYSeries("Seventh Harmonic", false);
	
	// create a data series to store eighth Harmonic tempo
	XYSeries eigthHarmSeries = new XYSeries("Eigth Harmonic", false);

	public void getTempoHarmonies(double[] intervalImpulses, double[] intervalVolumes, double time, double windowOffsetsPerBeat) {

		// decide how many cycles there will be in the interval
		double numOfCyclesInInterval = intervalImpulses.length / windowOffsetsPerBeat;

		// we want to create bins to search between
		double lowerBin = numOfCyclesInInterval * 3.0 / 4;
		double upperBin = numOfCyclesInInterval * 3.0 / 2;

		double jump = search(intervalImpulses, 31, 5, lowerBin, upperBin);

		// convert the answer the DFT search into windows per cycle rather than cycles per interval
		jump = (intervalImpulses.length) / jump;
		if (60000 / (jump * 3) > 40) 
			firstHarmSeries.add(time / 1000, 60000 / (jump * 3));

		// do the same thing for the other harmonics

		// SECOND
		jump = search(intervalImpulses, 31, 5, lowerBin * 2, upperBin + lowerBin);
		jump = (intervalImpulses.length) / jump;
		if (60000 / (jump * 3) > 40) 
			secondHarmSeries.add(time / 1000, 60000 / (jump * 3));

		// THIRD
		jump = search(intervalImpulses, 31, 5, lowerBin * 3, upperBin + lowerBin * 2);
		jump = (intervalImpulses.length) / jump;
		if (60000 / (jump * 3) > 40) 
			thirdHarmSeries.add(time / 1000, 60000 / (jump * 3));

		// FOURTH
		jump = search(intervalImpulses, 31, 5, lowerBin * 4, upperBin + lowerBin * 3);
		jump = (intervalImpulses.length) / jump;
		if (60000 / (jump * 3) > 40) 
			fourthHarmSeries.add(time / 1000, 60000 / (jump * 3));
		
		// FIFTH
		jump = search(intervalImpulses, 31, 5, lowerBin * 5, upperBin + lowerBin * 4);
		jump = (intervalImpulses.length) / jump;
		if (60000 / (jump * 3) > 40) 
			fifthHarmSeries.add(time / 1000, 60000 / (jump * 3));
		
		// SIXTH
		jump = search(intervalImpulses, 31, 5, lowerBin * 6, upperBin + lowerBin * 5);
		jump = (intervalImpulses.length) / jump;
		if (60000 / (jump * 3) > 40) 
			sixthHarmSeries.add(time / 1000, 60000 / (jump * 3));
		
		// SEVENTH
		jump = search(intervalImpulses, 31, 5, lowerBin * 7, upperBin + lowerBin * 6);
		jump = (intervalImpulses.length) / jump;
		if (60000 / (jump * 3) > 40) 
			seventhHarmSeries.add(time / 1000, 60000 / (jump * 3));
		
		// EIGHT
		jump = search(intervalImpulses, 31, 5, lowerBin * 8, upperBin + lowerBin * 7);
		jump = (intervalImpulses.length) / jump;
		if (60000 / (jump * 3) > 40) 
			eigthHarmSeries.add(time / 1000, 60000 / (jump * 3));

	}


	public void plotTempoHamonies() {

		XYSeriesCollection outputDataset = new XYSeriesCollection();
		outputDataset.addSeries(firstHarmSeries);
		outputDataset.addSeries(secondHarmSeries);
		outputDataset.addSeries(thirdHarmSeries);
		outputDataset.addSeries(fourthHarmSeries);
		outputDataset.addSeries(fifthHarmSeries);
		outputDataset.addSeries(sixthHarmSeries);
		outputDataset.addSeries(seventhHarmSeries);
		outputDataset.addSeries(eigthHarmSeries);
		JFreeChart chartTempos = ChartFactory.createXYLineChart("Tempo Harmonics Without Window", "time (seconds)", "BPM", outputDataset);
		ChartFrame frame2 = new ChartFrame("This Doesn't Matter", chartTempos);
		frame2.setVisible(true);
		frame2.setSize(1500, 350);
	}


}
