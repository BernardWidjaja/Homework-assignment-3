import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.Desktop;
import java.io.*;
import java.io.IOException;

public class MainUI {
    private JTextField sBoxIdField;
    private JTextField weightField;
    private JTextField descField;
    private JButton storeBoxButton;
    private JTextArea outputArea;
    private JPanel mainPanel;
    private JTextField rBoxIdField;
    private JButton retrieveBoxButton;
    private JTextField baseFolderField;
    private JTextField yearField;
    private JTextField monthField;
    private JTextField fileNameField;
    private JTextField dateField;
    private JButton viewLogButton;
    private JButton deleteLogButton;
    private JTextField destinationPathField;
    private JButton moveLogButton;
    private JButton displayAllInformationButton;
    private JButton clearButton;

    private static MainUI instance;

    public static void appendOutput(String text) {
        if (instance != null && instance.outputArea != null) {
            instance.outputArea.append(text + "\n");
        } else {
            System.out.println("[WARN] UI not ready yet: " + text);
        }
    }

    public void clearAllFields() {
        sBoxIdField.setText("");
        weightField.setText("");
        descField.setText("");
        rBoxIdField.setText("");
        baseFolderField.setText("");
        yearField.setText("");
        monthField.setText("");
        dateField.setText("");
        fileNameField.setText("");
        destinationPathField.setText("");
    }


    public MainUI() {
        instance = this;
        storeBoxButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String id = sBoxIdField.getText();
                    String weight = weightField.getText();
                    String desc = descField.getText();

                    if (CapstoneProject.area.findBoxById(id) != null) {
                        MainUI.appendOutput("[ERROR] Box ID already exists!");
                        CapstoneProject.systemLog.log("[ERROR] Box ID already exists!");
                        CapstoneProject.overallLog.log("[ERROR] Box ID already exists!");
                        return;
                    }

                    if (!weight.matches("\\d+(\\.\\d+)?")) {
                        MainUI.appendOutput("[ERROR] Weight must be numeric!");
                        CapstoneProject.systemLog.log("[ERROR] Invalid weight: " + weight);
                        CapstoneProject.overallLog.log("[ERROR] Invalid weight: " + weight);
                        return;
                    }

                    Box userBox = new Box(id, weight, desc);
                    Position emptySlot = CapstoneProject.area.findEmptySlot();

                    if (emptySlot != null) {
                        userBox.setPosition(emptySlot.getRow(), emptySlot.getCol());
                        CapstoneProject.enteredLog.recordEvent(userBox);

                        Storing storeProcess = new Storing(CapstoneProject.storingActive, CapstoneProject.storingStandby, userBox, CapstoneProject.area, CapstoneProject.station);

                        new Thread(() -> {
                            try {
                                storeProcess.execute();
                                storeProcess.logProcess();
                                CapstoneProject.storedLog.recordEvent(userBox);
                            } catch (ProcessException | StorageException ex) {
                                System.err.println("[PROCESS ERROR] " + ex.getMessage());
                                CapstoneProject.systemLog.log("[PROCESS ERROR] " + ex.getMessage());
                                CapstoneProject.overallLog.log("[PROCESS ERROR] " + ex.getMessage());
                            }
                        }).start();

                        clearAllFields();
                        MainUI.appendOutput("[INFO] Box " + id + " is being stored...");
                    } else {
                        throw new ProcessException("Storage area is full! Cannot store Box#" + id);
                    }
                } catch (ProcessException | StorageException pe) {
                    System.err.println("[PROCESS ERROR] " + pe.getMessage());
                    CapstoneProject.systemLog.log("[PROCESS ERROR] " + pe.getMessage());
                    CapstoneProject.overallLog.log("[PROCESS ERROR] " + pe.getMessage());
                    MainUI.appendOutput("[PROCESS ERROR] " + pe.getMessage());
                }
            }
        });

        retrieveBoxButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String id = rBoxIdField.getText(); // get input from GUI field

                    Position boxPos = CapstoneProject.area.findBoxById(id);
                    if (boxPos != null) {
                        Box storedBox = CapstoneProject.area.getBoxAt(boxPos.getRow(), boxPos.getCol());
                        storedBox.setPosition(boxPos.getRow(), boxPos.getCol());

                        Retrieving retrieveProcess = new Retrieving(CapstoneProject.retrievingActive, CapstoneProject.retrievingStandby, storedBox, CapstoneProject.area, CapstoneProject.station2);

                        new Thread(() -> {
                            try {
                                retrieveProcess.execute();
                                retrieveProcess.logProcess();
                                CapstoneProject.exitedLog.recordEvent(storedBox);
                            } catch (ProcessException | StorageException ex) {
                                System.err.println("[PROCESS ERROR] " + ex.getMessage());
                                CapstoneProject.systemLog.log("[PROCESS ERROR] " + ex.getMessage());
                                CapstoneProject.overallLog.log("[PROCESS ERROR] " + ex.getMessage());
                            }
                        }).start();

                        clearAllFields();
                        MainUI.appendOutput("[INFO] Box " + id + " is being retrieved...");
                    } else {
                        throw new ProcessException("Box with ID " + id + " not found in storage.");
                    }
                } catch (ProcessException pe) {
                    System.err.println("[PROCESS ERROR] " + pe.getMessage());
                    CapstoneProject.systemLog.log("[PROCESS ERROR] " + pe.getMessage());
                    CapstoneProject.overallLog.log("[PROCESS ERROR] " + pe.getMessage());
                    MainUI.appendOutput("[PROCESS ERROR] " + pe.getMessage());
                }
            }
        });

        displayAllInformationButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearAllFields();
                // Storage info
                System.out.println("=== Storage Info ===");
                MainUI.appendOutput("\n=== Storage Info ===");
                CapstoneProject.area.displayAllBoxes();

                // AGV info
                System.out.println("== AGV Info ===");
                MainUI.appendOutput("\n=== AGV Info ===");
                CapstoneProject.storingActive.displayInfo();
                CapstoneProject.storingStandby.displayInfo();
                CapstoneProject.retrievingActive.displayInfo();
                CapstoneProject.retrievingStandby.displayInfo();

                // Logs
                CapstoneProject.enteredLog.displayLog();
                CapstoneProject.exitedLog.displayLog();
                CapstoneProject.storedLog.displayLog();
            }
        });

        viewLogButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // Get input from GUI text fields
                    String baseFolder = baseFolderField.getText().trim();
                    String year = yearField.getText().trim();
                    String month = monthField.getText().trim();
                    String date = dateField.getText().trim();
                    String fileName = fileNameField.getText().trim();

                    if (!baseFolder.equals("AGV") && !baseFolder.equals("Battery")
                            && !baseFolder.equals("Overall") && !baseFolder.equals("System")) {
                        throw new InvalidPathException("Invalid base folder! Try again.");
                    }
                    if (!year.matches("\\d{4}")) {
                        throw new InvalidPathException("Year must be in yyyy format.");
                    }
                    if (!month.matches("[A-Za-z]{3}")) {
                        throw new InvalidPathException("Month must be in MMM format.");
                    }
                    if (!date.matches("\\d{1,2}")) {
                        throw new InvalidPathException("Date must be numeric.");
                    }
                    if (!fileName.matches("log_\\d{2}-\\d{2}-\\d{2}\\.txt")) {
                        throw new InvalidPathException("File name must be in log_HH-mm-ss.txt format.");
                    }

                    // Build file path
                    File logFile = new File("Logs/" + baseFolder + "/" + year + "/" + month + "/" + date + "/" + fileName);

                    if (!logFile.exists()) {
                        throw new InvalidPathException("File not found. Try again.");
                    }

                    // Open the file
                    Desktop.getDesktop().open(logFile);
                    MainUI.appendOutput("[LOG] Opened file: " + logFile.getAbsolutePath());

                    clearAllFields();
                } catch (InvalidPathException ex) {
                    MainUI.appendOutput("[ERROR] " + ex.getMessage());
                } catch (IOException ex) {
                    MainUI.appendOutput("[ERROR] Could not open file: " + ex.getMessage());

                }
            }
        });

        deleteLogButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // Get input from GUI text fields
                    String baseFolder = baseFolderField.getText().trim();
                    String year = yearField.getText().trim();
                    String month = monthField.getText().trim();
                    String date = dateField.getText().trim();
                    String fileName = fileNameField.getText().trim();

                    // === Your exact same validation logic ===
                    if (!baseFolder.equals("AGV") && !baseFolder.equals("Battery")
                            && !baseFolder.equals("Overall") && !baseFolder.equals("System")) {
                        throw new InvalidPathException("Invalid base folder! Try again.");
                    }
                    if (!year.matches("\\d{4}")) {
                        throw new InvalidPathException("Year must be in yyyy format.");
                    }
                    if (!month.matches("[A-Za-z]{3}")) {
                        throw new InvalidPathException("Month must be in MMM format.");
                    }
                    if (!date.matches("\\d{1,2}")) {
                        throw new InvalidPathException("Date must be numeric.");
                    }
                    if (!fileName.matches("log_\\d{2}-\\d{2}-\\d{2}\\.txt")) {
                        throw new InvalidPathException("File name must be in log_HH-mm-ss.txt format.");
                    }
                    // === Construct file path ===
                    File logFile = new File("Logs/" + baseFolder + "/" + year + "/" + month + "/" + date + "/" + fileName);

                    if (!logFile.exists()) {
                        throw new InvalidPathException("File not found. Try again.");
                    }

                    // === Delete file ===
                    if (!logFile.delete()) {
                        throw new InvalidPathException("Failed to delete file: " + logFile.getAbsolutePath());
                    } else {
                        MainUI.appendOutput("[LOG] File deleted successfully: " + logFile.getAbsolutePath());
                    }

                    clearAllFields();
                } catch (InvalidPathException ex) {
                    MainUI.appendOutput("[ERROR] " + ex.getMessage());
                } catch (Exception ex) {
                    MainUI.appendOutput("[ERROR] Unexpected error: " + ex.getMessage());
                }
            }
        });

        moveLogButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // Read from GUI text fields
                    String baseFolder = baseFolderField.getText().trim();
                    String year = yearField.getText().trim();
                    String month = monthField.getText().trim();
                    String date = dateField.getText().trim();
                    String fileName = fileNameField.getText().trim();
                    String destinationPath = destinationPathField.getText().trim();

                    // Construct source path
                    File sourceFile = new File("Logs/" + baseFolder + "/" + year + "/" + month + "/" + date + "/" + fileName);

                    if (!sourceFile.exists()) {
                        MainUI.appendOutput("[ERROR] Source file not found: " + sourceFile.getAbsolutePath());
                        return;
                    }

                    // Create destination folder if not exists
                    File destinationFolder = new File(destinationPath);
                    if (!destinationFolder.exists()) {
                        destinationFolder.mkdirs();
                        MainUI.appendOutput("[LOG] Created destination folder: " + destinationFolder.getAbsolutePath());
                    }

                    File destinationFile = new File(destinationFolder, fileName);

                    // Copy file
                    try (FileInputStream in = new FileInputStream(sourceFile);
                         FileOutputStream out = new FileOutputStream(destinationFile)) {

                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = in.read(buffer)) > 0) {
                            out.write(buffer, 0, length);
                        }

                    } catch (IOException ex) {
                        MainUI.appendOutput("[ERROR] Error moving file: " + ex.getMessage());
                        return;
                    }

                    // Delete original file after copying
                    if (sourceFile.delete()) {
                        MainUI.appendOutput("[LOG] File moved successfully to: " + destinationFile.getAbsolutePath());
                    } else {
                        MainUI.appendOutput("[WARN] File copied but not deleted from source.");
                    }

                    clearAllFields();
                } catch (Exception ex) {
                    MainUI.appendOutput("[ERROR] Unexpected error: " + ex.getMessage());
                }
            }
        });

        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                outputArea.setText("");
            }
        });
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}
