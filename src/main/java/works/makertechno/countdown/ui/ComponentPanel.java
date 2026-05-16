package works.makertechno.countdown.ui;

import works.makertechno.countdown.component.ComponentManager;
import works.makertechno.countdown.i18n.I18n;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.nio.file.Path;

/**
 * 自定义组件管理面板
 */
public class ComponentPanel extends JPanel {
    
    private final I18n i18n;
    private final ComponentManager componentManager;
    
    private JLabel textRendererStatus;
    private JLabel windowFilterStatus;
    private JButton loadTextRendererBtn;
    private JButton unloadTextRendererBtn;
    private JButton loadWindowFilterBtn;
    private JButton unloadWindowFilterBtn;
    
    public ComponentPanel(I18n i18n, ComponentManager componentManager) {
        this.i18n = i18n;
        this.componentManager = componentManager;
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        
        buildUI();
        updateStatus();
    }
    
    private void buildUI() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setOpaque(false);
        
        // TextRenderer 区域
        mainPanel.add(createTextRendererSection());
        mainPanel.add(Box.createVerticalStrut(15));
        
        // WindowFilter 区域
        mainPanel.add(createWindowFilterSection());
        
        add(mainPanel, BorderLayout.NORTH);
    }
    
    private JPanel createTextRendererSection() {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(BorderFactory.createTitledBorder(i18n.get("component.text_renderer")));
        panel.setOpaque(false);
        
        // 状态显示
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setOpaque(false);
        statusPanel.add(new JLabel(i18n.get("component.status") + ":"));
        textRendererStatus = new JLabel();
        textRendererStatus.setForeground(new Color(100, 200, 100));
        statusPanel.add(textRendererStatus);
        panel.add(statusPanel, BorderLayout.NORTH);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.setOpaque(false);
        
        loadTextRendererBtn = new JButton(i18n.get("component.load"));
        loadTextRendererBtn.addActionListener(e -> loadTextRenderer());
        
        unloadTextRendererBtn = new JButton(i18n.get("component.unload"));
        unloadTextRendererBtn.addActionListener(e -> unloadTextRenderer());
        
        buttonPanel.add(loadTextRendererBtn);
        buttonPanel.add(unloadTextRendererBtn);
        panel.add(buttonPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createWindowFilterSection() {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(BorderFactory.createTitledBorder(i18n.get("component.window_filter")));
        panel.setOpaque(false);
        
        // 状态显示
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setOpaque(false);
        statusPanel.add(new JLabel(i18n.get("component.status") + ":"));
        windowFilterStatus = new JLabel();
        windowFilterStatus.setForeground(new Color(100, 200, 100));
        statusPanel.add(windowFilterStatus);
        panel.add(statusPanel, BorderLayout.NORTH);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.setOpaque(false);
        
        loadWindowFilterBtn = new JButton(i18n.get("component.load"));
        loadWindowFilterBtn.addActionListener(e -> loadWindowFilter());
        
        unloadWindowFilterBtn = new JButton(i18n.get("component.unload"));
        unloadWindowFilterBtn.addActionListener(e -> unloadWindowFilter());
        
        buttonPanel.add(loadWindowFilterBtn);
        buttonPanel.add(unloadWindowFilterBtn);
        panel.add(buttonPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void loadTextRenderer() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("ZIP Files (*.zip)", "zip"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path zipPath = chooser.getSelectedFile().toPath();
            
            // 显示加载进度
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            loadTextRendererBtn.setEnabled(false);
            
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return componentManager.loadTextRenderer(zipPath);
                }
                
                @Override
                protected void done() {
                    try {
                        boolean success = get();
                        if (success) {
                            JOptionPane.showMessageDialog(ComponentPanel.this,
                                    i18n.get("component.load_success"),
                                    i18n.get("component.text_renderer"),
                                    JOptionPane.INFORMATION_MESSAGE);
                            updateStatus();
                        } else {
                            JOptionPane.showMessageDialog(ComponentPanel.this,
                                    i18n.get("component.load_fail"),
                                    i18n.get("component.text_renderer"),
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(ComponentPanel.this,
                                i18n.get("component.load_fail") + "\n" + e.getMessage(),
                                i18n.get("component.text_renderer"),
                                JOptionPane.ERROR_MESSAGE);
                    } finally {
                        setCursor(Cursor.getDefaultCursor());
                        loadTextRendererBtn.setEnabled(true);
                    }
                }
            };
            worker.execute();
        }
    }
    
    private void unloadTextRenderer() {
        int result = JOptionPane.showConfirmDialog(this,
                i18n.get("component.unload_confirm"),
                i18n.get("component.text_renderer"),
                JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            componentManager.unloadTextRenderer();
            updateStatus();
            JOptionPane.showMessageDialog(this,
                    i18n.get("component.unload_success"),
                    i18n.get("component.text_renderer"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void loadWindowFilter() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("ZIP Files (*.zip)", "zip"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path zipPath = chooser.getSelectedFile().toPath();
            
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            loadWindowFilterBtn.setEnabled(false);
            
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return componentManager.loadWindowFilter(zipPath);
                }
                
                @Override
                protected void done() {
                    try {
                        boolean success = get();
                        if (success) {
                            JOptionPane.showMessageDialog(ComponentPanel.this,
                                    i18n.get("component.load_success"),
                                    i18n.get("component.window_filter"),
                                    JOptionPane.INFORMATION_MESSAGE);
                            updateStatus();
                        } else {
                            JOptionPane.showMessageDialog(ComponentPanel.this,
                                    i18n.get("component.load_fail"),
                                    i18n.get("component.window_filter"),
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(ComponentPanel.this,
                                i18n.get("component.load_fail") + "\n" + e.getMessage(),
                                i18n.get("component.window_filter"),
                                JOptionPane.ERROR_MESSAGE);
                    } finally {
                        setCursor(Cursor.getDefaultCursor());
                        loadWindowFilterBtn.setEnabled(true);
                    }
                }
            };
            worker.execute();
        }
    }
    
    private void unloadWindowFilter() {
        int result = JOptionPane.showConfirmDialog(this,
                i18n.get("component.unload_confirm"),
                i18n.get("component.window_filter"),
                JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            componentManager.unloadWindowFilter();
            updateStatus();
            JOptionPane.showMessageDialog(this,
                    i18n.get("component.unload_success"),
                    i18n.get("component.window_filter"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void updateStatus() {
        if (componentManager.hasCustomTextRenderer()) {
            textRendererStatus.setText(componentManager.getTextRendererInfo());
            textRendererStatus.setForeground(new Color(100, 200, 100));
        } else {
            textRendererStatus.setText(i18n.get("component.none"));
            textRendererStatus.setForeground(Color.GRAY);
        }
        
        if (componentManager.hasCustomWindowFilter()) {
            windowFilterStatus.setText(componentManager.getWindowFilterInfo());
            windowFilterStatus.setForeground(new Color(100, 200, 100));
        } else {
            windowFilterStatus.setText(i18n.get("component.none"));
            windowFilterStatus.setForeground(Color.GRAY);
        }
    }
    
    /**
     * 获取当前组件管理器（用于外部获取修改后的状态）
     */
    public ComponentManager getComponentManager() {
        return componentManager;
    }
}