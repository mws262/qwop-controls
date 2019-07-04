package ui.runner;

import com.fasterxml.jackson.annotation.JsonProperty;
import controllers.Controller_ValueFunction;
import controllers.IController;
import game.GameUnified;
import game.IGameInternal;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import value.ValueFunction_TensorFlow;
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

public class PanelRunner_ControlledTFlow extends PanelRunner_Controlled {

    private File checkpointSaveLocation = new File("src/main/resources/tflow_models/checkpoints");
    private File modelSaveLocation = new File("src/main/resources/tflow_models");

    private final JComboBox<String> modelSelection;
    private final JComboBox<String> checkpointSelection;
    private final JLabel badCheckpointMsg;

    public PanelRunner_ControlledTFlow(@JsonProperty("name") String name, GameUnified game,
                                       ValueFunction_TensorFlow valueFunction) {
        super(name, game, new Controller_ValueFunction(valueFunction));

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

        add(modelSelection, constraints);
        modelSelection.addActionListener(this);

        // Selecting the checkpoint.
        JLabel checkpointLabel = new JLabel("Controller checkpoint");
        constraints.gridy = layoutRows - 3;
        constraints.anchor = GridBagConstraints.PAGE_END;
        add(checkpointLabel, constraints);

        checkpointSelection = new JComboBox<>(updateCheckpoints());
        constraints.gridy = layoutRows - 2;
        constraints.anchor = GridBagConstraints.PAGE_START;

        add(checkpointSelection, constraints);
        checkpointSelection.addActionListener(this);
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
        if (e.getSource().equals(modelSelection)) {
            System.out.println("Model selection!");
            System.out.println(((JComboBox<String>) e.getSource()).getSelectedItem());
            deactivateTab();
            // Switch controllers to the one
            try {
                controller =
                        new Controller_ValueFunction(
                                new ValueFunction_TensorFlow_StateOnly(
                                        Paths.get(
                                                modelSaveLocation.getPath(),
                                                modelSelection.getSelectedItem().toString()
                                        ).toFile(),
                                        game
                                )
                        );
                tryCheckpoint(checkpointSelection);
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            }

            activateTab();
        } else if (e.getSource().equals(checkpointSelection)) {
            deactivateTab();
            tryCheckpoint(checkpointSelection);
            activateTab();

        }
    }

    private void tryCheckpoint(JComboBox<String> checkpointSelection) {
        badCheckpointMsg.setVisible(false);
        System.out.println("Checkpoint selection!");
        System.out.println(checkpointSelection.getSelectedItem());
        // TODO: below is probably the worst line of code I've ever written.
        try {
            ((ValueFunction_TensorFlow) ((Controller_ValueFunction) controller).getValueFunction()).loadCheckpoint(checkpointSaveLocation.getPath() + "/" + checkpointSelection.getSelectedItem().toString());
        } catch (IOException exception) {
            System.out.println("Couldn't use this one.");
            badCheckpointMsg.setVisible(true);
            return;
        }
        System.out.println("Loaded checkpoint!");
    }
}
