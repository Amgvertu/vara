package info.prorabka.varamy.exporter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class CodeExporter {

    /**
     * Export all Spring Boot project files
     * @param projectPath absolute path to project root
     */
    public static void exportAll(String projectPath) {
        exportJavaFiles(projectPath);
        exportSettingsFiles(projectPath);
    }

    /**
     * Export Java files from specific package, excluding exporter package
     */
    public static void exportJavaFiles(String projectPath) {
        // Path to Java sources: src/main/java/info/prorabka/varamy
        File targetPackageDir = new File(projectPath, "src/main/java/info/prorabka/varamy");
        if (!targetPackageDir.exists()) {
            System.err.println("Folder not found: " + targetPackageDir.getAbsolutePath());
            return;
        }

        File outputFile = new File(projectPath, "allClasses.txt");

        // Find all Java files in package and subpackages, excluding exporter
        List<File> javaFiles = findJavaFilesExcludingExporter(targetPackageDir);

        if (javaFiles.isEmpty()) {
            System.err.println("No Java files found in info/prorabka/varamy (excluding exporter package)");
            return;
        }

        System.out.println("Found Java files: " + javaFiles.size());

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            for (File javaFile : javaFiles) {
                // Calculate relative path from the package root
                String relativePath = getRelativePath(targetPackageDir.getAbsolutePath(), javaFile.getAbsolutePath());
                // Add full package path
                if (relativePath.isEmpty()) {
                    relativePath = "info/prorabka/varamy/" + javaFile.getName();
                } else {
                    relativePath = "info/prorabka/varamy/" + relativePath;
                }

                // Write header
                writer.println("======================= " + relativePath + " =======================");

                // Write file content
                try (java.util.Scanner scanner = new java.util.Scanner(javaFile, "UTF-8")) {
                    while (scanner.hasNextLine()) {
                        writer.println(scanner.nextLine());
                    }
                }

                writer.println(); // Add empty line between files
            }
            System.out.println("Java files exported to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error exporting Java files: " + e.getMessage());
        }
    }

    /**
     * Recursively find all Java files, excluding exporter package
     */
    private static List<File> findJavaFilesExcludingExporter(File directory) {
        List<File> javaFiles = new ArrayList<>();

        if (!directory.exists() || !directory.isDirectory()) {
            return javaFiles;
        }

        // Skip exporter directory
        if (directory.getName().equals("exporter")) {
            return javaFiles;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    javaFiles.addAll(findJavaFilesExcludingExporter(file));
                } else if (file.getName().endsWith(".java")) {
                    javaFiles.add(file);
                }
            }
        }

        return javaFiles;
    }

    /**
     * Export Spring Boot configuration files
     */
    public static void exportSettingsFiles(String projectPath) {
        File outputFile = new File(projectPath, "settings.txt");

        // List of files to export
        List<FileEntry> filesToExport = new ArrayList<>();

        // Application configuration
        File applicationYml = new File(projectPath, "src/main/resources/application.yml");
        if (applicationYml.exists()) {
            filesToExport.add(new FileEntry(applicationYml, "src/main/resources/application.yml"));
        } else {
            // Try application.properties if yml doesn't exist
            File applicationProperties = new File(projectPath, "src/main/resources/application.properties");
            if (applicationProperties.exists()) {
                filesToExport.add(new FileEntry(applicationProperties, "src/main/resources/application.properties"));
            }
        }

        // Gradle files
        filesToExport.add(new FileEntry(new File(projectPath, "settings.gradle"), "settings.gradle"));
        filesToExport.add(new FileEntry(new File(projectPath, "gradle.properties"), "gradle.properties"));

        // Build.gradle (root)
        File buildGradle = new File(projectPath, "build.gradle");
        if (buildGradle.exists()) {
            filesToExport.add(new FileEntry(buildGradle, "build.gradle"));
        }

        // Additional useful files
        File gradleWrapperProps = new File(projectPath, "gradle/wrapper/gradle-wrapper.properties");
        if (gradleWrapperProps.exists()) {
            filesToExport.add(new FileEntry(gradleWrapperProps, "gradle/wrapper/gradle-wrapper.properties"));
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            for (FileEntry fileEntry : filesToExport) {
                File file = fileEntry.file;
                String displayName = fileEntry.displayName;

                if (!file.exists()) {
                    writer.println("======================= " + displayName + " =======================");
                    writer.println("File not found: " + file.getAbsolutePath());
                    writer.println();
                    continue;
                }

                // Write header
                writer.println("======================= " + displayName + " =======================");

                // Write file content
                try (java.util.Scanner scanner = new java.util.Scanner(file, "UTF-8")) {
                    while (scanner.hasNextLine()) {
                        writer.println(scanner.nextLine());
                    }
                } catch (IOException e) {
                    writer.println("Error reading file: " + e.getMessage());
                }

                writer.println(); // Add empty line between files
            }
            System.out.println("Configuration files exported to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error exporting settings: " + e.getMessage());
        }
    }

    /**
     * Export with option to include resource files
     */
    public static void exportAllWithResources(String projectPath) {
        exportJavaFiles(projectPath);
        exportSettingsFiles(projectPath);
        exportResourceFiles(projectPath);
    }

    /**
     * Export additional resource files (templates, static, etc.)
     */
    public static void exportResourceFiles(String projectPath) {
        File outputFile = new File(projectPath, "resources.txt");

        List<File> resourceFiles = new ArrayList<>();

        // Templates folder
        File templatesDir = new File(projectPath, "src/main/resources/templates");
        if (templatesDir.exists()) {
            resourceFiles.addAll(findFilesByExtension(templatesDir, ".html"));
            resourceFiles.addAll(findFilesByExtension(templatesDir, ".ftl"));
            resourceFiles.addAll(findFilesByExtension(templatesDir, ".thymeleaf"));
        }

        // Static folder
        File staticDir = new File(projectPath, "src/main/resources/static");
        if (staticDir.exists()) {
            resourceFiles.addAll(findFilesByExtension(staticDir, ".css"));
            resourceFiles.addAll(findFilesByExtension(staticDir, ".js"));
            resourceFiles.addAll(findFilesByExtension(staticDir, ".html"));
        }

        // SQL files
        File sqlDir = new File(projectPath, "src/main/resources/db");
        if (sqlDir.exists()) {
            resourceFiles.addAll(findFilesByExtension(sqlDir, ".sql"));
        }

        if (resourceFiles.isEmpty()) {
            System.out.println("No additional resource files found");
            return;
        }

        System.out.println("Found resource files: " + resourceFiles.size());

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            for (File resourceFile : resourceFiles) {
                String relativePath = getRelativePath(projectPath + "/src/main/resources", resourceFile.getAbsolutePath());

                writer.println("======================= src/main/resources/" + relativePath + " =======================");

                try (java.util.Scanner scanner = new java.util.Scanner(resourceFile, "UTF-8")) {
                    while (scanner.hasNextLine()) {
                        writer.println(scanner.nextLine());
                    }
                }

                writer.println();
            }
            System.out.println("Resource files exported to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error exporting resource files: " + e.getMessage());
        }
    }

    /**
     * Recursively find all Java files (legacy method, kept for compatibility)
     */
    private static List<File> findJavaFiles(File directory) {
        List<File> javaFiles = new ArrayList<>();

        if (!directory.exists() || !directory.isDirectory()) {
            return javaFiles;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    javaFiles.addAll(findJavaFiles(file));
                } else if (file.getName().endsWith(".java")) {
                    javaFiles.add(file);
                }
            }
        }

        return javaFiles;
    }

    /**
     * Find files by extension
     */
    private static List<File> findFilesByExtension(File directory, String extension) {
        List<File> files = new ArrayList<>();

        if (!directory.exists() || !directory.isDirectory()) {
            return files;
        }

        File[] fileList = directory.listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                if (file.isDirectory()) {
                    files.addAll(findFilesByExtension(file, extension));
                } else if (file.getName().endsWith(extension)) {
                    files.add(file);
                }
            }
        }

        return files;
    }

    /**
     * Get relative path
     */
    private static String getRelativePath(String basePath, String fullPath) {
        try {
            File base = new File(basePath);
            File full = new File(fullPath);

            String baseCanonical = base.getCanonicalPath();
            String fullCanonical = full.getCanonicalPath();

            if (fullCanonical.startsWith(baseCanonical)) {
                String relative = fullCanonical.substring(baseCanonical.length());
                if (relative.startsWith(File.separator)) {
                    relative = relative.substring(1);
                }
                return relative.replace("\\", "/");
            }

            return full.getName();
        } catch (IOException e) {
            return fullPath;
        }
    }

    /**
     * Helper class to store file information
     */
    private static class FileEntry {
        File file;
        String displayName;

        FileEntry(File file, String displayName) {
            this.file = file;
            this.displayName = displayName;
        }
    }
}