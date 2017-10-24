package org.ciat.gavilan.control;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.ciat.gavilan.model.SummaryRun;
import org.ciat.gavilan.model.Variable;
import org.ciat.gavilan.view.ProgressBar;

public class OverviewWorker {

	private Map<String, String> outputValues;
	private List<String> cropNSoilLables;
	private SummaryRun run;

	public enum fileSection {
		INIT(), CROP_N_SOIL, GROWTH, END
	};

	public OverviewWorker(SummaryRun summaryRun) {
		this.run = summaryRun;
		this.cropNSoilLables = new ArrayList<String>();
		this.outputValues = new LinkedHashMap<String, String>();

	}

	public void work() {

		ProgressBar bar = new ProgressBar();
		int subFolderIndex = 0;

		populateVariables();

		File CSV = run.getOverviewCSVOutput();
		// File JSON = run.getOverviewJSONOutput();

		try (BufferedWriter CSVwriter = new BufferedWriter(
				new PrintWriter(CSV)); /* BufferedWriter JSONwriter = new BufferedWriter(new PrintWriter(JSON)) */) {

			/* Building the header */
			String head = SummaryRun.CANDIDATE_LABEL + SummaryRun.COLUMN_SEPARATOR + SummaryRun.TREATMENT_LABEL + SummaryRun.COLUMN_SEPARATOR + SummaryRun.TREATMENT_LABEL + SummaryRun.COLUMN_SEPARATOR;

			for (String var : cropNSoilLables) {
				outputValues.put(var, "");
				var = var.replaceAll(",", "");
				var = var.replaceAll(SummaryRun.COLUMN_SEPARATOR, "");
				head += var + SummaryRun.COLUMN_SEPARATOR;
			}

			CSVwriter.write(head);
			CSVwriter.newLine();
			/* END building the header **/

			/* Search on each OVERVIEW.OUT file from 0/ folder and further */
			boolean flagFolder = true;
			for (int folder = 0; flagFolder; folder++) {
				File bigFolder = new File(folder + SummaryRun.PATH_SEPARATOR);
				if (bigFolder.exists()) {
					bar = new ProgressBar();
					System.out.println("Getting overwiew on folder " + bigFolder.getName());
					int subFoderTotal = bigFolder.listFiles().length;

					for (File subFolder : bigFolder.listFiles()) { // for each subfolder
						// look at the overview.out file
						File output = new File(subFolder.getAbsolutePath() + SummaryRun.PATH_SEPARATOR + "OVERVIEW.OUT");
						if (output.exists()) {
							// for each candidate get all the simulated and observed values
							for (String cadena : getCandidateVariables(output)) {
								CSVwriter.write(cadena); // print the values
								CSVwriter.newLine();
							}

						} else {
							App.log.warning(subFolder.getName() + SummaryRun.PATH_SEPARATOR + output.getName() + " not found");
						}
						subFolderIndex++;
						if (subFolderIndex % 100 == 0) {
							bar.update(subFolderIndex, subFoderTotal);
						}
					}
					bar.update(subFoderTotal - 1, subFoderTotal);
					// bwriter.flush();

				} else {
					flagFolder = false; // Flag that there are no more folders search in
					App.log.fine("Finished gathering overwiew results");
				}
			}

			App.log.fine("overview.csv created");

		} catch (IOException e1) {
			e1.printStackTrace();
		}

	}

	private void populateVariables() {
		switch (run.getModel()) {
			case BEAN: {
				cropNSoilLables.add("Emergence");
				cropNSoilLables.add("End Juven");
				cropNSoilLables.add("Flower Ind");
				cropNSoilLables.add("First Flwr");
				cropNSoilLables.add("First Pod");
				cropNSoilLables.add("First Seed");
				cropNSoilLables.add("End Pod");
				cropNSoilLables.add("Phys. Mat");
				cropNSoilLables.add("End Leaf");
				cropNSoilLables.add("Harv. Mat");
				cropNSoilLables.add("Harvest");
	
			}
	
				break;
			case MAIZE: {
				cropNSoilLables.add("End Juveni");
				cropNSoilLables.add("Floral Ini");
				cropNSoilLables.add("Silkin");
				cropNSoilLables.add("Beg Gr Fil");
				cropNSoilLables.add("End Gr Fil");
				cropNSoilLables.add("Maturity");
				cropNSoilLables.add("Harvest");
	
			}
				break;
			default: {
				App.log.warning("Crop not configurated for overview: " + run.getModel() + ", using default variables");
				cropNSoilLables.add("End Juven");
				cropNSoilLables.add("Floral I");
				cropNSoilLables.add("Harvest");
	
			}
		}

	}

	/*
	 * obtain all the simulated and observed values of the variables populated in both cropNSoilVariables and
	 * growthVariables
	 */
	private List<String> getCandidateVariables(File cultivarOutput) {

		List<String> runsOutput = new ArrayList<String>();
		String cadena = "";
		String line = "";
		fileSection flag = fileSection.INIT;
		int treatment = 0;
		int run = 0;

		try (Scanner reader = new Scanner(cultivarOutput)) {

			while (reader.hasNextLine()) { // reading the whole file
				line = reader.nextLine();

				switch (flag) {
				case INIT: {
					if (line.contains("*RUN")) { // to detect each single run
						run = Integer.parseInt(line.substring(6, 10).replaceAll(" ", ""));
						// to print candidate ID and the run
						cadena = (new File(cultivarOutput.getParent())).getName() + SummaryRun.COLUMN_SEPARATOR + run + SummaryRun.COLUMN_SEPARATOR;
						for (String key : outputValues.keySet()) {
							outputValues.put(key, ""); // clear the previous values to recycle the Map
						}
					}
					if (line.contains("TREATMENT")) { // to detect each single treatment of a run
						treatment = Integer.parseInt(line.substring(11, 15).replaceAll(" ", ""));
						// to print experiment treatment
						cadena += treatment + SummaryRun.COLUMN_SEPARATOR;
					}
					if (line.contains("*SIMULATED CROP AND SOIL STATUS AT MAIN DEVELOPMENT STAGES")) { // detect section
						flag = fileSection.CROP_N_SOIL;
					}
				}
					break;
				case CROP_N_SOIL: {

					for (String var : cropNSoilLables) {
						if (line.contains(var)) { // if contains the string that corresponds to the variable
							outputValues.put(var, line.substring(54, 60)); // get value from file
						}
					}
					// to detect the end of the section
					if (line.contains("*MAIN GROWTH AND DEVELOPMENT VARIABLES")) {
						flag = fileSection.END;
						for (String key : outputValues.keySet()) {
							cadena += outputValues.get(key) + SummaryRun.COLUMN_SEPARATOR;
						}
						runsOutput.add(cadena);
					}
				}
					break;
				case END: {
					if (line.contains("*DSSAT Cropping System Model")) { // detect the start of a new treatment run
						flag = fileSection.INIT;
					}

				}
					break;
				default:
					break;
				}
			}

			// reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return runsOutput;
	}

}
