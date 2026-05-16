package works.makertechno.countdown.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import works.makertechno.countdown.component.ComponentManager;
import works.makertechno.countdown.i18n.I18n;
import works.makertechno.countdown.model.CountdownData;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class SettingsDialog extends JDialog {

    private static final Logger LOG = LoggerFactory.getLogger(SettingsDialog.class);

    private final I18n i18n;
    private final LocalDateTime initialTime;
    private final String initialEventName;
    private final Color initialColor;
    private final Path initialTransPath;
    private final Map<String, String> initialVariables;
    private final Consumer<SettingsResult> onConfirm;
    private final boolean initialShowYearsMonthsDays;
    private final boolean initialAutoStart;
    // 开机自启动变更回调，返回是否设置成功
    private final Function<Boolean, Boolean> onAutoStartChange;
    private JSpinner yearSp, monthSp, daySp, hourSp, minSp, secSp;
    private ColorPickerPanel colorPickerPanel;
    private JLabel transPathLabel;
    private Path selectedTransPath;
    // 内建变量编辑 - 简化为单个 Name 输入框
    private JTextField nameField;
    // 显示模式组件
    private JRadioButton ymdRadio, totalDaysRadio;
    private ButtonGroup displayModeGroup;
    // 开机自启动选项
    private JCheckBox autoStartCheckBox;
    // 组件管理器
    private final ComponentManager componentManager;
    private ComponentPanel componentPanel;

    public SettingsDialog(Frame owner, I18n i18n, CountdownData data,
                          Color currentColor, Path currentTransPath,
                          Map<String, String> currentVariables,
                          boolean currentShowYearsMonthsDays,
                          boolean currentAutoStart,
                          Function<Boolean, Boolean> onAutoStartChange,
                          ComponentManager componentManager,
                          Consumer<SettingsResult> onConfirm) {
        super(owner, i18n.get("settings.title"), true);
        this.i18n = i18n;
        this.initialTime = data.targetDateTime();
        this.initialEventName = data.userDataText();
        this.initialColor = currentColor;
        this.initialTransPath = currentTransPath;
        this.initialVariables = new LinkedHashMap<>(currentVariables);
        this.selectedTransPath = currentTransPath;
        this.initialShowYearsMonthsDays = currentShowYearsMonthsDays;  // 保存初始值
        this.onAutoStartChange = onAutoStartChange;  // 保存回调
        this.componentManager = componentManager;
        this.initialAutoStart = currentAutoStart;
        this.onConfirm = onConfirm;

        setSize(620, 550);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        buildUI();
        if (autoStartCheckBox != null) {
            autoStartCheckBox.setSelected(initialAutoStart);
        }
    }

    private void buildUI() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        mainPanel.add(createVariablesSection());
        mainPanel.add(Box.createVerticalStrut(12));
        mainPanel.add(createTimeSection());
        mainPanel.add(Box.createVerticalStrut(12));
        mainPanel.add(createDisplayModeSection());
        mainPanel.add(Box.createVerticalStrut(12));
        mainPanel.add(createColorSection());
        mainPanel.add(Box.createVerticalStrut(12));
        mainPanel.add(createLanguageSection());
        mainPanel.add(Box.createVerticalStrut(12));
        mainPanel.add(createAutoStartSection());
        mainPanel.add(Box.createVerticalStrut(12));
        mainPanel.add(createComponentSection());
        mainPanel.add(Box.createVerticalStrut(16));
        mainPanel.add(createButtonRow());

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        setContentPane(scrollPane);
    }

    // ==================== 内建变量区域 ====================

    private JPanel createVariablesSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createTitledBorder(i18n.get("settings.variables")));

        JPanel content = new JPanel(new BorderLayout(0, 4));

        // 标签
        JLabel label = new JLabel(i18n.get("settings.display_name") + " (EventName)");
        content.add(label, BorderLayout.NORTH);

        // 输入框
        nameField = new JTextField(initialVariables.getOrDefault("EventName", initialEventName));
        nameField.setPreferredSize(new Dimension(0, 28));
        content.add(nameField, BorderLayout.CENTER);

        panel.add(content, BorderLayout.NORTH);
        return panel;
    }

    // ==================== 时间区域 ====================

    private JPanel createTimeSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(i18n.get("settings.time")));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel label = new JLabel(i18n.get("settings.time_label") + ":");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 6;
        panel.add(label, gbc);

        SpinnerNumberModel yModel = new SpinnerNumberModel(initialTime.getYear(), 2026, 2200, 1);
        SpinnerNumberModel moModel = new SpinnerNumberModel(initialTime.getMonthValue(), 1, 12, 1);
        SpinnerNumberModel dModel = new SpinnerNumberModel(initialTime.getDayOfMonth(), 1, 31, 1);
        SpinnerNumberModel hModel = new SpinnerNumberModel(initialTime.getHour(), 0, 23, 1);
        SpinnerNumberModel miModel = new SpinnerNumberModel(initialTime.getMinute(), 0, 59, 1);
        SpinnerNumberModel sModel = new SpinnerNumberModel(initialTime.getSecond(), 0, 59, 1);

        yearSp = new JSpinner(yModel);
        monthSp = new JSpinner(moModel);
        daySp = new JSpinner(dModel);
        hourSp = new JSpinner(hModel);
        minSp = new JSpinner(miModel);
        secSp = new JSpinner(sModel);

        addTimeRow(panel, gbc, 1, "settings.year", yearSp);
        addTimeRow(panel, gbc, 2, "settings.month", monthSp);
        addTimeRow(panel, gbc, 3, "settings.day", daySp);
        addTimeRow(panel, gbc, 4, "settings.hour", hourSp);
        addTimeRow(panel, gbc, 5, "settings.minute", minSp);
        addTimeRow(panel, gbc, 6, "settings.second", secSp);

        return panel;
    }

    private void addTimeRow(JPanel panel, GridBagConstraints gbc, int row, String key, JSpinner spinner) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(new JLabel(i18n.get(key) + ":"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 5;
        gbc.weightx = 1.0;
        panel.add(spinner, gbc);
    }

    // ===========输出格式区域=============
    private JPanel createDisplayModeSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createTitledBorder(i18n.get("settings.display_mode")));

        ymdRadio = new JRadioButton(i18n.get("settings.show_ymd"));
        totalDaysRadio = new JRadioButton(i18n.get("settings.show_total_days"));

        // 根据初始值设置选中状态
        ymdRadio.setSelected(initialShowYearsMonthsDays);
        totalDaysRadio.setSelected(!initialShowYearsMonthsDays);

        displayModeGroup = new ButtonGroup();
        displayModeGroup.add(ymdRadio);
        displayModeGroup.add(totalDaysRadio);

        // 从配置中读取（需要传入初始值）
        // 这里先默认选中 ymdRadio，后续在构造函数中设置

        JPanel radioPanel = new JPanel(new GridLayout(2, 1, 0, 6));
        radioPanel.setOpaque(false);
        radioPanel.add(ymdRadio);
        radioPanel.add(totalDaysRadio);

        panel.add(radioPanel, BorderLayout.CENTER);
        return panel;
    }
    // ==================== 颜色区域 ====================

    private JPanel createColorSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(i18n.get("settings.color")));

        colorPickerPanel = new ColorPickerPanel(i18n, initialColor, c -> {
        });
        panel.add(colorPickerPanel, BorderLayout.CENTER);
        return panel;
    }

    // ==================== 语言区域 ====================

    private JPanel createLanguageSection() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBorder(BorderFactory.createTitledBorder(i18n.get("settings.language")));

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    buttonPanel.setOpaque(false);

        JButton selectBtn = new JButton(i18n.get("settings.select_file"));
        selectBtn.addActionListener(e -> chooseTransFile());

    JButton resetBtn = new JButton(i18n.get("settings.reset_language"));
    resetBtn.addActionListener(e -> resetToDefaultLanguage());

    buttonPanel.add(selectBtn);
    buttonPanel.add(resetBtn);

    transPathLabel = new JLabel(selectedTransPath != null ? selectedTransPath.toString() : i18n.get("settings.builtin"));
        transPathLabel.setForeground(Color.GRAY);

    panel.add(buttonPanel, BorderLayout.WEST);
        panel.add(transPathLabel, BorderLayout.CENTER);
        return panel;
    }

/**
 * 恢复内置默认语言
 */
private void resetToDefaultLanguage() {
    selectedTransPath = null;
    transPathLabel.setText(i18n.get("settings.builtin"));
    // 注意：这里只是清空了路径，实际的翻译重置需要在确认时执行
    // 所以需要将 selectedTransPath = null 的状态传递出去
}

    // ==================== 开机自启动区域 ====================

    private JPanel createAutoStartSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createTitledBorder(i18n.get("settings.auto_start")));

        autoStartCheckBox = new JCheckBox(i18n.get("settings.auto_start_tip"));
        autoStartCheckBox.setOpaque(false);

        // 添加监听器，勾选/取消时立即弹出确认
        autoStartCheckBox.addActionListener(e -> {
            boolean isSelected = autoStartCheckBox.isSelected();
            // 调用回调，让外部处理开机自启动逻辑
            if (onAutoStartChange != null) {
            boolean success = onAutoStartChange.apply(isSelected);
            if (!success) {
                // 如果设置失败，恢复复选框到之前的状态
                autoStartCheckBox.setSelected(!isSelected);
            }
            }
        });

        panel.add(autoStartCheckBox, BorderLayout.CENTER);
        return panel;
    }
    // ==================== 自定义组件区域 ====================

    private JPanel createComponentSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(i18n.get("component.title")));

        componentPanel = new ComponentPanel(i18n, componentManager);
        panel.add(componentPanel, BorderLayout.CENTER);

        return panel;
    }
    private void chooseTransFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter(i18n.get("settings.filter_trans"), "trans"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedTransPath = chooser.getSelectedFile().toPath();
            transPathLabel.setText(selectedTransPath.toString());
        }
    }

    // ==================== 按钮行 ====================

    private JPanel createButtonRow() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelBtn = new JButton(i18n.get("settings.cancel"));
        JButton confirmBtn = new JButton(i18n.get("settings.confirm"));

        cancelBtn.addActionListener(e -> dispose());
        confirmBtn.addActionListener(e -> {
            LocalDateTime newTime = getSelectedTime();
            if (!newTime.isAfter(LocalDateTime.now())) {
                JOptionPane.showMessageDialog(this, i18n.get("settings.time_must_future"),
                        "", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String eventName = nameField.getText().trim();
            if (eventName.isEmpty()) eventName = " ";

            boolean showYMD = ymdRadio.isSelected();

            onConfirm.accept(new SettingsResult(
                    newTime,
                    colorPickerPanel.getCurrentColor(),
                    selectedTransPath, eventName, showYMD, selectedTransPath == null));
            dispose();
        });

        panel.add(cancelBtn);
        panel.add(confirmBtn);
        return panel;
    }

    private LocalDateTime getSelectedTime() {
        int y = (int) yearSp.getValue();
        int mo = (int) monthSp.getValue();
        int d = Math.min((int) daySp.getValue(),
                LocalDateTime.of(y, mo, 1, 0, 0).toLocalDate().lengthOfMonth());
        int h = (int) hourSp.getValue();
        int mi = (int) minSp.getValue();
        int s = (int) secSp.getValue();
        return LocalDateTime.of(y, mo, d, h, mi, s);
    }

    public record SettingsResult(LocalDateTime targetTime, Color textColor,
                                 Path transPath, String eventName, boolean showYearsMonthsDays, boolean resetLanguage) {
    }
}
