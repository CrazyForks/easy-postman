package com.laker.postman.panel.performance.threadgroup;

import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class ThreadGroupPropertyPanel extends JPanel {
    private final JComboBox<ThreadGroupData.ThreadMode> modeComboBox;
    private final CardLayout cardLayout;
    private final JPanel cardPanel;
    private JMeterTreeNode currentNode;

    // 固定模式面板组件
    private final JPanel fixedPanel;
    private final JSpinner fixedNumThreadsSpinner;
    private final JSpinner fixedLoopsSpinner;
    private final JCheckBox useTimeCheckBox;
    private final JSpinner durationSpinner;

    // 递增模式面板组件
    private final JPanel rampUpPanel;
    private final JSpinner rampUpStartThreadsSpinner;
    private final JSpinner rampUpEndThreadsSpinner;
    private final JSpinner rampUpTimeSpinner;
    private final JSpinner rampUpDurationSpinner;

    // 尖刺模式面板组件
    private final JPanel spikePanel;
    private final JSpinner spikeMinThreadsSpinner;
    private final JSpinner spikeMaxThreadsSpinner;
    private final JSpinner spikeRampUpTimeSpinner;
    private final JSpinner spikeHoldTimeSpinner;
    private final JSpinner spikeRampDownTimeSpinner;
    private final JSpinner spikeDurationSpinner;

    // 阶梯模式面板组件
    private final JPanel stairsPanel;
    private final JSpinner stairsStartThreadsSpinner;
    private final JSpinner stairsEndThreadsSpinner;
    private final JSpinner stairsStepSpinner;
    private final JSpinner stairsHoldTimeSpinner;
    private final JSpinner stairsDurationSpinner;

    // 负载模式预览相关
    private final ThreadLoadPreviewPanel previewPanel;

    public ThreadGroupPropertyPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 顶部模式选择区域
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        topPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.THREADGROUP_MODE_LABEL)));
        modeComboBox = new JComboBox<>(ThreadGroupData.ThreadMode.values());
        modeComboBox.setPreferredSize(new Dimension(150, 28));
        topPanel.add(modeComboBox);

        // 中间部分：左侧配置面板，右侧预览图
        JPanel mainPanel = new JPanel(new BorderLayout(10, 0));

        // 中间卡片布局，用于切换不同模式的配置面板
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        // 动态调整配置面板大小以适应不同语言的标签长度
        int configPanelWidth = I18nUtil.isChinese() ? 380 : 480;  // 英文需要更多空间
        cardPanel.setPreferredSize(new Dimension(configPanelWidth, 150));

        // 初始化所有控件和面板
        // 1. 固定模式面板
        fixedPanel = new JPanel(new GridBagLayout());
        fixedPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        fixedNumThreadsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        fixedNumThreadsSpinner.setPreferredSize(new Dimension(80, 28));
        fixedLoopsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100000, 1));
        fixedLoopsSpinner.setPreferredSize(new Dimension(80, 28));
        useTimeCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.THREADGROUP_FIXED_USE_TIME));
        durationSpinner = new JSpinner(new SpinnerNumberModel(60, 1, 86400, 10));
        durationSpinner.setPreferredSize(new Dimension(80, 28));

        // 2. 递增模式面板
        rampUpPanel = new JPanel(new GridBagLayout());
        rampUpPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        rampUpStartThreadsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        rampUpStartThreadsSpinner.setPreferredSize(new Dimension(80, 28));
        rampUpEndThreadsSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 1000, 1));
        rampUpEndThreadsSpinner.setPreferredSize(new Dimension(80, 28));
        rampUpTimeSpinner = new JSpinner(new SpinnerNumberModel(30, 1, 3600, 5));
        rampUpTimeSpinner.setPreferredSize(new Dimension(80, 28));
        rampUpDurationSpinner = new JSpinner(new SpinnerNumberModel(120, 1, 86400, 10));
        rampUpDurationSpinner.setPreferredSize(new Dimension(80, 28));

        // 3. 尖刺模式面板
        spikePanel = new JPanel(new GridBagLayout());
        spikePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        spikeMinThreadsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        spikeMinThreadsSpinner.setPreferredSize(new Dimension(80, 28));
        spikeMaxThreadsSpinner = new JSpinner(new SpinnerNumberModel(20, 1, 1000, 1));
        spikeMaxThreadsSpinner.setPreferredSize(new Dimension(80, 28));
        spikeRampUpTimeSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 3600, 1));
        spikeRampUpTimeSpinner.setPreferredSize(new Dimension(80, 28));
        spikeHoldTimeSpinner = new JSpinner(new SpinnerNumberModel(5, 0, 3600, 1));
        spikeHoldTimeSpinner.setPreferredSize(new Dimension(80, 28));
        spikeRampDownTimeSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 3600, 1));
        spikeRampDownTimeSpinner.setPreferredSize(new Dimension(80, 28));
        spikeDurationSpinner = new JSpinner(new SpinnerNumberModel(120, 1, 86400, 10));
        spikeDurationSpinner.setPreferredSize(new Dimension(80, 28));

        // 4. 阶梯模式面板
        stairsPanel = new JPanel(new GridBagLayout());
        stairsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        stairsStartThreadsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        stairsStartThreadsSpinner.setPreferredSize(new Dimension(80, 28));
        stairsEndThreadsSpinner = new JSpinner(new SpinnerNumberModel(20, 1, 1000, 1));
        stairsEndThreadsSpinner.setPreferredSize(new Dimension(80, 28));
        stairsStepSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
        stairsStepSpinner.setPreferredSize(new Dimension(80, 28));
        stairsHoldTimeSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 3600, 1));
        stairsHoldTimeSpinner.setPreferredSize(new Dimension(80, 28));
        stairsDurationSpinner = new JSpinner(new SpinnerNumberModel(240, 1, 86400, 10));
        stairsDurationSpinner.setPreferredSize(new Dimension(80, 28));

        // 设置各个面板的布局
        setupFixedPanel();
        setupRampUpPanel();
        setupSpikePanel();
        setupStairsPanel();

        // 添加所有面板到卡片布局
        cardPanel.add(fixedPanel, ThreadGroupData.ThreadMode.FIXED.name());
        cardPanel.add(rampUpPanel, ThreadGroupData.ThreadMode.RAMP_UP.name());
        cardPanel.add(spikePanel, ThreadGroupData.ThreadMode.SPIKE.name());
        cardPanel.add(stairsPanel, ThreadGroupData.ThreadMode.STAIRS.name());

        // 默认显示固定模式面板
        cardLayout.show(cardPanel, ThreadGroupData.ThreadMode.FIXED.name());

        // 模式切换监听器
        modeComboBox.addActionListener(e -> {
            ThreadGroupData.ThreadMode selectedMode = (ThreadGroupData.ThreadMode) modeComboBox.getSelectedItem();
            if (selectedMode != null) {
                cardLayout.show(cardPanel, selectedMode.name());
                updatePreview();
            }
        });

        // 初始设置
        durationSpinner.setEnabled(false); // 默认按循环次数执行

        // 按时间执行时禁用循环次数，按循环次数执行时禁用持续时间
        useTimeCheckBox.addActionListener(e -> {
            boolean useTime = useTimeCheckBox.isSelected();
            fixedLoopsSpinner.setEnabled(!useTime);
            durationSpinner.setEnabled(useTime);
        });

        // 预览图表区域
        previewPanel = new ThreadLoadPreviewPanel();
        // 在英文环境下适当调整预览面板的最小尺寸
        int previewPanelWidth = I18nUtil.isChinese() ? 500 : 450;  // 英文环境给配置面板更多空间
        previewPanel.setPreferredSize(new Dimension(previewPanelWidth, 180));
        previewPanel.setMinimumSize(new Dimension(400, 180));  // 设置最小尺寸防止过度压缩
        previewPanel.setBorder(BorderFactory.createTitledBorder(I18nUtil.getMessage(MessageKeys.THREADGROUP_PREVIEW_TITLE)));

        // 左侧配置区包装在一个面板中，以便控制布局
        JPanel configPanel = new JPanel(new BorderLayout());
        configPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        configPanel.add(cardPanel, BorderLayout.NORTH);
        // 动态调整配置面板宽度以匹配卡片面板
        configPanel.setPreferredSize(new Dimension(configPanelWidth, 180));

        // 添加到主面板
        mainPanel.add(configPanel, BorderLayout.WEST);
        mainPanel.add(previewPanel, BorderLayout.CENTER);

        // 整体布局
        add(topPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);

        // 为所有输入组件添加变化监听，刷新预览图
        addPreviewUpdateListeners();
    }

    // 设置固定模式面板
    private void setupFixedPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // 第一行
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        fixedPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.THREADGROUP_FIXED_USERS), SwingConstants.RIGHT), gbc);

        gbc.gridx = 1;
        fixedPanel.add(fixedNumThreadsSpinner, gbc);

        gbc.gridx = 2;
        fixedPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.THREADGROUP_FIXED_EXECUTION_MODE), SwingConstants.RIGHT), gbc);

        gbc.gridx = 3;
        fixedPanel.add(useTimeCheckBox, gbc);

        // 第二行
        gbc.gridx = 0;
        gbc.gridy = 1;
        fixedPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.THREADGROUP_FIXED_LOOPS), SwingConstants.RIGHT), gbc);

        gbc.gridx = 1;
        fixedPanel.add(fixedLoopsSpinner, gbc);

        gbc.gridx = 2;
        fixedPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.THREADGROUP_FIXED_DURATION), SwingConstants.RIGHT), gbc);

        gbc.gridx = 3;
        fixedPanel.add(durationSpinner, gbc);
    }

    // 设置递增模式面板
    private void setupRampUpPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // 第一行
        gbc.gridx = 0;
        gbc.gridy = 0;
        rampUpPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.THREADGROUP_RAMPUP_START_USERS), SwingConstants.RIGHT), gbc);

        gbc.gridx = 1;
        rampUpPanel.add(rampUpStartThreadsSpinner, gbc);

        gbc.gridx = 2;
        rampUpPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.THREADGROUP_RAMPUP_END_USERS), SwingConstants.RIGHT), gbc);

        gbc.gridx = 3;
        rampUpPanel.add(rampUpEndThreadsSpinner, gbc);

        // 第二行
        gbc.gridx = 0;
        gbc.gridy = 1;
        rampUpPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.THREADGROUP_RAMPUP_RAMP_TIME), SwingConstants.RIGHT), gbc);

        gbc.gridx = 1;
        rampUpPanel.add(rampUpTimeSpinner, gbc);

        gbc.gridx = 2;
        rampUpPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.THREADGROUP_RAMPUP_TEST_DURATION), SwingConstants.RIGHT), gbc);

        gbc.gridx = 3;
        rampUpPanel.add(rampUpDurationSpinner, gbc);
    }

    // 设置尖刺模式面板
    private void setupSpikePanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // 第一行
        gbc.gridx = 0;
        gbc.gridy = 0;
        spikePanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.THREADGROUP_SPIKE_MIN_USERS), SwingConstants.RIGHT), gbc);

        gbc.gridx = 1;
        spikePanel.add(spikeMinThreadsSpinner, gbc);

        gbc.gridx = 2;
        spikePanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.THREADGROUP_SPIKE_MAX_USERS), SwingConstants.RIGHT), gbc);

        gbc.gridx = 3;
        spikePanel.add(spikeMaxThreadsSpinner, gbc);

        // 第二行
        gbc.gridx = 0;
        gbc.gridy = 1;
        spikePanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.THREADGROUP_SPIKE_RAMP_UP_TIME), SwingConstants.RIGHT), gbc);

        gbc.gridx = 1;
        spikePanel.add(spikeRampUpTimeSpinner, gbc);

        gbc.gridx = 2;
        spikePanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.THREADGROUP_SPIKE_HOLD_TIME), SwingConstants.RIGHT), gbc);

        gbc.gridx = 3;
        spikePanel.add(spikeHoldTimeSpinner, gbc);

        // 第三行
        gbc.gridx = 0;
        gbc.gridy = 2;
        spikePanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.THREADGROUP_SPIKE_RAMP_DOWN_TIME), SwingConstants.RIGHT), gbc);

        gbc.gridx = 1;
        spikePanel.add(spikeRampDownTimeSpinner, gbc);

        gbc.gridx = 2;
        spikePanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.THREADGROUP_SPIKE_TEST_DURATION), SwingConstants.RIGHT), gbc);

        gbc.gridx = 3;
        spikePanel.add(spikeDurationSpinner, gbc);
    }

    // 设置阶梯模式面板
    private void setupStairsPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // 第一行
        gbc.gridx = 0;
        gbc.gridy = 0;
        stairsPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.THREADGROUP_STAIRS_START_USERS), SwingConstants.RIGHT), gbc);

        gbc.gridx = 1;
        stairsPanel.add(stairsStartThreadsSpinner, gbc);

        gbc.gridx = 2;
        stairsPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.THREADGROUP_STAIRS_END_USERS), SwingConstants.RIGHT), gbc);

        gbc.gridx = 3;
        stairsPanel.add(stairsEndThreadsSpinner, gbc);

        // 第二行
        gbc.gridx = 0;
        gbc.gridy = 1;
        stairsPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.THREADGROUP_STAIRS_STEP_SIZE), SwingConstants.RIGHT), gbc);

        gbc.gridx = 1;
        stairsPanel.add(stairsStepSpinner, gbc);

        gbc.gridx = 2;
        stairsPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.THREADGROUP_STAIRS_HOLD_TIME), SwingConstants.RIGHT), gbc);

        gbc.gridx = 3;
        stairsPanel.add(stairsHoldTimeSpinner, gbc);

        // 第三行
        gbc.gridx = 0;
        gbc.gridy = 2;
        stairsPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.THREADGROUP_STAIRS_TEST_DURATION), SwingConstants.RIGHT), gbc);

        gbc.gridx = 1;
        stairsPanel.add(stairsDurationSpinner, gbc);
    }

    private void addPreviewUpdateListeners() {
        // 模式选择变化监听
        modeComboBox.addActionListener(e -> updatePreview());

        // 固定模式参数变化监听
        fixedNumThreadsSpinner.addChangeListener(e -> updatePreview());
        fixedLoopsSpinner.addChangeListener(e -> updatePreview());
        useTimeCheckBox.addActionListener(e -> updatePreview());
        durationSpinner.addChangeListener(e -> updatePreview());

        // 递增模式参数变化监听
        rampUpStartThreadsSpinner.addChangeListener(e -> updatePreview());
        rampUpEndThreadsSpinner.addChangeListener(e -> updatePreview());
        rampUpTimeSpinner.addChangeListener(e -> updatePreview());
        rampUpDurationSpinner.addChangeListener(e -> updatePreview());

        // 尖刺模式参数变化监听
        spikeMinThreadsSpinner.addChangeListener(e -> updatePreview());
        spikeMaxThreadsSpinner.addChangeListener(e -> updatePreview());
        spikeRampUpTimeSpinner.addChangeListener(e -> updatePreview());
        spikeHoldTimeSpinner.addChangeListener(e -> updatePreview());
        spikeRampDownTimeSpinner.addChangeListener(e -> updatePreview());
        spikeDurationSpinner.addChangeListener(e -> updatePreview());

        // 阶梯模式参数变化监听
        stairsStartThreadsSpinner.addChangeListener(e -> updatePreview());
        stairsEndThreadsSpinner.addChangeListener(e -> updatePreview());
        stairsStepSpinner.addChangeListener(e -> updatePreview());
        stairsHoldTimeSpinner.addChangeListener(e -> updatePreview());
        stairsDurationSpinner.addChangeListener(e -> updatePreview());
    }

    private void updatePreview() {
        ThreadGroupData.ThreadMode mode = (ThreadGroupData.ThreadMode) modeComboBox.getSelectedItem();
        if (mode == null) return;

        ThreadLoadPreviewData previewData = new ThreadLoadPreviewData();
        previewData.mode = mode;

        switch (mode) {
            case FIXED:
                previewData.fixedThreads = (Integer) fixedNumThreadsSpinner.getValue();
                previewData.useTime = useTimeCheckBox.isSelected();
                previewData.duration = (Integer) durationSpinner.getValue();
                previewData.loops = (Integer) fixedLoopsSpinner.getValue();
                break;

            case RAMP_UP:
                previewData.rampUpStartThreads = (Integer) rampUpStartThreadsSpinner.getValue();
                previewData.rampUpEndThreads = (Integer) rampUpEndThreadsSpinner.getValue();
                previewData.rampUpTime = (Integer) rampUpTimeSpinner.getValue();
                previewData.rampUpDuration = (Integer) rampUpDurationSpinner.getValue();
                break;

            case SPIKE:
                previewData.spikeMinThreads = (Integer) spikeMinThreadsSpinner.getValue();
                previewData.spikeMaxThreads = (Integer) spikeMaxThreadsSpinner.getValue();
                previewData.spikeRampUpTime = (Integer) spikeRampUpTimeSpinner.getValue();
                previewData.spikeHoldTime = (Integer) spikeHoldTimeSpinner.getValue();
                previewData.spikeRampDownTime = (Integer) spikeRampDownTimeSpinner.getValue();
                previewData.spikeDuration = (Integer) spikeDurationSpinner.getValue();
                break;

            case STAIRS:
                previewData.stairsStartThreads = (Integer) stairsStartThreadsSpinner.getValue();
                previewData.stairsEndThreads = (Integer) stairsEndThreadsSpinner.getValue();
                previewData.stairsStep = (Integer) stairsStepSpinner.getValue();
                previewData.stairsHoldTime = (Integer) stairsHoldTimeSpinner.getValue();
                previewData.stairsDuration = (Integer) stairsDurationSpinner.getValue();
                break;
        }

        previewPanel.setPreviewData(previewData);
        previewPanel.repaint();
    }

    // 回填数据
    public void setThreadGroupData(JMeterTreeNode node) {
        this.currentNode = node;
        ThreadGroupData data = node.threadGroupData;
        if (data == null) {
            data = new ThreadGroupData();
            node.threadGroupData = data;
        }

        // 设置模式
        modeComboBox.setSelectedItem(data.threadMode);
        cardLayout.show(cardPanel, data.threadMode.name());

        // 设置固定模式参数
        fixedNumThreadsSpinner.setValue(data.numThreads);
        fixedLoopsSpinner.setValue(data.loops);
        useTimeCheckBox.setSelected(data.useTime);
        durationSpinner.setValue(data.duration);

        // 更新UI状态
        fixedLoopsSpinner.setEnabled(!data.useTime);
        durationSpinner.setEnabled(data.useTime);

        // 设置递增模式参数
        rampUpStartThreadsSpinner.setValue(data.rampUpStartThreads);
        rampUpEndThreadsSpinner.setValue(data.rampUpEndThreads);
        rampUpTimeSpinner.setValue(data.rampUpTime);
        rampUpDurationSpinner.setValue(data.rampUpDuration);

        // 设置尖刺模式参数
        spikeMinThreadsSpinner.setValue(data.spikeMinThreads);
        spikeMaxThreadsSpinner.setValue(data.spikeMaxThreads);
        spikeRampUpTimeSpinner.setValue(data.spikeRampUpTime);
        spikeHoldTimeSpinner.setValue(data.spikeHoldTime);
        spikeRampDownTimeSpinner.setValue(data.spikeRampDownTime);
        spikeDurationSpinner.setValue(data.spikeDuration);

        // 设置阶梯模式参数
        stairsStartThreadsSpinner.setValue(data.stairsStartThreads);
        stairsEndThreadsSpinner.setValue(data.stairsEndThreads);
        stairsStepSpinner.setValue(data.stairsStep);
        stairsHoldTimeSpinner.setValue(data.stairsHoldTime);
        stairsDurationSpinner.setValue(data.stairsDuration);

        // 更新预览图
        updatePreview();
    }

    public void saveThreadGroupData() {
        if (currentNode == null) return;
        ThreadGroupData data = currentNode.threadGroupData;
        if (data == null) {
            data = new ThreadGroupData();
            currentNode.threadGroupData = data;
        }

        // 保存模式
        data.threadMode = (ThreadGroupData.ThreadMode) modeComboBox.getSelectedItem();

        // 保存固定模式参数
        data.numThreads = (Integer) fixedNumThreadsSpinner.getValue();
        data.loops = (Integer) fixedLoopsSpinner.getValue();
        data.useTime = useTimeCheckBox.isSelected();
        data.duration = (Integer) durationSpinner.getValue();

        // 保存递增模式参数
        data.rampUpStartThreads = (Integer) rampUpStartThreadsSpinner.getValue();
        data.rampUpEndThreads = (Integer) rampUpEndThreadsSpinner.getValue();
        data.rampUpTime = (Integer) rampUpTimeSpinner.getValue();
        data.rampUpDuration = (Integer) rampUpDurationSpinner.getValue();

        // 保存尖刺模式参数
        data.spikeMinThreads = (Integer) spikeMinThreadsSpinner.getValue();
        data.spikeMaxThreads = (Integer) spikeMaxThreadsSpinner.getValue();
        data.spikeRampUpTime = (Integer) spikeRampUpTimeSpinner.getValue();
        data.spikeHoldTime = (Integer) spikeHoldTimeSpinner.getValue();
        data.spikeRampDownTime = (Integer) spikeRampDownTimeSpinner.getValue();
        data.spikeDuration = (Integer) spikeDurationSpinner.getValue();

        // 保存阶梯模式参数
        data.stairsStartThreads = (Integer) stairsStartThreadsSpinner.getValue();
        data.stairsEndThreads = (Integer) stairsEndThreadsSpinner.getValue();
        data.stairsStep = (Integer) stairsStepSpinner.getValue();
        data.stairsHoldTime = (Integer) stairsHoldTimeSpinner.getValue();
        data.stairsDuration = (Integer) stairsDurationSpinner.getValue();
    }

    // 预览数据模型
    private static class ThreadLoadPreviewData {
        ThreadGroupData.ThreadMode mode;
        // 固定模式
        int fixedThreads;
        boolean useTime;
        int duration;
        int loops;
        // 递增模式
        int rampUpStartThreads;
        int rampUpEndThreads;
        int rampUpTime;
        int rampUpDuration;
        // 尖刺模式
        int spikeMinThreads;
        int spikeMaxThreads;
        int spikeRampUpTime;
        int spikeHoldTime;
        int spikeRampDownTime;
        int spikeDuration;
        // 阶梯模式
        int stairsStartThreads;
        int stairsEndThreads;
        int stairsStep;
        int stairsHoldTime;
        int stairsDuration;
    }

    // 预览面板实现
    private static class ThreadLoadPreviewPanel extends JPanel {
        private ThreadLoadPreviewData previewData;
        private static final Color GRID_COLOR = new Color(220, 220, 220);
        private static final Color CURVE_COLOR = new Color(41, 121, 255);
        private static final Color AXIS_COLOR = new Color(100, 100, 100);
        private static final Color TEXT_COLOR = new Color(80, 80, 80);
        private static final int PADDING = 40;

        public ThreadLoadPreviewPanel() {
            setBackground(Color.WHITE);
        }

        public void setPreviewData(ThreadLoadPreviewData data) {
            this.previewData = data;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (previewData == null) return;

            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth() - 2 * PADDING;
            int height = getHeight() - 2 * PADDING;

            // 绘制网格和坐标轴
            drawGrid(g2d, width, height);

            // 根据不同模式绘制曲线
            drawCurve(g2d, width, height);

            g2d.dispose();
        }

        /**
         * 生成优美刻度（nice ticks），如1-2-5-10-20等
         * @param min 最小值
         * @param max 最大值
         * @param maxTicks 最多刻度数
         * @return 刻度数组
         */
        private static java.util.List<Integer> getNiceTicks(int min, int max, int maxTicks) {
            java.util.List<Integer> ticks = new java.util.ArrayList<>();
            if (max <= min || maxTicks <= 0) return ticks;
            int range = max - min;
            if (range == 0) {
                ticks.add(min);
                return ticks;
            }
            // 计算优美间隔
            double rawStep = (double) range / maxTicks;
            double mag = Math.pow(10, Math.floor(Math.log10(rawStep)));
            double[] niceSteps = {1, 2, 5, 10};
            double niceStep = niceSteps[0] * mag;
            for (double s : niceSteps) {
                if (rawStep <= s * mag) {
                    niceStep = s * mag;
                    break;
                }
            }
            int niceMin = (int) (Math.floor(min / niceStep) * niceStep);
            int niceMax = (int) (Math.ceil(max / niceStep) * niceStep);
            for (int v = niceMin; v <= niceMax; v += niceStep) {
                if (v >= min && v <= max) {
                    ticks.add(v);
                }
            }
            if (!ticks.contains(max)) ticks.add(max);
            return ticks;
        }

        private void drawGrid(Graphics2D g2d, int width, int height) {
            g2d.setColor(GRID_COLOR);
            g2d.setStroke(new BasicStroke(0.5f));

            // Y轴优美刻度
            int maxThreads = getMaxThreads();
            java.util.List<Integer> yTicks = getNiceTicks(0, maxThreads, 5);
            for (int i = 0; i < yTicks.size(); i++) {
                int threadValue = yTicks.get(i);
                int y = PADDING + height - (int) ((double) threadValue / maxThreads * height);
                g2d.draw(new java.awt.geom.Line2D.Double(PADDING, y, PADDING + width, y));
                if (threadValue > 0) {
                    g2d.setColor(TEXT_COLOR);
                    g2d.drawString(String.valueOf(threadValue), PADDING - 30, y + 5);
                    g2d.setColor(GRID_COLOR);
                }
            }

            // X轴优美刻度
            int duration = getDuration();
            java.util.List<Integer> xTicks = getNiceTicks(0, duration, 10);
            for (int i = 0; i < xTicks.size(); i++) {
                int timeValue = xTicks.get(i);
                int x = PADDING + (int) ((double) timeValue / duration * width);
                g2d.draw(new java.awt.geom.Line2D.Double(x, PADDING, x, PADDING + height));
                if (timeValue > 0) {
                    g2d.setColor(TEXT_COLOR);
                    g2d.drawString(timeValue + "s", x - 10, PADDING + height + 15);
                    g2d.setColor(GRID_COLOR);
                }
            }

            // 坐标轴
            g2d.setColor(AXIS_COLOR);
            g2d.setStroke(new BasicStroke(1.5f));

            // X轴
            g2d.draw(new Line2D.Double(PADDING, PADDING + height, PADDING + width, PADDING + height));
            // Y轴
            g2d.draw(new Line2D.Double(PADDING, PADDING, PADDING, PADDING + height));

            // 坐标轴标签
            g2d.setColor(TEXT_COLOR);
            g2d.drawString(I18nUtil.getMessage(MessageKeys.THREADGROUP_PREVIEW_TIME_SECONDS), PADDING + width / 2 - 20, PADDING + height + 30);

            // 在左上角添加模式信息
            g2d.drawString(I18nUtil.getMessage(MessageKeys.THREADGROUP_PREVIEW_MODE_PREFIX) + " " + previewData.mode.getDisplayName(), PADDING, PADDING - 10);
        }

        private int getMaxThreads() {

            // 根据当前模式计算实际使用的最大线程数
            int actualMax;
            switch (previewData.mode) {
                case FIXED:
                    actualMax = previewData.fixedThreads;
                    break;
                case RAMP_UP:
                    actualMax = Math.max(previewData.rampUpStartThreads, previewData.rampUpEndThreads);
                    break;
                case SPIKE:
                    actualMax = previewData.spikeMaxThreads;
                    break;
                case STAIRS:
                    actualMax = previewData.stairsEndThreads;
                    break;
                default:
                    actualMax = 20; // 默认值
            }
            // 增加30% 的安全余量
            return (int) (actualMax * 1.3);
        }

        private int getDuration() {
            // 根据当前模式返回持续时间（用于X轴刻度）
            switch (previewData.mode) {
                case FIXED:
                    return previewData.useTime ? previewData.duration : 60;
                case RAMP_UP:
                    return previewData.rampUpDuration;
                case SPIKE:
                    return previewData.spikeDuration;
                case STAIRS:
                    return previewData.stairsDuration;
                default:
                    return 60;
            }
        }

        private void drawCurve(Graphics2D g2d, int width, int height) {
            g2d.setColor(CURVE_COLOR);
            g2d.setStroke(new BasicStroke(2.0f));

            List<Point> points = new ArrayList<>();

            switch (previewData.mode) {
                case FIXED:
                    drawFixedCurve(points, width, height);
                    break;
                case RAMP_UP:
                    drawRampUpCurve(points, width, height);
                    break;
                case SPIKE:
                    drawSpikeCurve(points, width, height);
                    break;
                case STAIRS:
                    drawStairsCurve(points, width, height);
                    break;
            }

            // 绘制线段
            if (points.size() >= 2) {
                for (int i = 0; i < points.size() - 1; i++) {
                    Point p1 = points.get(i);
                    Point p2 = points.get(i + 1);
                    g2d.draw(new Line2D.Double(p1.x, p1.y, p2.x, p2.y));
                }
            }

            // 绘制点
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(1.0f));
            for (Point p : points) {
                g2d.fill(new Rectangle2D.Double((double)p.x - 3, (double)p.y - 3, 6, 6));
                g2d.setColor(CURVE_COLOR);
                g2d.draw(new Rectangle2D.Double((double)p.x - 3, (double)p.y - 3, 6, 6));
                g2d.setColor(Color.WHITE);
            }
        }

        private void drawFixedCurve(List<Point> points, int width, int height) {
            int maxThreads = getMaxThreads();

            int x1 = PADDING;
            int y2 = PADDING + height - height * previewData.fixedThreads / maxThreads;
            if (y2 < PADDING) y2 = PADDING + 5;
            points.add(new Point(x1, y2));

            x1 = PADDING + width;
            points.add(new Point(x1, y2));
        }

        private void drawRampUpCurve(List<Point> points, int width, int height) {
            int maxThreads = getMaxThreads();
            int duration = getDuration();

            int x1 = PADDING;
            int y2 = PADDING + height - height * previewData.rampUpStartThreads / maxThreads;
            if (y2 < PADDING) y2 = PADDING + 5;
            points.add(new Point(x1, y2));

            int rampUpEndX = PADDING + (width * previewData.rampUpTime / duration);
            int y3 = PADDING + height - height * previewData.rampUpEndThreads / maxThreads;
            if (y3 < PADDING) y3 = PADDING + 5;
            points.add(new Point(rampUpEndX, y3));

            points.add(new Point(PADDING + width, y3));
        }

        private void drawSpikeCurve(List<Point> points, int width, int height) {
            int maxThreads = getMaxThreads();
            int duration = getDuration();

            int x = PADDING;
            int yMin = PADDING + height - height * previewData.spikeMinThreads / maxThreads;
            if (yMin < PADDING) yMin = PADDING + 5;
            int yMax = PADDING + height - height * previewData.spikeMaxThreads / maxThreads;
            if (yMax < PADDING) yMax = PADDING + 5;


            // 最小线程
            points.add(new Point(x, yMin));

            // 计算时间比例
            int totalPhaseTime = previewData.spikeRampUpTime + previewData.spikeHoldTime + previewData.spikeRampDownTime;
            int availWidth = width;

            // 上升
            x = PADDING + (availWidth * previewData.spikeRampUpTime / duration);
            points.add(new Point(x, yMax));

            // 保持
            x = PADDING + (availWidth * (previewData.spikeRampUpTime + previewData.spikeHoldTime) / duration);
            points.add(new Point(x, yMax));

            // 下降
            x = PADDING + (availWidth * (previewData.spikeRampUpTime + previewData.spikeHoldTime + previewData.spikeRampDownTime) / duration);
            points.add(new Point(x, yMin));

            points.add(new Point(PADDING + width, yMin));
        }

        private void drawStairsCurve(List<Point> points, int width, int height) {
            int maxThreads = getMaxThreads();
            int duration = previewData.stairsDuration; // 总持续时间

            int x = PADDING;
            int startThreads = previewData.stairsStartThreads;
            int endThreads = previewData.stairsEndThreads;
            int step = previewData.stairsStep;
            int holdTime = previewData.stairsHoldTime;

            int currentThreads = startThreads;
            int time = 0;

            // 计算y坐标
            int y = PADDING + height - height * currentThreads / maxThreads;
            if (y < PADDING) y = PADDING + 5;
            points.add(new Point(x, y));

            while (time < duration && currentThreads < endThreads) {
                // 计算下一个阶梯的时间
                int nextTime = time + holdTime;
                if (nextTime > duration) nextTime = duration;

                // 计算x坐标
                int nextX = PADDING + (width * nextTime / duration);
                // 水平线：用户数不变，x推进
                points.add(new Point(nextX, y));

                // 竖线：用户数递增
                currentThreads += step;
                if (currentThreads > endThreads) currentThreads = endThreads;
                int nextY = PADDING + height - height * currentThreads / maxThreads;
                if (nextY < PADDING) nextY = PADDING + 5;
                points.add(new Point(nextX, nextY));

                // 推进时间
                time = nextTime;
                y = nextY;
                x = nextX;
            }

            // 如果最后一个点未到duration，补一段水平线到结束
            int endX = PADDING + width;
            if (x < endX) {
                points.add(new Point(endX, y));
            }
        }
    }
}
