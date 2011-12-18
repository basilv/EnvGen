// Copyright 2007 by Basil Vandegriend.  All rights reserved.

package com.basilv.envgen;

import com.basilv.core.Assert;
import com.basilv.core.StringUtilities;
import com.csvreader.CsvReader;

/**
 * Loads environment properties from the specified CSV file and populates an instance of 
 * EnvironmentProperties. 
 * 
 * The file format is as follows: 
 * <ul>
 * <li>Each row corresponds to a single property or to a comment.</li>
 * <li>For a property, the first column contains the name of the property.<br> 
 * Each subsequent column contains the value of the property for a single environment.</li>
 * <li>For a comment, the first column must start with the symbol '#' or '//', or be completely empty. 
 * The remainder of the row is ignored. </li>
 * </ul>
 */
public class EnvironmentPropertiesLoader
{

	/**
	 * Load the CSV file into an EnvironmentProperties instance.
	 * @param filename  Cannot be null or empty. Must correspond to a CSV file of the correct format.
	 * @return the EnvironmentProperties instance.
	 */
	public static EnvironmentProperties load(String filename) {
		Assert.notNullOrEmpty("filename", filename);
		
		try {
			CsvReader reader = new CsvReader(filename);
			try {
				EnvironmentProperties envProps = null;

				final int FIRST_ROW = 1; 
				int rowNumber = FIRST_ROW; // Indexed from one.
				while (reader.readRecord()) {
					String propertyName = reader.get(0);
					if (isComment(propertyName)) {
						continue;
					}
					
					int columnCount = reader.getColumnCount();
					Assert.isTrue("Must have at least two columns - one for the property name " +
						"and one row per environment for the property value.", columnCount > 1);
					if (rowNumber == FIRST_ROW) {
						int envCount = columnCount - 1;
						envProps = new EnvironmentProperties(envCount);
					}
					
					for (int column = 1; column < columnCount; column++) {
						int environment = column - 1;
						String propertyValue = reader.get(column);
						envProps.addProperty(environment, propertyName, propertyValue);
					}
					
					rowNumber++;
				}
				if (envProps == null) {
					Assert.shouldNotReach("File [" + filename + "] does not contain any rows.");
				}
				
				return envProps;

			} finally {
				reader.close();
			}
		} catch (Exception e) {
			throw new RuntimeException("Error reading cvs file [" + 
				filename + "] due to " + e.getMessage() + ".", e);
		}
		
	}

	// Non-private for testing
	static boolean isComment(String propertyName) {
		if (StringUtilities.isNullOrEmpty(propertyName)) {
			return true;
		}
		
		if (propertyName.startsWith("#")) {
			return true;
		}

		if (propertyName.startsWith("//")) {
			return true;
		}

		return false;
	}
	
}
