package info.prorabka.varamy.exporter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ExportRunner {
    public static void main(String[] args) {
        String projectPath = null;

        // 1. Если передан аргумент – используем его
        if (args.length > 0) {
            projectPath = args[0];
        }

        // 2. Если аргумент не задан – пытаемся прочитать из setting.txt
        if (projectPath == null || projectPath.isEmpty()) {
            projectPath = readPathFromSettingsFile();
        }

        // 3. Если всё ещё пусто – используем значение по умолчанию (можно изменить)
        if (projectPath == null || projectPath.isEmpty()) {
            projectPath = "e:\\katokPro\\vara_my";
            System.out.println("Using default project path: " + projectPath);
        }

        System.out.println("=== Starting Spring Boot project export ===");
        System.out.println("Project path: " + projectPath);
        System.out.println();

        // Basic export (Java files + settings)
        CodeExporter.exportAll(projectPath);

        System.out.println();
        System.out.println("=== Export completed ===");
        System.out.println("Files saved:");
        System.out.println("  - " + projectPath + "\\allClasses.txt");
        System.out.println("  - " + projectPath + "\\settings.txt");
    }

    /**
     * Reads the first non-empty line from setting.txt file located in the same
     * directory as the JAR (or current working directory).
     * @return project path or null if file not found / empty
     */
    private static String readPathFromSettingsFile() {
        File settingsFile = new File("setting.txt");
        if (!settingsFile.exists()) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(settingsFile))) {
            String line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) {
                return line.trim();
            }
        } catch (IOException e) {
            System.err.println("Error reading setting.txt: " + e.getMessage());
        }
        return null;
    }
}