package com.laker.postman.common.component;

import com.formdev.flatlaf.extras.components.FlatTextField;
import com.laker.postman.model.VariableSegment;
import com.laker.postman.util.EasyPostManFontUtil;
import com.laker.postman.util.EasyPostmanVariableUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

/**
 * 1.支持 Postman 风格变量高亮和悬浮提示的文本输入框
 * 变量格式：{{varName}}
 * 已定义的变量显示为蓝色背景，未定义的变量显示为红色背景
 * 鼠标悬浮在变量上时显示变量值的提示
 * 2.支持win和mac下的撤回重做快捷键
 * win：Ctrl+Z 撤回，Ctrl+Y 重做
 * mac：Cmd+Z 撤回，Cmd+Shift+Z 重做
 * 3.支持变量自动补全功能
 * 输入 {{ 时自动弹出变量列表，包括环境变量和内置函数
 */
@Slf4j
public class EasyPostmanTextField extends FlatTextField {
    // Postman 风格颜色
    private static final Color DEFINED_VAR_BG = new Color(180, 210, 255, 120);
    private static final Color DEFINED_VAR_BORDER = new Color(80, 150, 255);
    private static final Color UNDEFINED_VAR_BG = new Color(255, 200, 200, 120);
    private static final Color UNDEFINED_VAR_BORDER = new Color(255, 100, 100);

    // 自动补全UI颜色
    private static final Color BUILTIN_FUNCTION_COLOR = new Color(156, 39, 176); // 紫色 - 内置函数
    private static final Color ENV_VARIABLE_COLOR = new Color(46, 125, 50); // 绿色 - 环境变量
    private static final Color POPUP_BACKGROUND = new Color(255, 255, 255);
    private static final Color POPUP_SELECTION_BG = new Color(232, 242, 252);

    private final UndoManager undoManager = new UndoManager();

    // 自动补全相关
    private JWindow autocompleteWindow;
    private JList<String> autocompleteList;
    private DefaultListModel<String> autocompleteModel;
    private Map<String, String> currentVariables;
    private int autocompleteStartPos = -1;

    public EasyPostmanTextField(int columns) {
        super();
        setColumns(columns);
        // 启用 ToolTip 支持，必须设置（即使内容为空）
        setToolTipText("");
        initUndoRedo();
        initAutocomplete();
    }

    public EasyPostmanTextField(String text, int columns) {
        super();
        setText(text);
        setColumns(columns);
        // 启用 ToolTip 支持，必须设置（即使内容为空）
        setToolTipText("");
        initUndoRedo();
        initAutocomplete();
    }

    public EasyPostmanTextField(String text, int columns, String placeholderText) {
        super();
        setText(text);
        setColumns(columns);
        setPlaceholderText(placeholderText);
        // 启用 ToolTip 支持，必须设置（即使内容为空）
        setToolTipText("");
        initUndoRedo();
        initAutocomplete();
    }

    @Override
    protected void paintComponent(Graphics g) {
        // 先绘制文本、光标、选区
        super.paintComponent(g);

        String value = getText();
        List<VariableSegment> segments = EasyPostmanVariableUtil.getVariableSegments(value);
        if (segments.isEmpty()) return;

        try {
            // 添加null检查，防止modelToView2D返回null
            var view = modelToView2D(0);
            if (view == null) {
                return; // 如果view为null，直接返回不绘制变量高亮
            }

            Rectangle startRect = view.getBounds();
            FontMetrics fm = getFontMetrics(getFont());
            int x = startRect.x;
            int baseY = startRect.y;
            int h = fm.getHeight();
            int last = 0;

            for (VariableSegment seg : segments) {
                // 计算变量前的文本宽度
                if (seg.start > last) {
                    String before = value.substring(last, seg.start);
                    int w = fm.stringWidth(before);
                    x += w;
                }
                // 判断变量状态：环境变量、临时变量或内置函数
                boolean isDefined = EasyPostmanVariableUtil.isVariableDefined(seg.name)
                        || EasyPostmanVariableUtil.isBuiltInFunction(seg.name);
                Color bgColor = isDefined ? DEFINED_VAR_BG : UNDEFINED_VAR_BG;
                Color borderColor = isDefined ? DEFINED_VAR_BORDER : UNDEFINED_VAR_BORDER;
                String varText = value.substring(seg.start, seg.end);
                int varWidth = fm.stringWidth(varText);
                // 只绘制变量的半透明背景和边框，不绘制文本
                g.setColor(bgColor);
                g.fillRoundRect(x, baseY, varWidth, h, 8, 8);
                g.setColor(borderColor);
                g.drawRoundRect(x, baseY, varWidth, h, 8, 8);
                x += varWidth;
                last = seg.end;
            }
        } catch (Exception e) {
            log.error("paintComponent", e);
        }
    }

    /**
     * 初始化撤回/重做功能
     */
    private void initUndoRedo() {
        getDocument().addUndoableEditListener(undoManager);

        // Undo
        getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");
        getInputMap().put(KeyStroke.getKeyStroke("meta Z"), "Undo"); // macOS Cmd+Z
        getActionMap().put("Undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (undoManager.canUndo()) undoManager.undo();
                } catch (CannotUndoException ignored) {
                }
            }
        });

        // Redo
        getInputMap().put(KeyStroke.getKeyStroke("control Y"), "Redo");
        getInputMap().put(KeyStroke.getKeyStroke("meta shift Z"), "Redo"); // macOS Cmd+Shift+Z
        getActionMap().put("Redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (undoManager.canRedo()) undoManager.redo();
                } catch (CannotRedoException ignored) {
                }
            }
        });
    }

    /**
     * 初始化自动补全功能
     */
    private void initAutocomplete() {
        // 创建弹出窗口（使用JWindow替代JPopupMenu以获得更好的控制）
        autocompleteWindow = new JWindow(SwingUtilities.getWindowAncestor(this));
        autocompleteWindow.setFocusableWindowState(false);

        autocompleteModel = new DefaultListModel<>();
        autocompleteList = new JList<>(autocompleteModel);
        autocompleteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        autocompleteList.setVisibleRowCount(10);
        autocompleteList.setBackground(POPUP_BACKGROUND);
        autocompleteList.setSelectionBackground(POPUP_SELECTION_BG);
        autocompleteList.setSelectionForeground(Color.BLACK);
        autocompleteList.setFont(getFont());
        // 固定列表宽度，防止内容过长导致横向滚动
        autocompleteList.setFixedCellWidth(384); // 400 - 边框和内边距

        // 自定义列表渲染器 - 显示图标、变量名和值/描述，支持长文本截断
        autocompleteList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                JPanel panel = new JPanel(new BorderLayout(8, 0));
                panel.setOpaque(true);
                panel.setBorder(new EmptyBorder(4, 8, 4, 8));
                // 设置固定大小，防止横向滚动
                panel.setPreferredSize(new Dimension(384, 32));
                panel.setMaximumSize(new Dimension(384, 32));

                if (isSelected) {
                    panel.setBackground(POPUP_SELECTION_BG);
                } else {
                    panel.setBackground(POPUP_BACKGROUND);
                }

                if (value != null && currentVariables != null) {
                    String varName = value.toString();
                    String varValue = currentVariables.get(varName);

                    // 判断是内置函数还是环境变量
                    boolean isBuiltIn = varName.startsWith("$");
                    Color labelColor = isBuiltIn ? BUILTIN_FUNCTION_COLOR : ENV_VARIABLE_COLOR;

                    // 使用彩色圆点代替 Emoji（更好的跨平台兼容性）
                    JPanel iconPanel = new JPanel() {
                        @Override
                        protected void paintComponent(Graphics g) {
                            super.paintComponent(g);
                            Graphics2D g2d = (Graphics2D) g.create();
                            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                            // 获取字体度量信息以实现垂直对齐
                            int panelHeight = getHeight();

                            // 计算圆点垂直居中位置
                            int circleSize = 12;
                            int circleY = (panelHeight - circleSize) / 2;

                            // 绘制圆形图标
                            g2d.setColor(isBuiltIn ? BUILTIN_FUNCTION_COLOR : ENV_VARIABLE_COLOR);
                            g2d.fillOval(2, circleY, circleSize, circleSize);

                            // 绘制白色符号 - 垂直居中对齐
                            g2d.setColor(Color.WHITE);
                            g2d.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 10));
                            FontMetrics symbolFm = g2d.getFontMetrics();
                            String symbol = isBuiltIn ? "$" : "E";
                            int symbolWidth = symbolFm.stringWidth(symbol);
                            int symbolAscent = symbolFm.getAscent();
                            int symbolDescent = symbolFm.getDescent();
                            int symbolHeight = symbolAscent + symbolDescent;

                            // 符号在圆点内垂直居中
                            int symbolX = 2 + (circleSize - symbolWidth) / 2;
                            int symbolY = circleY + (circleSize - symbolHeight) / 2 + symbolAscent;
                            g2d.drawString(symbol, symbolX, symbolY);

                            g2d.dispose();
                        }

                        @Override
                        public Dimension getPreferredSize() {
                            return new Dimension(16, 24);
                        }
                    };
                    iconPanel.setOpaque(false);
                    panel.add(iconPanel, BorderLayout.WEST);

                    // 中间区域：使用固定宽度的面板容纳name和value，让它们各占一半空间
                    JPanel contentPanel = new JPanel(new GridLayout(1, 2, 8, 0));
                    contentPanel.setOpaque(false);

                    // 左侧：变量名（占一半空间）
                    JPanel namePanel = new JPanel(new BorderLayout());
                    namePanel.setOpaque(false);

                    String displayName = varName;
                    String fullName = varName;

                    // 变量名最大长度（约占一半宽度）
                    int maxNameLength = 25;
                    if (varName.length() > maxNameLength) {
                        displayName = varName.substring(0, maxNameLength - 3) + "...";
                    }

                    JLabel nameLabel = new JLabel(displayName);
                    nameLabel.setFont(getFont().deriveFont(Font.BOLD));
                    nameLabel.setForeground(labelColor);

                    // 为变量名添加工具提示
                    if (!displayName.equals(fullName)) {
                        nameLabel.setToolTipText(fullName);
                    }

                    namePanel.add(nameLabel, BorderLayout.WEST);
                    contentPanel.add(namePanel);

                    // 右侧：变量值或描述（占一半空间）
                    JPanel valuePanel = new JPanel(new BorderLayout());
                    valuePanel.setOpaque(false);

                    if (varValue != null && !varValue.isEmpty()) {
                        String displayValue = varValue;
                        String fullValue = varValue;

                        // 变量值最大长度（约占一半宽度）
                        int maxValueLength = 25;
                        if (varValue.length() > maxValueLength) {
                            displayValue = varValue.substring(0, maxValueLength - 3) + "...";
                        }

                        JLabel valueLabel = new JLabel(displayValue);
                        valueLabel.setFont(getFont().deriveFont(Font.PLAIN, getFont().getSize() - 1));
                        valueLabel.setForeground(Color.GRAY);

                        // 为值添加工具提示（显示完整内容）
                        if (!displayValue.equals(fullValue) || fullValue.length() > 20) {
                            // 格式化工具提示，支持换行
                            String tooltipText = formatTooltipText(fullValue, 60);
                            valueLabel.setToolTipText("<html>" + tooltipText + "</html>");
                        }

                        valuePanel.add(valueLabel, BorderLayout.WEST);
                    }
                    contentPanel.add(valuePanel);

                    panel.add(contentPanel, BorderLayout.CENTER);

                    // 为整个面板添加工具提示（显示完整信息）
                    StringBuilder tooltipBuilder = new StringBuilder("<html>");
                    tooltipBuilder.append("<b>").append(escapeHtml(varName)).append("</b>");
                    if (varValue != null && !varValue.isEmpty()) {
                        tooltipBuilder.append("<br/>").append(escapeHtml(varValue));
                    }
                    tooltipBuilder.append("</html>");
                    panel.setToolTipText(tooltipBuilder.toString());
                }

                return panel;
            }
        });

        JScrollPane scrollPane = new JScrollPane(autocompleteList);
        // 禁用横向滚动条，只保留纵向滚动
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180), 1),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        autocompleteWindow.add(scrollPane);

        // 监听文档变化
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> checkForAutocomplete());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> checkForAutocomplete());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> checkForAutocomplete());
            }
        });

        // 列表鼠标点击事件
        autocompleteList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 || e.getClickCount() == 1) {
                    insertSelectedVariable();
                }
            }
        });

        // 文本框键盘事件
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (autocompleteWindow.isVisible()) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_DOWN:
                            int nextIndex = autocompleteList.getSelectedIndex() + 1;
                            if (nextIndex < autocompleteModel.getSize()) {
                                autocompleteList.setSelectedIndex(nextIndex);
                                autocompleteList.ensureIndexIsVisible(nextIndex);
                            }
                            e.consume();
                            break;
                        case KeyEvent.VK_UP:
                            int prevIndex = autocompleteList.getSelectedIndex() - 1;
                            if (prevIndex >= 0) {
                                autocompleteList.setSelectedIndex(prevIndex);
                                autocompleteList.ensureIndexIsVisible(prevIndex);
                            }
                            e.consume();
                            break;
                        case KeyEvent.VK_ENTER:
                        case KeyEvent.VK_TAB:
                            insertSelectedVariable();
                            e.consume();
                            break;
                        case KeyEvent.VK_ESCAPE:
                            hideAutocomplete();
                            e.consume();
                            break;
                        default:
                            break;
                    }
                }
            }
        });

        // 失去焦点时隐藏
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                // 延迟隐藏，允许点击列表项
                SwingUtilities.invokeLater(() -> {
                    if (!autocompleteList.hasFocus()) {
                        hideAutocomplete();
                    }
                });
            }
        });
    }

    /**
     * 格式化工具提示文本，自动换行
     */
    private String formatTooltipText(String text, int maxLineLength) {
        if (text == null || text.length() <= maxLineLength) {
            return escapeHtml(text);
        }

        StringBuilder formatted = new StringBuilder();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxLineLength, text.length());
            // 尝试在空格处断行
            if (end < text.length()) {
                int lastSpace = text.lastIndexOf(' ', end);
                if (lastSpace > start && lastSpace - start > maxLineLength / 2) {
                    end = lastSpace;
                }
            }
            formatted.append(escapeHtml(text.substring(start, end)));
            if (end < text.length()) {
                formatted.append("<br/>");
            }
            start = end + 1;
        }
        return formatted.toString();
    }

    /**
     * HTML转义，防止特殊字符破坏工具提示
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    /**
     * 检查是否需要显示自动补全
     */
    private void checkForAutocomplete() {
        String text = getText();
        int caretPos = getCaretPosition();

        if (text == null || caretPos < 2) {
            hideAutocomplete();
            return;
        }

        // 查找光标前最近的 {{
        int openBracePos = text.lastIndexOf("{{", caretPos - 1);
        if (openBracePos == -1) {
            hideAutocomplete();
            return;
        }

        // 检查 {{ 之后是否有 }}
        int closeBracePos = text.indexOf("}}", openBracePos);
        if (closeBracePos != -1 && closeBracePos < caretPos) {
            hideAutocomplete();
            return;
        }

        // 获取 {{ 之后的文本作为过滤前缀
        String prefix = text.substring(openBracePos + 2, caretPos);

        // 过滤变量列表
        currentVariables = EasyPostmanVariableUtil.filterVariables(prefix);

        if (currentVariables.isEmpty()) {
            hideAutocomplete();
            return;
        }

        // 更新列表
        autocompleteStartPos = openBracePos + 2;
        autocompleteModel.clear();
        for (String varName : currentVariables.keySet()) {
            autocompleteModel.addElement(varName);
        }

        // 默认选中第一项
        if (autocompleteModel.getSize() > 0) {
            autocompleteList.setSelectedIndex(0);
            autocompleteList.ensureIndexIsVisible(0);
        }

        // 显示弹出菜单
        showAutocomplete();
    }

    /**
     * 显示自动补全弹出窗口
     */
    private void showAutocomplete() {
        if (autocompleteModel.getSize() == 0) {
            return;
        }

        try {
            // 获取光标位置
            Rectangle rect = modelToView2D(getCaretPosition()).getBounds();
            Point screenPos = getLocationOnScreen();

            // 计算弹出窗口大小 - 固定宽度为400
            int itemHeight = 32; // 每项高度
            int popupWidth = 400;
            int popupHeight = Math.min(autocompleteModel.getSize() * itemHeight + 10, 320);

            // 设置窗口大小和位置
            autocompleteWindow.setSize(popupWidth, popupHeight);
            autocompleteWindow.setLocation(
                    screenPos.x + rect.x,
                    screenPos.y + rect.y + rect.height + 2
            );

            if (!autocompleteWindow.isVisible()) {
                autocompleteWindow.setVisible(true);
            }
        } catch (Exception e) {
            log.error("showAutocomplete", e);
        }
    }

    /**
     * 隐藏自动补全弹出窗口
     */
    private void hideAutocomplete() {
        if (autocompleteWindow != null && autocompleteWindow.isVisible()) {
            autocompleteWindow.setVisible(false);
        }
        autocompleteStartPos = -1;
    }

    /**
     * 插入选中的变量
     */
    private void insertSelectedVariable() {
        String selected = autocompleteList.getSelectedValue();
        if (selected == null || autocompleteStartPos == -1) {
            return;
        }

        try {
            String text = getText();
            int caretPos = getCaretPosition();

            // 找到 {{ 的位置
            int openBracePos = text.lastIndexOf("{{", caretPos - 1);
            if (openBracePos == -1) {
                return;
            }

            // 替换 {{ 到光标位置的文本为 {{selected}}
            String before = text.substring(0, openBracePos);
            String after = text.substring(caretPos);
            String newText = before + "{{" + selected + "}}" + after;

            setText(newText);
            setCaretPosition(openBracePos + selected.length() + 4); // 光标放在 }} 之后

            hideAutocomplete();
        } catch (Exception e) {
            log.error("insertSelectedVariable", e);
        }
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        String value = getText();
        List<VariableSegment> segments = EasyPostmanVariableUtil.getVariableSegments(value);
        if (segments.isEmpty()) return super.getToolTipText(event);

        try {
            int mouseX = event.getX();
            var view = modelToView2D(0);
            if (view == null) {
                return super.getToolTipText(event);
            }

            Rectangle startRect = view.getBounds();
            FontMetrics fm = getFontMetrics(getFont());
            int x = startRect.x;
            int last = 0;

            for (VariableSegment seg : segments) {
                if (seg.start > last) {
                    String before = value.substring(last, seg.start);
                    int w = fm.stringWidth(before);
                    x += w;
                }
                String varText = value.substring(seg.start, seg.end);
                int varWidth = fm.stringWidth(varText);
                if (mouseX >= x && mouseX <= x + varWidth) {
                    // 鼠标悬浮在变量上
                    String varName = seg.name;

                    // 检查是否是内置函数
                    if (EasyPostmanVariableUtil.isBuiltInFunction(varName)) {
                        String desc = currentVariables != null ?
                                currentVariables.get(varName) :
                                "Dynamic function generates value at runtime";
                        return buildTooltip(varName, desc, true, true);
                    }

                    // 环境变量
                    String varValue = EasyPostmanVariableUtil.getVariableValue(varName);
                    if (varValue != null) {
                        return buildTooltip(varName, varValue, false, true);
                    } else {
                        return buildTooltip(varName, "Variable not defined in current environment", false, false);
                    }
                }
                x += varWidth;
                last = seg.end;
            }
        } catch (Exception e) {
            log.error("getToolTipText", e);
        }
        return super.getToolTipText(event);
    }

    /**
     * 构建美观的工具提示HTML
     */
    private String buildTooltip(String varName, String content, boolean isBuiltIn, boolean isDefined) {
        StringBuilder tooltip = new StringBuilder();
        tooltip.append("<html><body style='padding: 8px; font-family: Arial, sans-serif;'>");

        // 标题部分 - 变量名
        String titleColor = isBuiltIn ? "#9C27B0" : (isDefined ? "#2E7D32" : "#D32F2F");
        String typeLabel = isBuiltIn ? "Built-in Function" : (isDefined ? "Environment Variable" : "Undefined Variable");
        String typeIcon = isBuiltIn ? "⚡" : (isDefined ? "✓" : "✗");

        tooltip.append("<div style='margin-bottom: 6px;'>");
        tooltip.append("<span style='font-size: 10px; color: ").append(titleColor).append(";'>");
        tooltip.append(typeIcon).append(" ").append(typeLabel);
        tooltip.append("</span></div>");

        // 变量名 - 粗体显示
        tooltip.append("<div style='margin-bottom: 1px;'>");
        tooltip.append("<b style='font-size: 10px; color: ").append(titleColor).append(";'>");
        tooltip.append(escapeHtml(varName));
        tooltip.append("</b></div>");

        // 分隔线
        tooltip.append("<hr style='border: none; border-top: 1px solid #E0E0E0; margin: 1px 0;'/>");

        // 内容部分 - 变量值或描述
        tooltip.append("<div style='margin-top: 1px; color: #424242; font-size: 10px;'>");

        if (isDefined && !isBuiltIn) {
            // 环境变量值
            tooltip.append("<span style='color: #757575;'>Value:</span><br/>");
            tooltip.append("<span style='font-family: Consolas, monospace; background-color: #F5F5F5; ");
            tooltip.append("padding: 4px 6px; border-radius: 3px; display: inline-block; margin-top: 1px;'>");

            // 限制显示长度，超过150字符截断
            String displayContent = content.length() > 150 ? content.substring(0, 150) + "..." : content;
            tooltip.append(escapeHtml(displayContent));
            tooltip.append("</span>");
        } else if (isBuiltIn) {
            // 内置函数描述
            tooltip.append("<span style='color: #757575; font-style: italic;'>");
            tooltip.append(escapeHtml(content));
            tooltip.append("</span>");
        } else {
            // 未定义变量警告
            tooltip.append("<span style='color: #D32F2F; font-weight: bold;'>⚠ ");
            tooltip.append(escapeHtml(content));
            tooltip.append("</span>");
        }

        tooltip.append("</div>");
        tooltip.append("</body></html>");

        return tooltip.toString();
    }
}
