package storage;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Basic file read/write utility class.
 * 
 * Supports:
 * - Reading all lines from a file
 * - Overwriting a file
 * - Appending a line to a file
 * 
 * Uses BufferedReader/BufferedWriter to optimize I/O performance.
 */
public class FileHandler {

    private String filePath;

    /**
     * Initializes FileHandler with a file path.
     * 
     * @param filePath path to the file (relative or absolute)
     */
    public FileHandler(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Reads all lines from the file.
     * Automatically returns an empty list if file doesn't exist.
     * 
     * @return list of lines, empty list if file does not exist
     */
    public List<String> readLines() {
        List<String> lines = new ArrayList<>();
        File file = new File(filePath);

        if (!file.exists()) {
            return lines;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            System.out.println("File read error: " + e.getMessage());
        }

        return lines;
    }

    /**
     * Overwrites the entire file content.
     * Automatically creates parent directories if they don't exist.
     * 
     * @param lines list of lines to write
     */
    public void writeLines(List<String> lines) {
        ensureParentDirectory();

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8"))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("File write error: " + e.getMessage());
        }
    }

    /**
     * Appends a single line to the end of the file.
     * Automatically creates file and directory if they don't exist.
     * 
     * @param line line to append
     */
    public void appendLine(String line) {
        ensureParentDirectory();

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath, true), "UTF-8"))) {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("File append error: " + e.getMessage());
        }
    }

    /**
     * Ensures parent directories exist.
     */
    private void ensureParentDirectory() {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    /**
     * Gets the file path.
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Checks if the file exists.
     */
    public boolean exists() {
        return new File(filePath).exists();
    }
}
