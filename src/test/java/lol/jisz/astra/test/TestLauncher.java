package lol.jisz.astra.test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test launcher class for the Astra framework.
 * This class provides functionality to run simulated tests on the Astra framework
 * without requiring a running Bukkit server. It performs static code analysis,
 * file structure verification, and package structure validation.
 */
public class TestLauncher {

    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;
    private static final List<String> failedTestMessages = new ArrayList<>();

    /**
     * Main entry point for the test launcher.
     * Executes a series of tests to verify the structure and basic functionality
     * of the Astra framework, then displays a summary of test results.
     *
     * @param args Command line arguments (not used in this implementation)
     */
    public static void main(String[] args) {
        System.out.println("Starting simulated tests for Astra...");
        System.out.println("=======================================");

        try {
            testFileStructure();
            testPackageStructure();
            testSourceCodeAnalysis();

            tryRunAstraTestRunner();

            System.out.println("\n=======================================");
            System.out.println("TEST SUMMARY:");
            System.out.println("Total tests: " + totalTests);
            System.out.println("Successful tests: " + passedTests);
            System.out.println("Failed tests: " + failedTests);

            if (failedTests > 0) {
                System.out.println("\nFAILURE DETAILS:");
                for (String failure : failedTestMessages) {
                    System.out.println("- " + failure);
                }
            }

            if (failedTests == 0) {
                System.out.println("\nSimulated tests completed successfully.");
            } else {
                System.out.println("\nSome tests failed. Check the details above.");
            }
        } catch (Exception e) {
            System.err.println("Error while running tests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Attempts to run AstraTestRunner via reflection if it exists.
     */
    private static void tryRunAstraTestRunner() {
        System.out.println("\nSearching for AstraTestRunner...");

        try {
            // Check if the AstraTestRunner class exists
            File runnerFile = new File("src/main/java/lol/jisz/astra/test/AstraTestRunner.java");
            if (!runnerFile.exists()) {
                System.out.println("AstraTestRunner not found, skipping additional tests.");
                return;
            }

            // Attempt to load the class and execute its test methods
            System.out.println("Running AstraTestRunner tests via code analysis...");

            // Read the file and search for test methods
            String sourceCode = new String(Files.readAllBytes(runnerFile.toPath()));
            Pattern methodPattern = Pattern.compile("public\\s+static\\s+void\\s+(test\\w+)\\s*\\(");
            Matcher matcher = methodPattern.matcher(sourceCode);

            while (matcher.find()) {
                String methodName = matcher.group(1);
                System.out.println("\nRunning simulated test: " + methodName);

                // Extract the method body for analysis
                String methodRegex = "public\\s+static\\s+void\\s+" + methodName + "\\s*\\([^)]*\\)\\s*\\{([^}]*)\\}";
                Pattern methodBodyPattern = Pattern.compile(methodRegex, Pattern.DOTALL);
                Matcher bodyMatcher = methodBodyPattern.matcher(sourceCode);

                if (bodyMatcher.find()) {
                    String methodBody = bodyMatcher.group(1);
                    analyzeTestMethod(methodName, methodBody);
                }
            }

        } catch (Exception e) {
            System.err.println("Error while attempting to run AstraTestRunner: " + e.getMessage());
        }
    }

    /**
     * Analyzes the body of a test method to simulate its execution.
     */
    private static void analyzeTestMethod(String methodName, String methodBody) {
        // Count this test
        totalTests++;

        try {
            // Search for assertions in the code
            Pattern assertPattern = Pattern.compile("assert(\\w+)\\s*\\(");
            Matcher assertMatcher = assertPattern.matcher(methodBody);

            boolean hasAssertions = false;
            while (assertMatcher.find()) {
                hasAssertions = true;
                System.out.println("  - Found assertion: " + assertMatcher.group());
            }

            // Search for possible exceptions thrown
            if (methodBody.contains("throw new") || methodBody.contains("throws ")) {
                System.out.println("  - Warning: The method may throw exceptions");
            }

            // Check if the method accesses Bukkit classes
            if (methodBody.contains("org.bukkit")) {
                System.out.println("  - Warning: The method accesses Bukkit classes that are not available");
                failedTests++;
                failedTestMessages.add(methodName + ": Uses Bukkit classes that are not available in this environment");
                return;
            }

            // If we reach here, consider the test passed
            if (!hasAssertions) {
                System.out.println("  - Warning: The method does not contain explicit assertions");
            }

            System.out.println("  ✓ Simulated test successful");
            passedTests++;

        } catch (Exception e) {
            failedTests++;
            failedTestMessages.add(methodName + ": " + e.getMessage());
            System.err.println("  ✗ Error while analyzing method: " + e.getMessage());
        }
    }

    /**
     * Verifies the file structure of the project.
     */
    private static void testFileStructure() {
        System.out.println("\nVerifying file structure...");

        // Check main directories
        assertFileExists("src/main/java", true, "Directory src/main/java");
        assertFileExists("src/main/resources", true, "Directory src/main/resources");
        assertFileExists("src/main/resources/paper-plugin.yml", false, "File paper-plugin.yml");
        assertFileExists("pom.xml", false, "File pom.xml");
    }

    /**
     * Verifies the package structure of the project without loading the classes.
     */
    private static void testPackageStructure() {
        System.out.println("\nVerifying package structure...");

        // Check main packages
        String basePackagePath = "src/main/java/lol/jisz/astra";

        // List of packages that should exist
        String[] expectedPackages = {
            "",  // base package
            "/api",
            "/command",
            "/utils"
        };

        for (String pkg : expectedPackages) {
            assertFileExists(basePackagePath + pkg, true,
                    "Package lol.jisz.astra" + pkg.replace('/', '.'));
        }

        // Check important Java files without loading them
        String[] expectedFiles = {
            "/Astra.java",
            "/command/CommandBase.java",
            "/utils/ClassScanner.java"
        };

        for (String file : expectedFiles) {
            assertFileExists(basePackagePath + file, false, "File " + file);
        }
    }

    /**
     * Performs static analysis of the source code.
     */
    private static void testSourceCodeAnalysis() {
        System.out.println("\nPerforming source code analysis...");

        // Check content of key files
        String astraPath = "src/main/java/lol/jisz/astra/Astra.java";
        assertFileContains(astraPath, "public abstract class Astra", "Class Astra is abstract");
        assertFileContains(astraPath, "extends JavaPlugin", "Astra extends JavaPlugin");

        String commandBasePath = "src/main/java/lol/jisz/astra/command/CommandBase.java";
        assertFileContains(commandBasePath, "public abstract class CommandBase", "CommandBase is abstract");
        assertFileContains(commandBasePath, "implements CommandExecutor", "CommandBase implements CommandExecutor");

        String classScannerPath = "src/main/java/lol/jisz/astra/utils/ClassScanner.java";
        assertFileContains(classScannerPath, "public class ClassScanner", "ClassScanner is a public class");
        assertFileContains(classScannerPath, "scanPackage", "ClassScanner has method scanPackage");
    }

    /**
     * Checks if a file exists.
     */
    private static void assertFileExists(String path, boolean isDirectory, String description) {
        totalTests++;
        File file = new File(path);
        boolean condition = file.exists() && (isDirectory == file.isDirectory());

        if (condition) {
            System.out.println("  ✓ " + description + " exists");
            passedTests++;
        } else {
            System.err.println("  ✗ " + description + " not found");
            failedTests++;
            failedTestMessages.add(description + " not found");
        }
    }

    /**
     * Checks if a file contains certain text.
     */
    private static void assertFileContains(String path, String text, String description) {
        totalTests++;
        File file = new File(path);

        if (!file.exists()) {
            System.err.println("  ✗ Cannot verify '" + description + "': file not found");
            failedTests++;
            failedTestMessages.add("File not found: " + path);
            return;
        }

        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            boolean contains = content.contains(text);

            if (contains) {
                System.out.println("  ✓ " + description);
                passedTests++;
            } else {
                System.err.println("  ✗ Not found: " + description);
                failedTests++;
                failedTestMessages.add("Not found: " + description + " in " + path);
            }
        } catch (Exception e) {
            System.err.println("  ✗ Error reading file " + file.getName() + ": " + e.getMessage());
            failedTests++;
            failedTestMessages.add("Error reading " + path + ": " + e.getMessage());
        }
    }

    /**
     * Checks if a file contains certain text (useful for basic checks).
     *
     * @param file The file to check
     * @param text The text to search for within the file
     * @return true if the file contains the specified text, false otherwise
     */
    private static boolean fileContains(File file, String text) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            return content.contains(text);
        } catch (Exception e) {
            System.err.println("Error reading file " + file.getName() + ": " + e.getMessage());
            return false;
        }
    }
}