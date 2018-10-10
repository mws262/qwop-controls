package ui;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.swing.JPanel;

import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.common.tools.MainUtils.Flag;

import main.Utility;

public class ScreenCapture {

    private AWTSequenceEncoder encoder;
    private Robot robot;

    public ScreenCapture(File saveFile) {
        try {
            this.encoder = AWTSequenceEncoder.create25Fps(saveFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        } finally {

        }
    }

    public void takeFrame(Rectangle rectangle) throws IOException {
        BufferedImage image = robot.createScreenCapture(rectangle);
        encoder.encodeImage(image);
    }

    public void takeFrameFromJPanel(JPanel panel) throws IOException {
        BufferedImage bi = new BufferedImage(1440, 720, 1); // This is evil hacks
        Graphics2D g = bi.createGraphics();
        panel.print(g);
        encoder.encodeImage(bi);
        g.dispose();
    }

    public void finalize() throws IOException {
        encoder.finish();
    }
}