package ui;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.spi.StandardLevel;
import org.eclipse.jetty.util.ConcurrentHashSet;
import ui.IUserInterface.TabbedPaneActivator;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;

import static ui.PanelLogger.StyleType.*;

/**
 * A console panel that displays logged information from log4j2. Various filtering capabilities.
 *
 * @author matt
 */
public class PanelLogger extends JPanel implements TabbedPaneActivator {

    /**
     * Should this panel be drawing or is it hidden.
     */
    transient protected boolean active = false;

    /**
     * Text pane to which the log information is actually displayed.
     */
    private final JTextPane logTextPane;

    /**
     * A drop-down menu which allows the user to select which log sources to focus display on.
     */
    private final JComboBox<String> classFilterSelector;

    /**
     * Drop-down menu string that results in all log sources being displayed.
     */
    private final String allClassesLabel = "< all sources >";

    /**
     * Colors for parts of the log panel.
     */
    private static final Color logBackground = new Color(128, 128, 128, 60);
    private static final Color scrollBarBackground = new Color(155, 155, 155, 128);
    private static final Color scrollBarColor = new Color(80, 101, 127, 180);
    private static final Color scrollButtonColor = new Color(54, 92, 109, 240);

    private static final Color baseTextColor = new Color(PanelTree.darkText[0], PanelTree.darkText[1],
            PanelTree.darkText[2]);

    /**
     * Custom log4j2 appender that receives log events and chooses how to display them.
     */
    private final PanelLogAppender appender;

    /**
     * Style types for various log levels.
     */
    enum StyleType {
        WHITE, RED, BLUE, YELLOW, GREEN,
        WHITE_BOLD, RED_BOLD, BLUE_BOLD, YELLOW_BOLD, GREEN_BOLD
    }

    public PanelLogger() {

        setLayout(new BorderLayout());
        setBackground(logBackground);

        // Create the text pane.
        logTextPane = new JTextPane();
        logTextPane.setEditable(false);
        logTextPane.setBackground(logBackground);
        logTextPane.setBorder(null);
        this.setBorder(null);

        // Create the scrolling pane that holds the text pane.
        JScrollPane scrollPane = new JScrollPane(logTextPane);
        scrollPane.setPreferredSize(new Dimension(1000, 200));
        scrollPane.setBorder(null);
        scrollPane.setBackground(logBackground);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setWheelScrollingEnabled(true);
        scrollPane.getVerticalScrollBar().setBackground(scrollBarBackground);
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() { // Allows scroll bar color customization.
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = scrollBarColor;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                JButton decreaseButton = new JButton();
                decreaseButton.setPreferredSize(new Dimension(22, 22));
                decreaseButton.setBackground(scrollButtonColor);
                return decreaseButton;
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                JButton increaseButton = new JButton();
                increaseButton.setPreferredSize(new Dimension(22, 22));
                increaseButton.setBackground(scrollButtonColor);
                return increaseButton;
            }
        });
        new SmartScroller(scrollPane); // Makes scrolling lock to the end sometimes in a behavior that's like the
        // console in an IDE.

        add(scrollPane, BorderLayout.CENTER);

        // Programmatic configuration of log4j appender.
        LoggerContext lc = (LoggerContext) LogManager.getContext(false);
        PatternLayout layout = PatternLayout.newBuilder().withConfiguration(lc.getConfiguration()).withPattern(
                "%c{1} %m%n").withAlwaysWriteExceptions(true).build();
        appender = new PanelLogAppender(logTextPane, "appender",
                ThresholdFilter.createFilter(Level.INFO,
                        Filter.Result.ACCEPT, Filter.Result.ACCEPT), layout);
        appender.start();
        // Modifying logging configuration after initialization. See:
        // https://stackoverflow.com/questions/15441477/how-to-add-log4j2-appenders-at-runtime-programmatically/
        lc.getConfiguration().addAppender(appender);
        lc.getRootLogger().addAppender(lc.getConfiguration().getAppender(appender.getName()));
        lc.updateLoggers();

        // Filtering of log display. Checkboxes and drop-down menu.
        JPanel optionsPanel = new JPanel();
        optionsPanel.setBackground(logBackground.darker());
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.X_AXIS));

        classFilterSelector = new JComboBox<>(new String[]{allClassesLabel});
        classFilterSelector.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED)
                appender.rewrite();
        });
        optionsPanel.add(classFilterSelector);
        addLevelCheckbox("Errors", StandardLevel.ERROR, true, optionsPanel);
        addLevelCheckbox("Warnings", StandardLevel.WARN, true, optionsPanel);
        addLevelCheckbox("Info", StandardLevel.INFO, true, optionsPanel);

        add(optionsPanel, BorderLayout.NORTH);

        setVisible(true);
    }

    /**
     * Add a checkbox that will trigger a log text update when changed.
     * @param name Text name displayed with the checkbox.
     * @param logLevel Logging level enabled with this checkbox.
     * @param isSelected Whether the checkbox is selected by default.
     * @param panel Panel to add this checkbox to.
     */
    private void addLevelCheckbox(String name, StandardLevel logLevel, boolean isSelected, JPanel panel) {
        JCheckBox selector = new JCheckBox(name);
        selector.setOpaque(false);
        selector.setForeground(baseTextColor.brighter());
        selector.addActionListener(e -> {
            if (selector.isSelected()) {
                if (!appender.activeLogLevels.contains(logLevel)) {
                    appender.activeLogLevels.add(logLevel);
                    appender.rewrite();
                }
            } else {
                if (appender.activeLogLevels.contains(logLevel)) {
                    appender.activeLogLevels.remove(logLevel);
                    appender.rewrite();
                }
            }
        });
        selector.setSelected(isSelected);
        panel.add(selector);
    }

    @Override
    public void activateTab() {
        active = true;
    }

    @Override
    public void deactivateTab() {
        active = false;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public String getName() {
        return "Logging";
    }

    /**
     * Custom log4j2 appender for receiving <code>LogEvent</code> and figuring out how to display the information.
     */
    public class PanelLogAppender extends AbstractAppender {

        /**
         * Text pane to which all the log messages are displayed.
         */
        private JTextPane logTextPane;

        /**
         * Basically a child of the text pane that allows better customization of the text.
         */
        private StyledDocument doc;

        /**
         * All logging lines, including those that are not being displayed currently. All are stored in case other
         * filtered log messages are reactivated.
         */
        private List<LogLine> logLines = new ArrayList<>();

        /**
         * Logging levels that are actively being displayed. Can be changed as filters are changed.
         */
        private Set<StandardLevel> activeLogLevels = new ConcurrentHashSet<>();

        /**
         * Text styles to be used with various log levels. Defines text weight, color, size, etc.
         */
        private Map<StyleType, Style> styles = new HashMap<>();

        protected PanelLogAppender(JTextPane logTextArea, String name, Filter filter, Layout layout) {
            super(name, filter, layout, false, Property.EMPTY_ARRAY);
            this.logTextPane = logTextArea;
            doc = logTextArea.getStyledDocument();
            addStyle(WHITE, WHITE_BOLD, PanelLogger.baseTextColor);
            addStyle(RED, RED_BOLD, new Color(181, 29, 49));
            addStyle(BLUE, BLUE_BOLD, new Color(37, 126, 173));
            addStyle(YELLOW, YELLOW_BOLD, new Color(176, 156, 37));
            addStyle(GREEN, GREEN_BOLD, new Color(91, 171, 124));

            // All log levels are active here but some are currently disabled in the log4j.xml configuration file.
            // For now, I'm keeping this way because the quantity of debug and trace messages that come through is
            // pretty massive and I'd rather not waste the effort of storing them all.
            activeLogLevels.add(StandardLevel.ALL);
            activeLogLevels.add(StandardLevel.ERROR);
            activeLogLevels.add(StandardLevel.FATAL);
            activeLogLevels.add(StandardLevel.INFO);
            activeLogLevels.add(StandardLevel.OFF);
            activeLogLevels.add(StandardLevel.WARN);
        }

        @Override
        public void append(LogEvent event) {
            // Decide what the text should look like based on the logging level.
            Style levelStyle;
            Style sourceStyle;
            Style msgStyle;
            switch (event.getLevel().getStandardLevel()) {
                case OFF:
                    levelStyle = sourceStyle = styles.get(WHITE_BOLD);
                    msgStyle = styles.get(WHITE);
                    break;
                case FATAL:
                    levelStyle = sourceStyle = styles.get(RED_BOLD);
                    msgStyle = styles.get(RED_BOLD);
                    break;
                case ERROR:
                    levelStyle = sourceStyle = styles.get(RED_BOLD);
                    msgStyle = styles.get(RED);
                    break;
                case WARN:
                    levelStyle = sourceStyle = styles.get(YELLOW_BOLD);
                    msgStyle = styles.get(YELLOW);
                    break;
                case INFO:
                    levelStyle = sourceStyle = styles.get(WHITE_BOLD);
                    msgStyle = styles.get(WHITE);
                    break;
                case DEBUG:
                    levelStyle = sourceStyle = styles.get(GREEN_BOLD);
                    msgStyle = styles.get(GREEN);
                    break;
                case TRACE:
                    levelStyle = sourceStyle = styles.get(GREEN_BOLD);
                    msgStyle = styles.get(GREEN);
                    break;
                case ALL:
                    levelStyle = sourceStyle = styles.get(WHITE_BOLD);
                    msgStyle = styles.get(WHITE);
                    break;
                default:
                    levelStyle = sourceStyle = styles.get(WHITE_BOLD);
                    msgStyle = styles.get(WHITE);
            }

            // All the log information for this event. Stays stored even if it currently is not being displayed.
            LogLine line = new LogLine(
                    event.getLevel().getStandardLevel(),
                    "[" + event.getLevel().name() + "]    ",
                    levelStyle,
                    event.getSource().getFileName().split("\\.")[0],
                    sourceStyle,
                    // This crazy regex basically makes the message part of the log entry wrap with indents matching
                    // the previous line.
                    new String(getLayout().toByteArray(event), StandardCharsets.UTF_8)
                            .replaceAll("(?m)^", "\t\t\t")
                            .replaceFirst("\t\t", ""), msgStyle);

            logLines.add(line);
            line.writeLineIf(); // Only will display if not filtered out.
        }

        /**
         * Add a text style and associate with a particular enum value.
         */
        private void addStyle(StyleType type, StyleType boldType, Color color) {
            Style style = logTextPane.addStyle(type.name(), null);
            StyleConstants.setFontSize(style, 14);
            StyleConstants.setForeground(style, color);
            styles.put(type, style);

            style = logTextPane.addStyle(boldType.name(), style);
            StyleConstants.setBold(style, true);
            styles.put(boldType, style);
        }

        /**
         * Re-write the entire text log on screen. Should be triggered when filtering parameters have been changed.
         */
        void rewrite() {
            logTextPane.setText("");
            for (LogLine line : logLines) {
                line.writeLineIf();
            }
        }

        /**
         * Stores pertinent information from log events.
         */
        class LogLine {
            private final StandardLevel level;
            private final String levelStr;
            private final Style levelStyle;
            private final String source;
            private final Style sourceStyle;
            private final String message;
            private final Style messageStyle;

            /**
             * Log lines consist of three message portions: The level, e.g. [Warn], the source class, and the log
             * message.
             */
            LogLine(StandardLevel level, String levelStr, Style levelStyle, String source, Style sourceStyle,
                    String message,
                    Style messageStyle) {

                this.level = level;
                this.levelStr = levelStr;
                this.levelStyle = levelStyle;
                this.source = source;
                this.sourceStyle = sourceStyle;
                this.message = message;
                this.messageStyle = messageStyle;

                if (((DefaultComboBoxModel) classFilterSelector.getModel()).getIndexOf(source) == -1) {
                    classFilterSelector.addItem(source);
                }
            }

            /**
             * Decides whether to write this LineLog to the panel or not and handles the mechanics of writing to the
             * onscreen log.
             */
            void writeLineIf() {
                // Log level needs to be active, and if so, then this class source can't be filtered out.
                if (activeLogLevels.contains(level)
                        && (source.equals(classFilterSelector.getSelectedItem()) || classFilterSelector.getSelectedItem() == allClassesLabel)) {
                    try {
                        doc.insertString(doc.getLength(), levelStr, levelStyle);
                        doc.insertString(doc.getLength(), source, sourceStyle);
                        doc.insertString(doc.getLength(), message, messageStyle);
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
