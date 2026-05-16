package works.makertechno.countdown.ui;

import works.makertechno.countdown.i18n.I18n;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;

public class ColorPickerPanel extends JPanel {

    private final I18n i18n;
    private final Consumer<Color> onColorChanged;
    private final ColorWheel wheel;
    private final JSlider rSlider, gSlider, bSlider, aSlider;
    private final JTextField rField, gField, bField, aField, hexField;
    private Color currentColor;
    private boolean updating = false;

    public ColorPickerPanel(I18n i18n, Color initialColor, Consumer<Color> onColorChanged) {
        this.i18n = i18n;
        this.currentColor = initialColor;
        this.onColorChanged = onColorChanged;

        setLayout(new BorderLayout(10, 0));
        setOpaque(false);

        wheel = new ColorWheel();
        wheel.setPreferredSize(new Dimension(220, 220));
        wheel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                updateFromWheel(e);
            }
        });
        wheel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                updateFromWheel(e);
            }
        });

        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.weightx = 1.0;

        rSlider = createSlider(0, 255, initialColor.getRed());
        gSlider = createSlider(0, 255, initialColor.getGreen());
        bSlider = createSlider(0, 255, initialColor.getBlue());
        aSlider = createSlider(0, 255, initialColor.getAlpha());

        rField = createIntField(initialColor.getRed());
        gField = createIntField(initialColor.getGreen());
        bField = createIntField(initialColor.getBlue());
        aField = createIntField(initialColor.getAlpha());

        hexField = new JTextField(colorToHex(initialColor), 9);
        hexField.addActionListener(e -> updateFromHex());
        hexField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateFromHex();
            }
        });

        ChangeListener sliderListener = e -> {
            if (!updating) updateFromSliders();
        };
        rSlider.addChangeListener(sliderListener);
        gSlider.addChangeListener(sliderListener);
        bSlider.addChangeListener(sliderListener);
        aSlider.addChangeListener(sliderListener);

        addSliderRow(leftPanel, gbc, 0, "settings.red", rSlider, rField);
        addSliderRow(leftPanel, gbc, 1, "settings.green", gSlider, gField);
        addSliderRow(leftPanel, gbc, 2, "settings.blue", bSlider, bField);
        addSliderRow(leftPanel, gbc, 3, "settings.alpha", aSlider, aField);

        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        leftPanel.add(new JLabel(i18n.get("settings.hex") + ":"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        leftPanel.add(hexField, gbc);

        add(leftPanel, BorderLayout.CENTER);
        add(wheel, BorderLayout.EAST);

        wheel.setColor(initialColor);
    }

    private static String colorToHex(Color c) {
        return String.format("#%02X%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private void addSliderRow(JPanel panel, GridBagConstraints gbc, int row, String labelKey,
                              JSlider slider, JTextField field) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(new JLabel(i18n.get(labelKey) + ":"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0;
        field.setPreferredSize(new Dimension(50, 22));
        panel.add(field, gbc);
        gbc.gridx = 2;
        gbc.weightx = 1.0;
        gbc.gridwidth = 1;
        panel.add(slider, gbc);
    }

    private JSlider createSlider(int min, int max, int value) {
        JSlider s = new JSlider(min, max, value);
        s.setOpaque(false);
        return s;
    }

    private JTextField createIntField(int value) {
        JTextField f = new JTextField(String.valueOf(value), 4);
        f.addActionListener(e -> updateFromFields());
        f.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateFromFields();
            }
        });
        return f;
    }

    private void updateFromWheel(MouseEvent e) {
        Color c = wheel.getColorAt(e.getX(), e.getY());
        if (c != null) {
            currentColor = new Color(c.getRed(), c.getGreen(), c.getBlue(), currentColor.getAlpha());
            syncAllFromColor();
            onColorChanged.accept(currentColor);
        }
    }

    private void updateFromSliders() {
        if (updating) return;
        currentColor = new Color(rSlider.getValue(), gSlider.getValue(), bSlider.getValue(), aSlider.getValue());
        syncAllFromColor();
        onColorChanged.accept(currentColor);
    }

    private void updateFromFields() {
        if (updating) return;
        try {
            int r = clamp(Integer.parseInt(rField.getText().trim()), 0, 255);
            int g = clamp(Integer.parseInt(gField.getText().trim()), 0, 255);
            int b = clamp(Integer.parseInt(bField.getText().trim()), 0, 255);
            int a = clamp(Integer.parseInt(aField.getText().trim()), 0, 255);
            currentColor = new Color(r, g, b, a);
            syncAllFromColor();
            onColorChanged.accept(currentColor);
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateFromHex() {
        if (updating) return;
        String hex = hexField.getText().trim();
        hex = hex.startsWith("#") ? hex.substring(1) : hex;
        hex = padHex(hex);
        if (hex.length() >= 6 && hex.matches("[0-9a-fA-F]+")) {
            try {
                int r = Integer.parseInt(hex.substring(0, 2), 16);
                int g = Integer.parseInt(hex.substring(2, 4), 16);
                int b = Integer.parseInt(hex.substring(4, 6), 16);
                int a = hex.length() >= 8 ? Integer.parseInt(hex.substring(6, 8), 16) : currentColor.getAlpha();
                currentColor = new Color(r, g, b, a);
                syncAllFromColor();
                onColorChanged.accept(currentColor);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private String padHex(String hex) {
        if (hex.length() < 6) {
            StringBuilder sb = new StringBuilder(hex);
            while (sb.length() < 6) sb.append('0');
            return sb.toString();
        }
        return hex;
    }

    private void syncAllFromColor() {
        updating = true;
        rSlider.setValue(currentColor.getRed());
        gSlider.setValue(currentColor.getGreen());
        bSlider.setValue(currentColor.getBlue());
        aSlider.setValue(currentColor.getAlpha());
        rField.setText(String.valueOf(currentColor.getRed()));
        gField.setText(String.valueOf(currentColor.getGreen()));
        bField.setText(String.valueOf(currentColor.getBlue()));
        aField.setText(String.valueOf(currentColor.getAlpha()));
        hexField.setText(colorToHex(currentColor));
        wheel.setColor(currentColor);
        updating = false;
    }

    public Color getCurrentColor() {
        return currentColor;
    }

    /**
     * HSV 色轮
     */
    private static class ColorWheel extends JPanel {
        private static final int SIZE = 200;
        private float hue = 0, saturation = 0;
        private float brightness = 1.0f;

        ColorWheel() {
            setPreferredSize(new Dimension(SIZE, SIZE));
            setOpaque(false);
        }

        void setColor(Color c) {
            float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
            hue = hsb[0];
            saturation = hsb[1];
            brightness = hsb[2];
            repaint();
        }

        Color getColorAt(int x, int y) {
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            int radius = Math.min(cx, cy) - 4;
            double dx = x - cx;
            double dy = y - cy;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist > radius) return null;
            float h = (float) (Math.atan2(dy, dx) / (2 * Math.PI));
            if (h < 0) h += 1.0f;
            float s = (float) (dist / radius);
            hue = h;
            saturation = Math.min(1.0f, s);
            repaint();
            return Color.getHSBColor(hue, saturation, brightness);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            int radius = Math.min(cx, cy) - 4;

            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist <= radius) {
                        float h = (float) (Math.atan2(dy, dx) / (2 * Math.PI));
                        if (h < 0) h += 1.0f;
                        float s = (float) (dist / radius);
                        g2.setColor(Color.getHSBColor(h, s, brightness));
                        g2.fillRect(cx + dx, cy + dy, 1, 1);
                    }
                }
            }

            int sx = cx + (int) (saturation * radius * Math.cos(hue * 2 * Math.PI));
            int sy = cy + (int) (saturation * radius * Math.sin(hue * 2 * Math.PI));
            g2.setColor(Color.WHITE);
            g2.drawOval(sx - 4, sy - 4, 8, 8);
            g2.setColor(Color.BLACK);
            g2.drawOval(sx - 5, sy - 5, 10, 10);
            g2.dispose();
        }
    }
}
