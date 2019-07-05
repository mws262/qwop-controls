package ui.runner;

import com.fasterxml.jackson.annotation.JsonProperty;
import controllers.Controller_ValueFunction;
import game.GameUnified;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import value.ValueFunction_TensorFlow_StateOnly;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

public class PanelRunner_ControlledTFlow<G extends GameUnified> extends PanelRunner_Controlled<Controller_ValueFunction<ValueFunction_TensorFlow_StateOnly>, G> {

    private File checkpointSaveLocation = new File("src/main/resources/tflow_models/checkpoints");
    private File modelSaveLocation = new File("src/main/resources/tflow_models");

    private final JComboBox<String> modelSelection;
    private final JComboBox<String> checkpointSelection;
    private final JLabel badCheckpointMsg;
    private final JLabel badModelMsg;

    public PanelRunner_ControlledTFlow(@JsonProperty("name") String name, G game) {
        super(name, game, null);

        // Selecting the TFlow model.
        JLabel modelLabel = new JLabel("Model");
        constraints.gridx = 0;
        constraints.gridy = layoutRows - 5;
        constraints.ipady = 0;
        constraints.anchor = GridBagConstraints.PAGE_END;
        add(modelLabel, constraints);

        modelSelection = new JComboBox<>(updateModels());
        constraints.gridx = 0;
        constraints.gridy = layoutRows - 4;
        constraints.ipady = 0;
        constraints.anchor = GridBagConstraints.PAGE_START;
        modelSelection.addPopupMenuListener(new PopupMenuListener() { // Update the available files right before
            // the menu opens.
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                updateModels();
            }
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {}
        });
        modelSelection.setMaximumRowCount(50);
        add(modelSelection, constraints);
        modelSelection.setSelectedIndex(-1);
        modelSelection.addActionListener(this);

        badModelMsg = new JLabel("Model input/output size incorrect.");
        badModelMsg.setForeground(Color.RED);
        badModelMsg.setVisible(false);
        constraints.gridx = 1;
        constraints.gridy = layoutRows - 4;
        constraints.anchor = GridBagConstraints.LINE_END;
        add(badModelMsg, constraints);

        // Selecting the checkpoint.
        JLabel checkpointLabel = new JLabel("Controller checkpoint");
        constraints.gridx = 0;
        constraints.gridy = layoutRows - 3;
        constraints.anchor = GridBagConstraints.PAGE_END;
        add(checkpointLabel, constraints);

        checkpointSelection = new JComboBox<>(updateCheckpoints());
        constraints.gridy = layoutRows - 2;
        constraints.anchor = GridBagConstraints.PAGE_START;

        checkpointSelection.addPopupMenuListener(new PopupMenuListener() { // Update the available files right before
            // the menu opens.
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                updateCheckpoints();
            }
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {}
        });
        checkpointSelection.setMaximumRowCount(50);
        add(checkpointSelection, constraints);
        checkpointSelection.setSelectedIndex(-1);
        checkpointSelection.addActionListener(this);

        badCheckpointMsg = new JLabel("Checkpoint does not match model.");
        badCheckpointMsg.setForeground(Color.RED);
        badCheckpointMsg.setVisible(false);
        constraints.gridx = 1;
        constraints.gridy = layoutRows - 2;
        constraints.anchor = GridBagConstraints.LINE_END;
        add(badCheckpointMsg, constraints);
    }

    private String[] updateCheckpoints() {
        File[] files = checkpointSaveLocation.listFiles();
        Objects.requireNonNull(files);
        if (files.length > 0) {
            Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
            return Arrays.stream(files).map(f -> f.getName().split("\\.")[0]).distinct().toArray(String[]::new);
        } else {
            return new String[0];
        }
    }

    private String[] updateModels() {
        File[] files = modelSaveLocation.listFiles();
        Objects.requireNonNull(files);
        if (files.length > 0) {
            Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
            return Arrays.stream(files).filter(f -> f.getPath().contains(".pb")).map(File::getName).distinct().toArray(String[]::new);
        } else {
            return new String[0];
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        badModelMsg.setVisible(false);
        if (e.getSource().equals(modelSelection)) {
            deactivateTab();
            // Switch controllers to the one
            try {
                String selectedModel = Objects.requireNonNull(modelSelection.getSelectedItem()).toString();
                controller =
                        new Controller_ValueFunction<>(
                                new ValueFunction_TensorFlow_StateOnly(
                                        Paths.get(
                                                modelSaveLocation.getPath(),
                                                selectedModel
                                        ).toFile(),
                                        game
                                )
                        );
                if (tryCheckpoint(checkpointSelection)) {
                    activateTab();
                } else {
                    activateDrawingButNotController();
                }
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            } catch (IllegalArgumentException modeNotMatchingGameException) { // Dimension of model input does not
                // match game state size
                activateDrawingButNotController();
                badModelMsg.setVisible(true);
            }
        } else if (e.getSource().equals(checkpointSelection)) {
            deactivateTab();
            if (tryCheckpoint(checkpointSelection)) {
                activateTab();
            } else {
                activateDrawingButNotController();
            }
        }
    }

    private boolean tryCheckpoint(JComboBox<String> checkpointSelection) {
        if (checkpointSelection == null || controller == null)
            return false;

        badCheckpointMsg.setVisible(false);
        try {
            String checkpointName = (String) checkpointSelection.getSelectedItem();
            controller.getValueFunction().loadCheckpoint(checkpointSaveLocation.getPath() + "/" + checkpointName);
            return true;
        } catch (IOException exception) {
            badCheckpointMsg.setVisible(true);
            return false;
        }
    }
}
