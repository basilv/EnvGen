// Copyright 2007 by Basil Vandegriend.  All rights reserved.
package com.basilv.envgen;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.FileUtils;

import com.basilv.core.FileUtilities;

import freemarker.template.Configuration;

public class EnvGenTaskTest extends TestCase {

	private File projectRootDir;
	private EnvGenTask task;
	private File testTargetDir;
	
	public EnvGenTaskTest(String arg0) {
		super(arg0);
	}

	protected void setUp() throws Exception {

        projectRootDir = new File(".").getCanonicalFile();
        if (!projectRootDir.getName().equals("EnvGen1.4")) {
            projectRootDir = projectRootDir.getParentFile();
        }

		Project project = new Project();

		task = new EnvGenTask();
		task.setProject(project);
		
        testTargetDir = new File(projectRootDir, "build/testTarget");
        FileUtilities.deleteRecursively(testTargetDir);
	}

	protected void tearDown() throws Exception {
		// Don't delete results so they can be manually examined if necessary. 
		// FileUtilities.deleteRecursively(testTargetDir);
	}

	public void testStripFileExtension() {
		assertEquals("", EnvGenTask.stripFileExtension(""));
		assertEquals("test", EnvGenTask.stripFileExtension("test"));
		assertEquals("test-this", EnvGenTask.stripFileExtension("test-this.txt"));
		assertEquals("test.do", EnvGenTask.stripFileExtension("test.do.strip"));
	}
	
	public void testGenerateString() {
		
		Map properties = new HashMap();
		properties.put("env", "devl");
		properties.put("dir", "/mydir");
		properties.put("test.dir", "/testing");
		properties.put("test.dir.type", "new");
		TemplateMapModel mapModel = new TemplateMapModel(properties);
		
		verifyGenerateString(mapModel, "", "");
		verifyGenerateString(mapModel, "/test/${env}/stuff", "/test/devl/stuff");
		verifyGenerateString(mapModel, "${env}${dir}", "devl/mydir");
		verifyGenerateString(mapModel, "${test.dir}", "/testing");
		verifyGenerateString(mapModel, "${test.dir.type}", "new");
	}
	
	private void verifyGenerateString(TemplateMapModel mapModel, String input, String expectedOutput) {
		String actualOutput = task.generateString(mapModel, input);
		assertEquals(expectedOutput, actualOutput);
	}
	
	
	public void testGenerateFile() throws IOException {

		Map properties = new HashMap();
		properties.put("env", "devl");
		properties.put("dir", "/mydir");
		properties.put("test.dir", "/testing");
		TemplateMapModel mapModel = new TemplateMapModel(properties);

		File sourceFile = File.createTempFile(getClass().getName() + "-genSource", ".tmp");
		sourceFile.deleteOnExit();

		// Populate source File
		Writer writer = new BufferedWriter(new FileWriter(sourceFile));
		writer.write("env=${env}\n");
		writer.write("<#if env=\"devl\">");
		writer.write("test.dir='${test.dir}'\n");
		writer.write("</#if>");
		writer.write("dir=${dir}\n");
		writer.flush();
		writer.close();
		
		File targetFile = File.createTempFile(getClass().getName() + "-genSource", ".tmp");
		targetFile.deleteOnExit();

		Configuration config = task.createFreemarkerConfiguration();
		config.setDirectoryForTemplateLoading(sourceFile.getParentFile());
		task.generateFile(config, mapModel, sourceFile.getParentFile(), sourceFile.getName(), targetFile);

		BufferedReader reader = new BufferedReader(new FileReader(targetFile));
		assertEquals("env=devl", reader.readLine());
		assertEquals("test.dir='/testing'", reader.readLine());
		assertEquals("dir=/mydir", reader.readLine());
		
		reader.close();
	}
	
	
	public void testUsingTransform() throws IOException {

		Map properties = new HashMap();
		properties.put("env", "devl");
		TemplateMapModel mapModel = new TemplateMapModel(properties);

		File sourceFile = File.createTempFile(getClass().getName() + "-usingTransform", ".tmp");
		sourceFile.deleteOnExit();

		// Populate source File
		Writer writer = new BufferedWriter(new FileWriter(sourceFile));
		writer.write("<@mainframeFileFormat>\n");
		writer.write("some text\n");
		// Write line that is too long.
		for (int i = 0; i < MainframeFileFormatTransform.MAX_LINE_LENGTH + 1; i++) {
			writer.write("a");
		}
		writer.write("\n\n");
		
		writer.write("more text\n");
		writer.write("</@mainframeFileFormat>\n");
		writer.flush();
		writer.close();
		
		File targetFile = File.createTempFile(getClass().getName() + "-usingTransform", ".tmp");
		targetFile.deleteOnExit();

		// Must be done before creating configuration
        task.addConfiguredTransform(createMainframeFileFormatTransformSpecification());
        
		Configuration config = task.createFreemarkerConfiguration();
		config.setDirectoryForTemplateLoading(sourceFile.getParentFile());
		
		try {
			task.generateFile(config, mapModel, sourceFile.getParentFile(), sourceFile.getName(), targetFile);
			fail("Expected exception due to line being too long for MainframeFileFormatTransform.");
		} catch (RuntimeException e) {
			// Expected case due to line being too long for MainframeFileFormatTransform.
		}

	}

	public void testValidateSettings() {
		
		// Start with valid setup.
		configTaskWithValidSettings();
		
		task.validateSettings();
		// Expect no exceptions.
		
		task.setEnvPropertiesFile(new File("does not exist I hope"));
		try {
			task.validateSettings();
			fail("Expected Build Exception");
		} catch (BuildException e) {
			// Expected case
		}
		
		configTaskWithValidSettings();
		task.setDestDir(null);
		try {
			task.validateSettings();
			fail("Expected Build Exception");
		} catch (BuildException e) {
			// Expected case
		}
	}
	

	public void testValidateSettingsNoSource() {

		configureTaskWithPropertiesAndDestDir();

		// No source specified, so expect exception.
		try {
			task.validateSettings();
			fail("Expected Build Exception");
		} catch (BuildException e) {
			// Expected case
		}
		
	}

	public void testDiffToUpdateSetting() throws IOException {

		// To test the setting, we copy the contents of the test source directory, do a generation,
		// then touch only the included file and do another generation with with setting enabled.
		
        File originalTestSourceDir = new File(projectRootDir, "src/testSource");
        
        File tempFile = File.createTempFile("EnvGenDiffTest", "");
        File tempTestSourceDir = new File(tempFile.getParentFile(), "EnvGenDiffToUpdateTest");
        assertTrue(tempFile.delete());
        FileUtilities.deleteRecursively(tempTestSourceDir);
        assertTrue(tempTestSourceDir.mkdir());
        tempTestSourceDir.deleteOnExit();
        
        // Copy files from originalTestSourceDir to tempTestSourceDir
        FileUtils fileUtils = FileUtils.getFileUtils();

        FileSet originalFileSet = new FileSet();
		originalFileSet.setDir(originalTestSourceDir);
		DirectoryScanner scanner = originalFileSet.getDirectoryScanner(new Project());
		String[] includedFilenames = scanner.getIncludedFiles();
		
		for (int fileIndex = 0; fileIndex < includedFilenames.length; fileIndex++) {
			String sourceFilename = includedFilenames[fileIndex];
			fileUtils.copyFile(new File(originalTestSourceDir, sourceFilename), 
				new File(tempTestSourceDir, sourceFilename));
		}

		configureTaskWithPropertiesAndDestDir();
		task.setOverwrite(false);
		task.setDiffToUpdate(true);
        task.addConfiguredTransform(createMainframeFileFormatTransformSpecification());
        task.addConfiguredTransform(createSkipGenerationTransformSpecification());
        FileSet testFileSet = new FileSet();
        testFileSet.setDir(tempTestSourceDir);
        task.addConfiguredSource(testFileSet);
        
		task.execute();
		verifyTargetFilesAndDirectories();
		assertEquals(11, task.getNumFilesGenerated());
		
		// Verify that execution a second time succeeds with nothing done
		task.execute();
		verifyTargetFilesAndDirectories();
		assertEquals(0, task.getNumFilesGenerated());

		File fileBeingIncluded = new File(tempTestSourceDir, "fileInMainDir.txt");
		
		// Touch main file - nothing should change.
        fileUtils.setFileLastModified(fileBeingIncluded, 
        	System.currentTimeMillis());

        task.execute();
		verifyTargetFilesAndDirectories();
		// No content has changed
		assertEquals(0, task.getNumFilesGenerated());
		
		// Modify content of fileBeingIncluded
		fileUtils.copyFile(new File(tempTestSourceDir, "${env}-props.txt"), fileBeingIncluded, null, true);
        task.execute();
		verifyTargetFilesAndDirectories();
		// Regenerate both the fileInMainDir plus the file that includes it = 2 files x 3 environments = 6 total
		assertEquals(6, task.getNumFilesGenerated());
		
	}

	// This is an integration test to verify the behavior of the entire task.
	public void testFullExecution() {

		configTaskWithValidSettings();
        
        task.addConfiguredTransform(createMainframeFileFormatTransformSpecification());
        task.addConfiguredTransform(createSkipGenerationTransformSpecification());

		task.execute();
		verifyTargetFilesAndDirectories();
		assertEquals(11, task.getNumFilesGenerated());
		
		// Verify that execution a second time succeeds with nothing done
		task.setOverwrite(false);
		task.execute();
		verifyTargetFilesAndDirectories();
		assertEquals(0, task.getNumFilesGenerated());
		
		// Verify that execution a second time succeeds and changes all files
		task.setOverwrite(true);
		task.execute();
		verifyTargetFilesAndDirectories();
		
		// This is 12 because there are 3 files that don't vary by environment, but they are still 
		// regenerated per environment, plus the 2 files that do vary by environment,
		// plus the file that is only generated for two of the three environments (the third is skipped): 
		// (3 + 2) x 3 + 2 environments = 17
		assertEquals(17, task.getNumFilesGenerated());
	}

	
	private void configTaskWithValidSettings() {
		configureTaskWithPropertiesAndDestDir();
        
        FileSet fileSet = new FileSet();
        fileSet.setDir(new File(projectRootDir, "src/testSource"));
        task.addConfiguredSource(fileSet);

	}

	private void configureTaskWithPropertiesAndDestDir() {
		task.setEnvPropertiesFile(new File(projectRootDir, "src/testResources/envProperties.csv"));

		task.setDestDir(testTargetDir);
	}


	private EnvGenTask.TransformSpecification createMainframeFileFormatTransformSpecification() {
		EnvGenTask.TransformSpecification transform = new EnvGenTask.TransformSpecification();
        transform.setName("mainframeFileFormat");
        transform.setClass(MainframeFileFormatTransform.class.getName());
		return transform;
	}

	private EnvGenTask.TransformSpecification createSkipGenerationTransformSpecification() {
		EnvGenTask.TransformSpecification transform = new EnvGenTask.TransformSpecification();
        transform.setName("skipGeneration");
        transform.setClass(SkipGenerationTransform.class.getName());
		return transform;
	}

	private void verifyTargetFilesAndDirectories() {
		File devlProps = new File(testTargetDir, "devl-props.txt");
		File testProps = new File(testTargetDir, "test-props.txt");
		File prodProps = new File(testTargetDir, "prod-props.txt");

		verifyFileExistsWithContent(devlProps);
		verifyFileExistsWithContent(testProps);
		verifyFileExistsWithContent(prodProps);
		
		File fileInMainDir = new File(testTargetDir, "fileInMainDir.txt");
		verifyFileExistsWithContent(fileInMainDir);
		
		File subDir = new File(testTargetDir, "subDir");
		File fileInSubDir = new File(subDir, "fileInSubDir.txt");
		verifyFileExistsWithContent(fileInSubDir);

		File devlDir = new File(testTargetDir, "devl");
		File testDir = new File(testTargetDir, "test");
		File prodDir = new File(testTargetDir, "prod");
		
		final String genFilename = "gen.txt.ftl";
		File devlGen = new File(devlDir, genFilename);
		File testGen = new File(testDir, genFilename);
		File prodGen = new File(prodDir, genFilename);

		verifyFileExistsWithContent(devlGen);
		verifyFileExistsWithContent(testGen);
		verifyFileExistsWithContent(prodGen);
	}

	private void verifyFileExistsWithContent(File file) {
		assertTrue(file.isFile());
		assertTrue(file.exists());
		assertTrue(file.length() > 0);
	}
	
}
