// Copyright 2007 by Basil Vandegriend.  All rights reserved.
package com.basilv.envgen;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.FileUtils;

import com.basilv.core.FileUtilities;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModelException;

/**
 * Ant task for EnvGen - Environment Specific File Generator.
 */
public class EnvGenTask extends Task {

	/**
	 * Represents a shared variable for the FreeMarker configuration.
	 */
	public static class SharedVariable {
		private String name;
		private String value;
		
		public String getValue() {
			return value;
		}
		
		public void setValue(String value) {
			this.value = value;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
	}
	
	/**
	 * Represents a shared transform for the FreeMarker configuration.
	 */
	public static class TransformSpecification {
		private String name;
		private String transformClassName;
		
		// If I try accepting a Class instance rather than a class name, then 
		// testing from ant fails due to ant being unable to instatiante the transform class due 
		// to weird classpath problems. Passing in the class as a string works.
		public void setClass(String className) {
			this.transformClassName = className;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		
		public Object getTransformInstance() {
			try {
				Class transformClass = getClass().getClassLoader().loadClass(transformClassName);
				return 	transformClass.newInstance();
			} catch (Exception e) {
				throw new RuntimeException("Error creating instance of class [" + transformClassName + "]. " +
						"The class is likely missing from the classpath.", e);
			}
		}
	}
	
	// Required
	private List sourceFileSets = new ArrayList();
	
	// Required
	private File destDir;
	
	// Required
	private File envPropertiesFile;
	
	private List sharedVariables = new ArrayList();
	private List transforms = new ArrayList();
	
	private boolean overwrite = false;

	private boolean stripFileExtension = false;
	
	private boolean diffToUpdate = false;
	
	// TODO: Properties to support later, with initial defaults
//	private boolean flatten = false;
//	private boolean includeEmptyDirs = false;

	
	private int numFilesGenerated;

	// Default no-arg constructor needed by ANT.
	public EnvGenTask() {
		
		// Initialize log4j, which freemarker uses, to write to standard output
		// Initialize log4j here rather than in execute() so that unit tests 
		// also have it initialized if they are calling other methods on this class.
		
		BasicConfigurator.configure();
		
		// Hide debugging output.
		Logger.getRootLogger().setLevel(Level.INFO);
		
	}
	
	public void addConfiguredSource(FileSet fileSet) {
		sourceFileSets.add(fileSet);
	}

	public void addConfiguredSharedVariable(SharedVariable var) {
		sharedVariables.add(var);
	}

	public void addConfiguredTransform(TransformSpecification transformSpec) {
		transforms.add(transformSpec);
	}

	public void setDestDir(File file) {
		destDir = file;
	}
	
	public void setEnvPropertiesFile(File file) {
		envPropertiesFile = file;
	}
	
	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	public void setDiffToUpdate(boolean diffToUpdate) {
		this.diffToUpdate = diffToUpdate;
	}
	
	public void setStripFileExtension(boolean stripFileExtension) {
		this.stripFileExtension = stripFileExtension;
	}

	public void execute() throws BuildException {

		validateSettings();

		numFilesGenerated = 0;
		
		// Log settings
		log("Environment properties file: " + FileUtilities.getCanonicalPath(envPropertiesFile));

		for (Iterator i = sourceFileSets.iterator(); i.hasNext();) {
			FileSet fileSet = (FileSet) i.next();
			File baseDir = fileSet.getDir(getProject());
			log("Source directory: " + FileUtilities.getCanonicalPath(baseDir));
		}				
		log("Destination directory: " + FileUtilities.getCanonicalPath(destDir));
		
		EnvironmentProperties envProps = EnvironmentPropertiesLoader.load(
			envPropertiesFile.getPath());

		Configuration config = createFreemarkerConfiguration();

		// For each environment
		for (Iterator envIterator = envProps.getEnvPropertiesList().iterator(); envIterator.hasNext();) {
			Map propertyMap = (Map) envIterator.next();
			
			TemplateMapModel mapModel = new TemplateMapModel(propertyMap);
			
			String targetDir = generateString(mapModel, FileUtilities.getCanonicalPath(destDir));

			for (Iterator sourceFileSetIterator = sourceFileSets.iterator(); sourceFileSetIterator.hasNext();) {
				FileSet fileSet = (FileSet) sourceFileSetIterator.next();
				
				File baseDir = fileSet.getDir(getProject());
				try {
					config.setDirectoryForTemplateLoading(baseDir);
				} catch (IOException e) {
					throw new RuntimeException("Error generating files from directory [" + 
						FileUtilities.getCanonicalPath(destDir) + "] due to " + e.getMessage() + ".", e);
				}

				DirectoryScanner scanner = fileSet.getDirectoryScanner(getProject());
				String[] includedFilenames = scanner.getIncludedFiles();
				
				// For each source file
				for (int fileIndex = 0; fileIndex < includedFilenames.length; fileIndex++) {
					String sourceFilename = includedFilenames[fileIndex];
					
					String targetFilename = generateString(mapModel, sourceFilename);
					
					if (stripFileExtension) {
						targetFilename = stripFileExtension(targetFilename);
					}
					
					
					File sourceFile = new File(baseDir, sourceFilename);
					File targetFile = new File(targetDir, targetFilename);
					
					// If overwrite is true, then we don't need to bother with the slower diff to update logic.
					// If we are in diffToUpdate mode and the target file does not exist, we will need to regenerate it
					if (overwrite || !diffToUpdate || !targetFile.exists()) {
						if (! shouldGenerateFile(sourceFile, targetFile)) {
							log("File [" + targetFile.getAbsolutePath() + "] is not generated because it is up to date.", 
								Project.MSG_VERBOSE);
							continue;
						}
						
						
						// Ensure parent directory exists - it may already exist, so do not check return value.
						targetFile.getParentFile().mkdirs();
					
				        if (generateFile(config, mapModel, baseDir, sourceFilename, targetFile)) {
					        recordGeneration(sourceFile, targetFile);
				        }
						
					} else {
						try {
							File tempTargetFile = File.createTempFile("EnvGen", ".tmp");
					        if (generateFile(config, mapModel, baseDir, sourceFilename, tempTargetFile)) {
							
					        	FileUtils fileUtils = FileUtils.getFileUtils();
					        	// This comparison ignores line-ending differences, in case the Fixcrlf task
					        	// is run on EnvGen's output.
						        if (! fileUtils.contentEquals(targetFile, tempTargetFile, true)) { 
						        	recordGeneration(sourceFile, targetFile);
									
						        	fileUtils.copyFile(tempTargetFile, targetFile, null, true);
						        }
					        } else {
					        	FileUtilities.deleteFile(targetFile);
					        }
					        FileUtilities.deleteRecursively(tempTargetFile);

						} catch (IOException e) {
							throw new RuntimeException("Error creating temp file", e);
						}
				        
					}
				}
			}
		}
		
		log("Generated " + numFilesGenerated + " target files.");
		
	}

	/**
	 * @param sourceFile
	 * @param targetFile
	 */
	private void recordGeneration(File sourceFile, File targetFile) {
		numFilesGenerated++;
		
		log("Generating from file [" + sourceFile.getAbsolutePath() + "].", Project.MSG_VERBOSE);
		log("	to file [" + targetFile.getAbsolutePath() + "].", Project.MSG_VERBOSE);
	}


	// Non-private for testing. Assumes that config.setDirectoryForTemplateLoading(sourceBaseDir) was called.
	// Returns true if file was generated, false if generation was skipped.
	boolean generateFile(Configuration config, TemplateMapModel mapModel, 
		File sourceBaseDir, String sourceFilename, File targetFile) {

		File sourceFile = new File(sourceBaseDir, sourceFilename);
		try {

			Template template = config.getTemplate(sourceFilename);			
			Writer outputFileWriter = new BufferedWriter(new FileWriter(targetFile));
			try {
				template.process(mapModel, outputFileWriter);
				outputFileWriter.flush();
			} finally {
				outputFileWriter.close();
			}
			
			if (SkipGenerationTransform.isSkipGenerationAndResetFlag()) {
	        	FileUtilities.deleteFile(targetFile);
				return false; 
			}

			return true;
			
		} catch (IOException e) {
			// Add underlying exception message to wrapper exception message because Ant  
			// only reports the message of the outer exception on the console.
			throw new RuntimeException("Error producing target file [" + 
				FileUtilities.getCanonicalPath(targetFile) + "] due to " + 
				e.getMessage() + ".", e);
			
		} catch (TemplateException e) {
			throw new RuntimeException("Error doing generation from source file [" + 
				FileUtilities.getCanonicalPath(sourceFile) + "] due to " + 
				e.getMessage() + ".", e);
		}
		
	}

	// Non-private for testing.
	static String stripFileExtension(String targetFilename) {
		if (targetFilename.indexOf(".") == -1) {
			return targetFilename;
		}
		
		int indexOfLastPeriod = targetFilename.lastIndexOf(".");
		String strippedName = targetFilename.substring(0, indexOfLastPeriod);
		return strippedName;
	}

	// Non-private for testing
	void validateSettings() {
		if (envPropertiesFile == null) {
			throw new BuildException("The envPropertiesFile attribute must be specified.");
		}
		
		if (!envPropertiesFile.exists()) {
			throw new BuildException("The environment properties file [" + 
				FileUtilities.getCanonicalPath(envPropertiesFile) + "] does not exist.");
		}

		if (!envPropertiesFile.isFile()) {
			throw new BuildException("The environment properties file specified [" + 
				FileUtilities.getCanonicalPath(envPropertiesFile) + "] is not a file.");
		}
		
		if (destDir == null) {
			throw new BuildException("The destDir attribute must be specified.");
		}
		// It is okay for the destination directory to not exist, as we will create it.

		if (destDir.exists() && !destDir.isDirectory()) {
			throw new BuildException("The destination directory specified [" + 
				FileUtilities.getCanonicalPath(destDir) + "] is not a directory.");
		}

		if (sourceFileSets.isEmpty()) {
			throw new BuildException("At least one nested <source> element must be specified.");
		}
		
	}

	// Non-private for testing
	int getNumFilesGenerated() {
		return numFilesGenerated;
	}

	// Non-private for testing
	boolean shouldGenerateFile(File sourceFile, File targetFile) {
		
		if (overwrite) {
			return true;
		}
		
		FileUtils fileUtils = FileUtils.getFileUtils();
		
		if (! fileUtils.isUpToDate(sourceFile, targetFile, fileUtils.getFileTimestampGranularity()) ) {
			return true;
		}
		
		if (! fileUtils.isUpToDate(envPropertiesFile, targetFile, fileUtils.getFileTimestampGranularity()) ) {
			return true;
		}
		
		return false;
	}

	// Non-private for testing
	Configuration createFreemarkerConfiguration() {
		Configuration config = new Configuration();
		config.setStrictSyntaxMode(true);
		config.setWhitespaceStripping(true);
		
		for (Iterator i = sharedVariables.iterator(); i.hasNext(); ) {
			SharedVariable sharedVariable = (SharedVariable) i.next();
			try {
				// TODO: Convert name to account for periods using TemplateMapModel
				config.setSharedVariable(sharedVariable.getName(), sharedVariable.getValue());
			} catch (TemplateModelException e) {
				throw new RuntimeException("Error configuring shared variable [" + 
					sharedVariable.getName() + "] due to " + e.getMessage() + ".", e);
			}
		}

		for (Iterator i = transforms.iterator(); i.hasNext();) {
			TransformSpecification transform = (TransformSpecification) i.next();
			try {
				config.setSharedVariable(transform.getName(), transform.getTransformInstance());
			} catch (TemplateModelException e) {
				throw new RuntimeException("Error configuring transform [" + 
					transform.getName() + "] due to " + e.getMessage() + ".", e);
			}
		}
		
		return config;
	}
	

	// Non-private for testing.
    String generateString(TemplateMapModel mapModel, String sourceString) {
        try {
            Template template = new Template("name", 
            	new StringReader(sourceString), createFreemarkerConfiguration());
            	
            StringWriter targetStringWriter = new StringWriter(); 
            template.process(mapModel, targetStringWriter);
            targetStringWriter.flush();
            
            String targetString = targetStringWriter.getBuffer().toString();
            return targetString;
        } catch (Exception e) {
        	throw new RuntimeException("Error generating from source string [" + 
        		sourceString + "] due to " + e.getMessage() + ".", e);
        }
    }
	    
}
