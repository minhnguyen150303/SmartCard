package com.mycompany.membershipcardgui;

import javax.imageio.ImageIO;
import javax.smartcardio.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.text.ParseException;

import com.formdev.flatlaf.FlatLightLaf;

public class MembershipCardGUI extends JFrame {

    // ================== MÀU CHỦ ĐẠO – MODERN PURPLE ==================
    private static final Color PRIMARY_PURPLE = new Color(106, 76, 147);   // Deep Purple
    private static final Color ACCENT_PURPLE  = new Color(142, 104, 190); // Light Purple
    private static final Color DARK_PURPLE    = new Color(75, 46, 131);   // Very Dark Purple
    private static final Color LIGHT_BG       = new Color(248, 246, 252); // Light background
    private static final Color CARD_BG        = Color.WHITE;
    private static final Color SUCCESS_COLOR  = new Color(88, 166, 124);  // Green
    private static final Color WARNING_COLOR  = new Color(230, 126, 34);  // Orange
    private static final Color DANGER_COLOR   = new Color(231, 76, 60);   // Red
    private static final Color TEXT_DARK      = new Color(44, 44, 44);
    private static final Color TEXT_LIGHT     = new Color(128, 128, 128);

    private static final int LOG_ENTRY_SIZE = 16;
    private static final int INS_UNLOCK_CARD = 0x03;
    private static final int INS_CHANGE_PIN_AFTER_UNLOCK = 0x21;

    // ================== BIẾN LOGIC GỐC ==================
    private byte[] fileData;
    private byte[] newAvatarData = null; // avatar mới khi sửa thông tin
    private boolean isConnected = false;
    private boolean isCardBlocked = false;
    private Card card = null;
    private CardChannel channel = null;
    private static int counter = 1;

    private JLabel imageLabel;
    private JTextField filePathField;
    private JTextField getBalanceField;

    private JFrame frame;
    private JPanel apduPanel, infoPanel, memberPanel;
    private JTextField responseField, getMaKH, getName, getDob, getGender, getPoints;
    private JTextField getPhone;
    private JPasswordField pinField;
    private JTextField makhField, nameField, dobField;
    private JComboBox<String> genderComboBox;
    private JButton browseButton;
    private JLabel imageInfoLabel;
    private JLabel statusIndicator;

    // ==== BUTTONS CHÍNH (giữ logic, nhưng text có icon) ====
    private JButton initCardButton      = createModernButton("Khởi tạo thẻ", "🆕");
    private JButton readCardButton      = createModernButton("Đọc dữ liệu thẻ", "📄");
    private JButton changePinButton     = createModernButton("Thay đổi mã PIN", "🔐");
    private JButton editButton          = createModernButton("Sửa Thông Tin", "✏️");
    private JButton topUpButton         = createModernButton("Nạp tiền", "💳");
    private JButton storeButton         = createModernButton("Cửa hàng", "🛒");
    private JButton upgradeTierButton   = createModernButton("Nâng hạng", "⭐");
    private JButton exchangePointsButton= createModernButton("Đổi điểm", "🎁");
    private JButton unblockCartButton   = createModernButton("Mở khoá thẻ", "🔓");
    private JButton verifybtn           = createModernButton("Kiểm tra PIN", "✓");
    private JButton viewLogButton       = createModernButton("Xem lịch sử", "📄");

    // ================== DATA GỐC ==================
    private static class Product {
        String name;
        long price;
        Product(String n, long p) { name = n; price = p; }
    }

    private static class TierPack {
        String name;
        int tier;
        long price;
        TierPack(String n, int t, long p) { name = n; tier = t; price = p; }
    }

    private Product[] products = new Product[]{
            new Product("Áo thun", 100_000L),
            new Product("Quần jean", 500_000L),
            new Product("Thắt lưng", 300_000L),
            new Product("Mũ", 400_000L),
            new Product("Găng tay", 200_000L),
            new Product("Giày sneaker", 1_500_000L)
    };

    private TierPack[] tierPacks = new TierPack[]{
            new TierPack("Bạc (-5%)", 1, 300_000),
            new TierPack("Vàng (-10%)", 2, 700_000),
            new TierPack("Bạch Kim (-15%)", 3, 1_200_000),
            new TierPack("Kim Cương (-20%)", 4, 2_000_000)
    };

    // ================== MAIN ==================
    public static void main(String[] args) {
        FlatLightLaf.setup();
        SwingUtilities.invokeLater(MembershipCardGUI::new);
    }

    // ================== HELPER UI ==================
    private JButton createModernButton(String text, String icon) {

        // HTML giúp Swing render emoji FULL glyph
        String htmlText = "<html>"
                + "<span style='font-family: Segoe UI Emoji; font-size:18px;'>"
                + icon
                + "</span>"
                + "&nbsp;&nbsp;"
                + "<span style='font-family: Segoe UI; font-size:15px;'>"
                + text
                + "</span>"
                + "</html>";

        JButton btn = new JButton(htmlText);

        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(220, 80));
        btn.setForeground(Color.WHITE);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(btn.getBackground().brighter());
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(btn.getBackground().darker());
            }
        });

        return btn;
    }


    private void styleConnectionButton(JButton btn, Color bgColor) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bgColor);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        btn.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(bgColor.brighter()); }
            public void mouseExited(MouseEvent e) { btn.setBackground(bgColor); }
        });
    }

    private void styleFunctionButton(JButton btn, Color bgColor) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bgColor);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        btn.setPreferredSize(new Dimension(0, 70));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(bgColor.brighter()); }
            public void mouseExited(MouseEvent e) { btn.setBackground(bgColor); }
        });
    }

    private void styleSmallActionButton(JButton btn, Color bgColor) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bgColor);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(bgColor.brighter()); }
            public void mouseExited(MouseEvent e) { btn.setBackground(bgColor); }
        });
    }

    private JLabel createLabel(String text) {
        JLabel lb = new JLabel(text);
        lb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lb.setForeground(TEXT_DARK);
        return lb;
    }

    // ================== CONSTRUCTOR – GIAO DIỆN NGOÀI ==================
    public MembershipCardGUI() {
        frame = new JFrame("Hệ Thống Quản Lý Thẻ Thành Viên");
        frame.setSize(1200, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(0, 0));
        frame.getContentPane().setBackground(LIGHT_BG);

        // Header
        JPanel header = createHeaderPanel();
        frame.add(header, BorderLayout.NORTH);

        // Left – kết nối thẻ
        apduPanel = createConnectionPanel();
        frame.add(apduPanel, BorderLayout.WEST);

        // Center – chức năng
        memberPanel = createFunctionsPanel();
        frame.add(memberPanel, BorderLayout.CENTER);

        // Status bar
        JPanel statusBar = createStatusBar();
        frame.add(statusBar, BorderLayout.SOUTH);

        // Gán sự kiện cho các button chức năng
        attachEventListeners();

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY_PURPLE);
        header.setPreferredSize(new Dimension(0, 80));
        header.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));

        JLabel titleLabel = new JLabel("HỆ THỐNG QUẢN LÝ THẺ THÀNH VIÊN");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Quản lý thông tin và giao dịch thẻ thông minh");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(230, 230, 230));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 2));
        titlePanel.setOpaque(false);
        titlePanel.add(titleLabel);
        titlePanel.add(subtitleLabel);

        statusIndicator = new JLabel("● Chưa kết nối");
        statusIndicator.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusIndicator.setForeground(new Color(255, 200, 200));

        header.add(titlePanel, BorderLayout.WEST);
        header.add(statusIndicator, BorderLayout.EAST);
        return header;
    }

    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(320, 0));
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(230, 230, 230)));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(CARD_BG);
        content.setBorder(BorderFactory.createEmptyBorder(25, 20, 25, 20));

        JLabel connectionTitle = new JLabel("KẾT NỐI THẺ");
        connectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        connectionTitle.setForeground(PRIMARY_PURPLE);
        connectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(connectionTitle);
        content.add(Box.createVerticalStrut(20));

        JButton connectButton = new JButton("Kết nối thẻ");
        JButton disconnectButton = new JButton("Ngắt kết nối");

        styleConnectionButton(connectButton, SUCCESS_COLOR);
        styleConnectionButton(disconnectButton, DANGER_COLOR);

        connectButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        disconnectButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(connectButton);
        content.add(Box.createVerticalStrut(12));
        content.add(disconnectButton);
        content.add(Box.createVerticalStrut(25));

        JLabel statusLabel = new JLabel("TRẠNG THÁI");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLabel.setForeground(TEXT_DARK);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(statusLabel);
        content.add(Box.createVerticalStrut(10));

        responseField = new JTextField();
        responseField.setEditable(false);
        responseField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        responseField.setBackground(LIGHT_BG);
        responseField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        responseField.setAlignmentX(Component.LEFT_ALIGNMENT);
        responseField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        content.add(responseField);
        content.add(Box.createVerticalGlue());

        panel.add(content, BorderLayout.CENTER);

        connectButton.addActionListener(e -> connectToCard());
        disconnectButton.addActionListener(e -> disconnectFromCard());

        return panel;
    }

    private JPanel createFunctionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(LIGHT_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JLabel title = new JLabel("CHỨC NĂNG");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(PRIMARY_PURPLE);

        JPanel grid = new JPanel(new GridLayout(4, 3, 15, 15));
        grid.setBackground(LIGHT_BG);

        styleFunctionButton(initCardButton, ACCENT_PURPLE);
        styleFunctionButton(readCardButton, PRIMARY_PURPLE);
        styleFunctionButton(topUpButton, SUCCESS_COLOR);
        styleFunctionButton(storeButton, new Color(52, 152, 219));
        styleFunctionButton(upgradeTierButton, new Color(241, 196, 15));
        styleFunctionButton(exchangePointsButton, new Color(155, 89, 182));
        styleFunctionButton(unblockCartButton, WARNING_COLOR);
        styleFunctionButton(viewLogButton, new Color(41, 128, 185));
        JButton forgotPinButton = createModernButton("Quên mã PIN", "❓");
        styleFunctionButton(forgotPinButton, new Color(52, 152, 219));
        forgotPinButton.addActionListener(e -> forgotPin());

        grid.add(createFunctionCard(initCardButton));
        grid.add(createFunctionCard(readCardButton));
        grid.add(createFunctionCard(topUpButton));
        grid.add(createFunctionCard(storeButton));
        grid.add(createFunctionCard(upgradeTierButton));
        grid.add(createFunctionCard(exchangePointsButton));
        grid.add(createFunctionCard(unblockCartButton));
        grid.add(createFunctionCard(viewLogButton));
        grid.add(createFunctionCard(forgotPinButton));

        panel.add(title, BorderLayout.NORTH);
        panel.add(grid, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createFunctionCard(JButton button) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 2, 2, new Color(230, 230, 230)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        card.add(button, BorderLayout.CENTER);
        return card;
    }

    // ==== CARD CHO SHOP / NÂNG HẠNG (MỖI Ô 1 MÀU) ====

    // bo viền khi chọn / bỏ chọn
    private void setCardSelected(JPanel card, boolean selected) {
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selected ? Color.WHITE : new Color(230, 230, 230),
                        selected ? 3 : 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
    }

    // tạo 1 ô vuông màu cho sản phẩm / gói hạng
    private JPanel createSelectCard(String title, String subtitle, Color bgColor) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(bgColor);
        setCardSelected(card, false); // ban đầu chưa chọn

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setHorizontalAlignment(SwingConstants.LEFT);

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(245, 245, 245));
        lblSub.setHorizontalAlignment(SwingConstants.LEFT);

        card.add(lblTitle, BorderLayout.CENTER);
        card.add(lblSub, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(250, 250, 250));
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));

        JLabel versionLabel = new JLabel("v1.0.0 | Membership Card Management System");
        versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        versionLabel.setForeground(TEXT_LIGHT);

        JLabel copyLabel = new JLabel("© 2025 All Rights Reserved");
        copyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        copyLabel.setForeground(TEXT_LIGHT);

        statusBar.add(versionLabel, BorderLayout.WEST);
        statusBar.add(copyLabel, BorderLayout.EAST);
        return statusBar;
    }

    private void attachEventListeners() {
        initCardButton.addActionListener(e -> {
            try {
                initializeCard();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        readCardButton.addActionListener(e -> readCard());

        editButton.addActionListener(e -> {
            Window window = SwingUtilities.getWindowAncestor(infoPanel);
            if (window != null) window.dispose();
            changeInfo();
        });

        changePinButton.addActionListener(e -> {
            Window window = SwingUtilities.getWindowAncestor(infoPanel);
            if (window != null) window.dispose();
            changePin();
        });

        exchangePointsButton.addActionListener(e -> exchangePoints());
        unblockCartButton.addActionListener(e -> unblockCard());
        verifybtn.addActionListener(e -> verifyPin());
        viewLogButton.addActionListener(e -> viewTransactionLogs());
        topUpButton.addActionListener(e -> topUpMoney());
        storeButton.addActionListener(e -> openStore());
        upgradeTierButton.addActionListener(e -> openUpgradeShop());
    }

    // ================== LOGIC GỐC – KẾT NỐI THẺ ==================
    private void connectToCard() {
        if (!isConnected) {
            try {
                TerminalFactory factory = TerminalFactory.getDefault();
                List<CardTerminal> terminals = factory.terminals().list();
                if (terminals.isEmpty()) {
                    responseField.setText("Không tìm thấy đầu đọc thẻ!");
                    return;
                }

                CardTerminal terminal = terminals.get(0);
                responseField.setText("Đang kết nối...");
                if (terminal.waitForCardPresent(10000)) {
                    card = terminal.connect("*");
                    channel = card.getBasicChannel();
                    isConnected = true;
                    responseField.setText("Kết nối thành công!");
                    statusIndicator.setText("● Đã kết nối");
                    statusIndicator.setForeground(new Color(150, 255, 150));
                    selectApplet(); // auto select AID
                } else {
                    responseField.setText("Không có thẻ trong đầu đọc!");
                }
            } catch (Exception ex) {
                responseField.setText("Lỗi: " + ex.getMessage());
            }
        } else {
            responseField.setText("Đã kết nối trước đó!");
        }
    }

    private void disconnectFromCard() {
        if (isConnected && card != null) {
            try {
                card.disconnect(false);
                isConnected = false;
                responseField.setText("Ngắt kết nối thành công!");
                statusIndicator.setText("● Chưa kết nối");
                statusIndicator.setForeground(new Color(255, 200, 200));
            } catch (Exception ex) {
                responseField.setText("Lỗi khi ngắt kết nối: " + ex.getMessage());
            }
        } else {
            responseField.setText("Chưa có kết nối để ngắt!");
        }
    }

    private void selectApplet() {
        if (!isConnected || channel == null) {
            responseField.setText("Bạn phải kết nối với thẻ trước!");
            return;
        }

        try {
            String aid = "112233445500";
            byte[] aidBytes = hexStringToByteArray(aid);

            CommandAPDU selectCommand = new CommandAPDU(0x00, 0xA4, 0x04, 0x00, aidBytes);
            ResponseAPDU response = channel.transmit(selectCommand);

            int sw1 = response.getSW1();
            int sw2 = response.getSW2();
            if (sw1 == 0x90 && sw2 == 0x00) {
                responseField.setText("Chọn Applet thành công!");
            } else {
                responseField.setText(String.format("Lỗi khi chọn applet! SW: %02X %02X", sw1, sw2));
            }
        } catch (Exception ex) {
            responseField.setText("Lỗi: " + ex.getMessage());
        }
    }

    // ================== VERIFY PIN – UI MODERN ==================
    private boolean verifyPin() {
        try {
            while (true) {
                JPanel pinPanel = new JPanel(new GridBagLayout());
                pinPanel.setBackground(LIGHT_BG);
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(5,5,5,5);
                gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
                pinPanel.add(createLabel("Nhập mã PIN (6 số):"), gbc);

                JPasswordField passwordField = new JPasswordField();
                passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                passwordField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(210,210,210)),
                        BorderFactory.createEmptyBorder(6,6,6,6)
                ));
                gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.weightx = 1.0;
                pinPanel.add(passwordField, gbc);

                int option = JOptionPane.showConfirmDialog(
                        null,
                        pinPanel,
                        "Xác thực mã PIN",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

                if (option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION) {
                    responseField.setText("Bạn đã hủy nhập mã PIN.");
                    return false;
                }

                String pin = new String(passwordField.getPassword()).trim();

                if (!pin.matches("\\d{6}")) {
                    JOptionPane.showMessageDialog(
                            null,
                            "Mã PIN phải gồm đúng 6 chữ số!",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE
                    );
                    continue;
                }

                byte[] pinBytes = pin.getBytes(StandardCharsets.UTF_8);
                CommandAPDU verifyPinCommand = new CommandAPDU(0x00, 0x02, 0x00, 0x00, pinBytes);
                ResponseAPDU verifyResponse = channel.transmit(verifyPinCommand);

                if (verifyResponse.getSW() != 0x9000) {
                    JOptionPane.showMessageDialog(
                            null,
                            "Lỗi từ thẻ! SW=" + Integer.toHexString(verifyResponse.getSW()),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return false;
                }

                byte[] data = verifyResponse.getData();
                if (data.length < 1) {
                    JOptionPane.showMessageDialog(
                            null,
                            "Thẻ trả về dữ liệu không hợp lệ!",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return false;
                }

                byte status = data[0];
                byte remain = (data.length > 1) ? data[1] : 0;

                if (status == (byte) 0x01) {
                    responseField.setText("Xác thực mã PIN thành công!");
                    return true;
                } else if (status == (byte) 0x00) {
                    String msg = "Mã PIN không đúng!";
                    if (remain > 0) {
                        msg += "\nBạn còn " + remain + " lần thử trước khi thẻ bị khóa.";
                    }
                    responseField.setText(msg);
                    JOptionPane.showMessageDialog(null, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
                    continue;
                } else if (status == (byte) 0x02) {
                    responseField.setText("Thẻ đã bị khóa do nhập sai PIN quá nhiều lần.");
                    JOptionPane.showMessageDialog(
                            null,
                            "Thẻ đã bị khóa do nhập sai PIN quá nhiều lần!",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return false;
                }

                JOptionPane.showMessageDialog(null, "Lỗi: trạng thái PIN không xác định (" + status + ")", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (CardException ex) {
            responseField.setText("Lỗi xác thực mã PIN: " + ex.getMessage());
            JOptionPane.showMessageDialog(
                    null,
                    "Lỗi xác thực mã PIN: " + ex.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
    }

    // ================== KHỞI TẠO THẺ – FORM ĐẸP ==================
    private void initializeCard() throws IOException {
        if (!isConnected || channel == null) {
            responseField.setText("Bạn phải kết nối với thẻ trước!");
            JOptionPane.showMessageDialog(null, "Bạn phải kết nối với thẻ trước!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        while (true) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            dateFormat.setLenient(false);
            JPanel addMemberPanel = new JPanel(new BorderLayout(15,15));
            addMemberPanel.setBackground(LIGHT_BG);
            addMemberPanel.setBorder(BorderFactory.createTitledBorder("Khởi tạo thẻ"));

            // ----- PANEL ẢNH (SỬA LỖI CO NHỎ) -----
            JPanel imagePanel = new JPanel();
            imagePanel.setBackground(Color.WHITE);
            imagePanel.setPreferredSize(new Dimension(140, 180));
            imagePanel.setMinimumSize(new Dimension(140, 180));
            imagePanel.setMaximumSize(new Dimension(140, 180));
            imagePanel.setBorder(BorderFactory.createLineBorder(new Color(180,180,180), 1));
            imagePanel.setLayout(new BorderLayout());

// Label hiển thị ảnh
            imageLabel = new JLabel();
            imageLabel.setHorizontalAlignment(JLabel.CENTER);
            imageLabel.setVerticalAlignment(JLabel.CENTER);

            imagePanel.add(imageLabel, BorderLayout.CENTER);

// Nút tải ảnh
            browseButton = new JButton("Tải ảnh lên");
            browseButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            browseButton.setFocusPainted(false);
            browseButton.setBackground(ACCENT_PURPLE);
            browseButton.setForeground(Color.WHITE);
            browseButton.setBorder(BorderFactory.createEmptyBorder(6,12,6,12));

            browseButton.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e){ browseButton.setBackground(ACCENT_PURPLE.brighter()); }
                public void mouseExited(MouseEvent e){ browseButton.setBackground(ACCENT_PURPLE); }
            });

            browseButton.addActionListener(e -> fileData = chooseAndReadFile());

// Panel gói 2 thứ lại
            JPanel leftPanel = new JPanel();
            leftPanel.setBackground(LIGHT_BG);
            leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

            leftPanel.add(Box.createVerticalStrut(10));
            leftPanel.add(imagePanel);
            leftPanel.add(Box.createVerticalStrut(10));
            leftPanel.add(browseButton);
            leftPanel.add(Box.createVerticalGlue());

            // Panel phải – thông tin
            JPanel rightPanel = new JPanel(new GridBagLayout());
            rightPanel.setBackground(LIGHT_BG);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5,5,5,5);
            gbc.anchor = GridBagConstraints.WEST;

            int row = 0;

            // PIN
            gbc.gridx = 0; gbc.gridy = row;
            rightPanel.add(createLabel("Pin:"), gbc);
            pinField = new JPasswordField();
            pinField.setPreferredSize(new Dimension(200, 25));
            pinField.setBorder(BorderFactory.createLineBorder(new Color(210,210,210)));
            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx=1.0;
            rightPanel.add(pinField, gbc);
            row++;

            // Mã KH (auto)
            gbc.gridx = 0; gbc.gridy = row; gbc.fill = 0; gbc.weightx=0;
            rightPanel.add(createLabel("Mã KH:"), gbc);
            String autoMaKH = "CT" + counter;
            makhField = new JTextField(autoMaKH);
            makhField.setEditable(false);
            makhField.setBorder(BorderFactory.createLineBorder(new Color(210,210,210)));
            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx=1.0;
            rightPanel.add(makhField, gbc);
            row++;

            // Họ tên
            gbc.gridx = 0; gbc.gridy = row; gbc.weightx=0; gbc.fill=0;
            rightPanel.add(createLabel("Họ và Tên:"), gbc);
            nameField = new JTextField();
            nameField.setBorder(BorderFactory.createLineBorder(new Color(210,210,210)));
            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx=1.0;
            rightPanel.add(nameField, gbc);
            row++;

            // Ngày sinh (CHỌN NGÀY)
            gbc.gridx = 0; gbc.gridy = row; gbc.weightx=0; gbc.fill=0;
            rightPanel.add(createLabel("Ngày Sinh:"), gbc);

            // Spinner chọn ngày
            SpinnerDateModel birthModel = new SpinnerDateModel();
            birthModel.setEnd(new Date());
            JSpinner birthSpinner = new JSpinner(birthModel);
            JSpinner.DateEditor birthEditor = new JSpinner.DateEditor(birthSpinner, "dd/MM/yyyy");
            birthSpinner.setEditor(birthEditor);

            // style cho giống input
            JComponent editor = birthSpinner.getEditor();
            if (editor instanceof JSpinner.DefaultEditor) {
                JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
                tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                tf.setBorder(BorderFactory.createLineBorder(new Color(210,210,210)));
            }

            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx=1.0;
            rightPanel.add(birthSpinner, gbc);
            row++;

            // Giới tính
            gbc.gridx = 0; gbc.gridy = row; gbc.weightx=0; gbc.fill=0;
            rightPanel.add(createLabel("Giới Tính:"), gbc);
            genderComboBox = new JComboBox<>(new String[]{"Nam","Nữ"});
            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx=1.0;
            rightPanel.add(genderComboBox, gbc);
            row++;

            // Số điện thoại
            gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.fill = 0;
            rightPanel.add(createLabel("Số điện thoại:"), gbc);
            JTextField phoneField = new JTextField();
            phoneField.setBorder(BorderFactory.createLineBorder(new Color(210,210,210)));
            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
            rightPanel.add(phoneField, gbc);
            row++;

            addMemberPanel.add(leftPanel, BorderLayout.LINE_START);
            addMemberPanel.add(rightPanel, BorderLayout.CENTER);

            int option = JOptionPane.showConfirmDialog(
                    null,
                    addMemberPanel,
                    "Thêm thành viên",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION) {
                responseField.setText("Đã hủy thao tác khởi tạo thẻ.");
                return;
            }

            try {
                String maKH = makhField.getText().trim();
                String name = nameField.getText().trim();
                Date birthDate = (Date) birthSpinner.getValue();
                String dob = dateFormat.format(birthDate);
                String gender = (String) genderComboBox.getSelectedItem();
                String pin = new String(pinField.getPassword()).trim();
                String phone = phoneField.getText().trim();

                // ===== VALIDATE HỌ TÊN =====
                if (!name.matches("^[A-Za-zÀ-ỹ\\s]+$")) {
                    JOptionPane.showMessageDialog(
                            null,
                            "Họ và tên chỉ được chứa chữ cái (không số, không ký tự đặc biệt)!",
                            "Lỗi dữ liệu",
                            JOptionPane.ERROR_MESSAGE
                    );
                    continue;
                }

                // ===== VALIDATE SỐ ĐIỆN THOẠI VIỆT NAM =====
                if (!phone.matches("^0\\d{9}$")) {
                    JOptionPane.showMessageDialog(
                            null,
                            "Số điện thoại phải đúng 10 chữ số và bắt đầu bằng số 0!",
                            "Lỗi dữ liệu",
                            JOptionPane.ERROR_MESSAGE
                    );
                    continue;
                }

                if (!pin.matches("\\d{6}")) {
                    JOptionPane.showMessageDialog(null,
                            "Mã PIN phải là 6 chữ số!",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    continue;
                }

                if (name.isEmpty() || dob.isEmpty() || pin.isEmpty()|| phone.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Vui lòng điền đầy đủ thông tin!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    continue;
                }

                String data = String.join("|", pin, maKH, name, dob, gender, phone);
                byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);

                if (dataBytes.length > 255) {
                    JOptionPane.showMessageDialog(null, "Dữ liệu quá lớn để lưu vào thẻ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    continue;
                }

                CommandAPDU writeCommand = new CommandAPDU(0x00, 0x01, 0x00, 0x00, dataBytes);
                ResponseAPDU response = channel.transmit(writeCommand);

                int sw1 = response.getSW1();
                int sw2 = response.getSW2();
                if (sw1 == 0x90 && sw2 == 0x00) {
                    if (fileData != null) {
                        sendImageData(fileData);
                    }
                    responseField.setText("Khởi tạo thẻ thành công!");
                    JOptionPane.showMessageDialog(null, "Khởi tạo thẻ thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);

                    counter++;
                    return;
                } else {
                    String errorMessage = String.format("Lỗi khi ghi dữ liệu! SW: %02X %02X", sw1, sw2);
                    JOptionPane.showMessageDialog(null, errorMessage, "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (CardException ex) {
                JOptionPane.showMessageDialog(null, "Lỗi thẻ: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Lỗi không xác định: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ================== ĐỌC THẺ ==================
    private void readCard() {
        if (!isConnected || channel == null) {
            responseField.setText("Bạn phải kết nối với thẻ trước!");
            return;
        }

        if (!verifyPin()) {
            return;
        } else {
            readCardData();
        }
    }

    private void readCardData() {
        infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("Thông tin thẻ"));
        infoPanel.setBackground(LIGHT_BG);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);

        imageInfoLabel = new JLabel();
        imageInfoLabel.setPreferredSize(new Dimension(100, 150));
        imageInfoLabel.setBorder(BorderFactory.createLineBorder(new Color(200,200,200)));

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        infoPanel.add(imageInfoLabel, gbc);

        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;

        int row = 1;

        // Mã KH
        gbc.gridx = 0; gbc.gridy = row;
        infoPanel.add(createLabel("Mã KH:"), gbc);
        getMaKH = new JTextField(); getMaKH.setEditable(false);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx=1.0;
        infoPanel.add(getMaKH, gbc);
        row++;

        // Họ tên
        gbc.gridx = 0; gbc.gridy = row;
        infoPanel.add(createLabel("Họ và Tên:"), gbc);
        getName = new JTextField(); getName.setEditable(false);
        gbc.gridx = 1;
        infoPanel.add(getName, gbc);
        row++;

        // Ngày sinh
        gbc.gridx = 0; gbc.gridy = row;
        infoPanel.add(createLabel("Ngày Sinh (dd/MM/yyyy):"), gbc);
        getDob = new JTextField(); getDob.setEditable(false);
        gbc.gridx = 1;
        infoPanel.add(getDob, gbc);
        row++;

        // Giới tính
        gbc.gridx = 0; gbc.gridy = row;
        infoPanel.add(createLabel("Giới Tính:"), gbc);
        getGender = new JTextField(); getGender.setEditable(false);
        gbc.gridx = 1;
        infoPanel.add(getGender, gbc);
        row++;

        // Số điện thoại - NEW
        gbc.gridx = 0; gbc.gridy = row;
        infoPanel.add(createLabel("Số điện thoại:"), gbc);
        getPhone = new JTextField(); getPhone.setEditable(false);
        gbc.gridx = 1;
        infoPanel.add(getPhone, gbc);
        row++;

        // Số dư
        gbc.gridx = 0; gbc.gridy = row;
        infoPanel.add(createLabel("Số dư (VNĐ):"), gbc);
        getBalanceField = new JTextField(); getBalanceField.setEditable(false);
        gbc.gridx = 1;
        infoPanel.add(getBalanceField, gbc);
        row++;

        // Điểm
        gbc.gridx = 0; gbc.gridy = row;
        infoPanel.add(createLabel("Tích điểm:"), gbc);
        getPoints = new JTextField(); getPoints.setEditable(false);
        gbc.gridx = 1;
        infoPanel.add(getPoints, gbc);
        row++;

        // Hạng
        gbc.gridx = 0; gbc.gridy = row;
        infoPanel.add(createLabel("Hạng thành viên:"), gbc);
        JTextField tierField = new JTextField(); tierField.setEditable(false);
        gbc.gridx = 1;
        infoPanel.add(tierField, gbc);
        row++;

        // Thời hạn hạng
        gbc.gridx = 0; gbc.gridy = row;
        infoPanel.add(createLabel("Thời hạn hạng còn lại:"), gbc);
        JTextField expireField = new JTextField(); expireField.setEditable(false);
        gbc.gridx = 1;
        infoPanel.add(expireField, gbc);
        row++;

        // Nút
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        btnPanel.setBackground(LIGHT_BG);
        styleSmallActionButton(changePinButton, DANGER_COLOR);
        styleSmallActionButton(editButton, WARNING_COLOR);
        btnPanel.add(changePinButton);
        btnPanel.add(editButton);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        infoPanel.add(btnPanel, gbc);

        try {
            CommandAPDU readCommand = new CommandAPDU(0x00, 0x06, 0x00, 0x00);
            ResponseAPDU response = channel.transmit(readCommand);

            if (response.getSW() == 0x9000) {

                byte[] data = response.getData();
                int realLen = data.length;
                while (realLen > 0 && data[realLen - 1] == 0x00) realLen--;

                String rawData = new String(data, 0, realLen, StandardCharsets.UTF_8);
                String[] fields = rawData.split("\\|");

                // MUST HAVE 6 FIELDS
                if (fields.length >= 6) {

                    getMaKH.setText(fields[0]);   // maKH
                    getName.setText(fields[1]);   // hoten
                    getDob.setText(fields[2]);    // ngaysinh
                    getGender.setText(fields[3]); // gioitinh
                    getPhone.setText(fields[4]);  // sdt
                    getPoints.setText(fields[5]); // sodu điểm (đổi tên nhưng đúng dữ liệu applet)

                    // Số dư tiền (tách API riêng)
                    long balance = getBalanceFromCard();
                    getBalanceField.setText(formatMoneyNoSign(balance) + " VNĐ");

                    // ===== TIER =====
                    CommandAPDU getTierCmd = new CommandAPDU(0x00, 0x14, 0x00, 0x00);
                    ResponseAPDU tierResp = channel.transmit(getTierCmd);

                    byte tierValue = tierResp.getData()[0];
                    String tierName = switch (tierValue) {
                        case 0 -> "Basic";
                        case 1 -> "Silver";
                        case 2 -> "Gold";
                        case 3 -> "Platinum";
                        case 4 -> "Diamond";
                        default -> "Unknown";
                    };
                    tierField.setText(tierName);

                    // ===== EXPIRE =====
                    CommandAPDU getExpireCmd = new CommandAPDU(0x00, 0x1B, 0x00, 0x00);
                    ResponseAPDU expireResp = channel.transmit(getExpireCmd);

                    long expireTime = 0;
                    if (expireResp.getData().length == 4) {
                        byte[] exp = expireResp.getData();
                        expireTime =
                                ((exp[0] & 0xFFL) << 24) |
                                        ((exp[1] & 0xFFL) << 16) |
                                        ((exp[2] & 0xFFL) << 8)  |
                                        (exp[3] & 0xFFL);
                    }

                    long nowSec = System.currentTimeMillis() / 1000;
                    long remainSec = expireTime - nowSec;
                    String remainText;

                    if (expireTime == 0 || tierValue == 0) {
                        remainText = "Không giới hạn / Chưa mua gói";
                    } else if (remainSec <= 0) {
                        remainText = "ĐÃ HẾT HẠN";
                    } else {
                        long days = remainSec / (24 * 3600);
                        remainText = (days <= 0) ? "< 1 ngày" : days + " ngày";
                    }
                    expireField.setText(remainText);

                    // ===== HÌNH ẢNH =====
                    getImageFile(imageInfoLabel);

                    responseField.setText("Đọc dữ liệu thẻ thành công!");
                    JOptionPane.showConfirmDialog(
                            null, infoPanel,
                            "Thông tin thẻ",
                            JOptionPane.CLOSED_OPTION,
                            JOptionPane.PLAIN_MESSAGE
                    );
                } else {
                    responseField.setText("Dữ liệu không đầy đủ hoặc sai định dạng!");
                }

            } else {
                responseField.setText("Lỗi từ thẻ: SW=" + Integer.toHexString(response.getSW()));
            }

        } catch (Exception ex) {
            responseField.setText("Lỗi đọc thẻ: " + ex.getMessage());
        }
    }


    // ================== ĐỔI PIN ==================
    private void changePin() {
        if (!isConnected || channel == null) {
            responseField.setText("Bạn phải kết nối với thẻ trước!");
            JOptionPane.showMessageDialog(null, "Bạn phải kết nối với thẻ trước!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        while (true) {
            JPanel pinPanel = new JPanel(new GridBagLayout());
            pinPanel.setBackground(LIGHT_BG);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5,5,5,5);
            gbc.anchor = GridBagConstraints.WEST;

            JPasswordField oldPinField = new JPasswordField();
            JPasswordField newPinField = new JPasswordField();
            JPasswordField confirmPinField = new JPasswordField();

            int row = 0;
            gbc.gridx=0; gbc.gridy=row; pinPanel.add(createLabel("Mã PIN cũ:"), gbc);
            gbc.gridx=1; gbc.fill=GridBagConstraints.HORIZONTAL; gbc.weightx=1.0;
            pinPanel.add(oldPinField, gbc); row++;

            gbc.gridx=0; gbc.gridy=row; gbc.fill=0; gbc.weightx=0;
            pinPanel.add(createLabel("Mã PIN mới:"), gbc);
            gbc.gridx=1; gbc.fill=GridBagConstraints.HORIZONTAL; gbc.weightx=1.0;
            pinPanel.add(newPinField, gbc); row++;

            gbc.gridx=0; gbc.gridy=row; gbc.fill=0; gbc.weightx=0;
            pinPanel.add(createLabel("Xác nhận mã PIN mới:"), gbc);
            gbc.gridx=1; gbc.fill=GridBagConstraints.HORIZONTAL; gbc.weightx=1.0;
            pinPanel.add(confirmPinField, gbc);

            int option = JOptionPane.showConfirmDialog(null, pinPanel, "Thay đổi mã PIN", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION) {
                responseField.setText("Hủy thao tác thay đổi mã PIN.");
                return;
            }

            String oldPin = new String(oldPinField.getPassword()).trim();
            String newPin = new String(newPinField.getPassword()).trim();
            String confirmPin = new String(confirmPinField.getPassword()).trim();

            if (!oldPin.matches("\\d{6}")) {
                JOptionPane.showMessageDialog(null, "Mã PIN cũ phải là 6 chữ số!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                continue;
            }
            if (!newPin.matches("\\d{6}")) {
                JOptionPane.showMessageDialog(null, "Mã PIN mới phải là 6 chữ số!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                continue;
            }
            if (!newPin.equals(confirmPin)) {
                JOptionPane.showMessageDialog(null, "Mã PIN mới và xác nhận không trùng khớp.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                continue;
            }

            try {
                String changePinData = String.join("|", oldPin, newPin);
                byte[] dataBytes = changePinData.getBytes(StandardCharsets.UTF_8);

                CommandAPDU changePinCommand = new CommandAPDU(0x00, 0x04, 0x00, 0x00, dataBytes);
                ResponseAPDU response = channel.transmit(changePinCommand);

                if (response.getSW1() == 0x90 && response.getSW2() == 0x00) {
                    responseField.setText("Mã PIN đã được thay đổi thành công.");
                    JOptionPane.showMessageDialog(null, "Mã PIN đã được thay đổi thành công.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    return;
                } else {
                    String errorMessage = String.format("Lỗi khi thay đổi mã PIN. SW: %04X", response.getSW());
                    JOptionPane.showMessageDialog(null, errorMessage, "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Lỗi khi thay đổi mã PIN: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ================== ĐỔI THÔNG TIN ==================
    private void changeInfo() {
        if (!isConnected || channel == null) {
            responseField.setText("Bạn phải kết nối với thẻ trước!");
            JOptionPane.showMessageDialog(null, "Bạn phải kết nối với thẻ trước!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!verifyPin()) {
            JOptionPane.showMessageDialog(null, "Xác thực mã PIN không thành công.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        while (true) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            dateFormat.setLenient(false);
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBackground(LIGHT_BG);

            // ===== AVATAR UI (SỬA THÔNG TIN) =====
            JPanel avatarBox = new JPanel(new BorderLayout());
            avatarBox.setBackground(Color.WHITE);
            avatarBox.setPreferredSize(new Dimension(120, 160));
            avatarBox.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

            JLabel avatarPreview = new JLabel();
            avatarPreview.setHorizontalAlignment(SwingConstants.CENTER);
            avatarPreview.setVerticalAlignment(SwingConstants.CENTER);
            avatarBox.add(avatarPreview, BorderLayout.CENTER);

            // Load avatar hiện tại từ thẻ lên preview
            getImageFile(avatarPreview);

            JButton changeAvatarBtn = new JButton("Đổi avatar");
            changeAvatarBtn.setFocusPainted(false);
            changeAvatarBtn.setBackground(ACCENT_PURPLE);
            changeAvatarBtn.setForeground(Color.WHITE);

            changeAvatarBtn.addActionListener(e -> {
                byte[] picked = chooseAndReadFile(avatarPreview);
                if (picked != null) {
                    newAvatarData = picked; // lưu lại để lát nữa bấm OK thì gửi xuống thẻ
                }
            });


            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5,5,5,5);
            gbc.anchor = GridBagConstraints.WEST;

            int row = 0;

            // Avatar preview (chiếm 2 cột)
            gbc.gridx = 0; gbc.gridy = row;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            panel.add(avatarBox, gbc);
            row++;

            gbc.gridx = 0; gbc.gridy = row;
            gbc.gridwidth = 2;
            panel.add(changeAvatarBtn, gbc);
            row++;

            // reset về layout bình thường
            gbc.gridwidth = 1;
            gbc.anchor = GridBagConstraints.WEST;

            // Họ tên
            gbc.gridx = 0; gbc.gridy = row;
            panel.add(createLabel("Họ và Tên:"), gbc);
            JTextField nameFieldNew = new JTextField(getName.getText());
            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
            panel.add(nameFieldNew, gbc);
            row++;

            // Ngày sinh (CHỌN NGÀY)
            gbc.gridx = 0; gbc.gridy = row; gbc.fill = 0; gbc.weightx = 0;
            panel.add(createLabel("Ngày Sinh:"), gbc);

            // Spinner chọn ngày
            SpinnerDateModel birthModel = new SpinnerDateModel();
            birthModel.setEnd(new Date());
            JSpinner birthSpinner = new JSpinner(birthModel);
            JSpinner.DateEditor birthEditor = new JSpinner.DateEditor(birthSpinner, "dd/MM/yyyy");
            birthSpinner.setEditor(birthEditor);

            // set ngày cũ từ thẻ
            String oldDob = getDob.getText().trim();
            try {
                Date oldDate = dateFormat.parse(oldDob);
                birthSpinner.setValue(oldDate);
            } catch (ParseException e) {
                birthSpinner.setValue(new Date());
            }

            // style giống input
            JComponent editor = birthSpinner.getEditor();
            if (editor instanceof JSpinner.DefaultEditor) {
                JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
                tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                tf.setBorder(BorderFactory.createLineBorder(new Color(210,210,210)));
            }

            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
            panel.add(birthSpinner, gbc);
            row++;

            // Số Điện Thoại
            gbc.gridx = 0; gbc.gridy = row;
            panel.add(createLabel("Số Điện Thoại:"), gbc);
            JTextField phoneFieldNew = new JTextField(getPhone.getText());
            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(phoneFieldNew, gbc);
            row++;

            // Giới tính
            gbc.gridx = 0; gbc.gridy = row;
            panel.add(createLabel("Giới Tính:"), gbc);
            JComboBox<String> genderComboBoxNew = new JComboBox<>(new String[]{"Nam", "Nữ"});
            genderComboBoxNew.setSelectedItem(getGender.getText());
            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(genderComboBoxNew, gbc);

            // Show popup
            int option = JOptionPane.showConfirmDialog(
                    null, panel, "Thay đổi thông tin",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
            );

            if (option != JOptionPane.OK_OPTION) {
                responseField.setText("Hủy thao tác thay đổi thông tin.");
                return;
            }

            // Validate
            String name = nameFieldNew.getText().trim();
            Date birthDate = (Date) birthSpinner.getValue();
            String dob = dateFormat.format(birthDate);
            String phone = phoneFieldNew.getText().trim();
            String gender = (String) genderComboBoxNew.getSelectedItem();

            if (!name.matches("^[A-Za-zÀ-ỹ\\s]+$")) {
                JOptionPane.showMessageDialog(
                        null,
                        "Họ và tên chỉ được chứa chữ cái!",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE
                );
                continue;
            }

            if (!phone.matches("^0\\d{9}$")) {
                JOptionPane.showMessageDialog(
                        null,
                        "Số điện thoại phải đúng 10 chữ số và bắt đầu bằng số 0!",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE
                );
                continue;
            }

            if (name.isEmpty() || dob.isEmpty() || phone.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Vui lòng nhập đầy đủ thông tin.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                continue;
            }

            if (!phone.matches("\\d{8,15}")) {
                JOptionPane.showMessageDialog(null, "Số điện thoại không hợp lệ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                continue;
            }

            try {
                // CHUẨN: gửi 4 trường: Hoten|NgaySinh|SoDienThoai|GioiTinh
                String changeInfoData = name + "|" + dob + "|" + phone + "|" + gender;
                byte[] dataBytes = changeInfoData.getBytes(StandardCharsets.UTF_8);

                CommandAPDU changeInfoCommand = new CommandAPDU(0x00, 0x05, 0x00, 0x00, dataBytes);
                ResponseAPDU response = channel.transmit(changeInfoCommand);

                if (response.getSW() == 0x9000) {
                    responseField.setText("Thông tin đã được thay đổi thành công.");
                    JOptionPane.showMessageDialog(null, "Thông tin đã được thay đổi thành công.", "Thành công", JOptionPane.INFORMATION_MESSAGE);

                    // ===== NẾU CÓ AVATAR MỚI THÌ GỬI XUỐNG THẺ =====
                    if (newAvatarData != null) {
                        sendImageData(newAvatarData);
                        newAvatarData = null;
                    }

                    readCardData();
                    return;
                } else {
                    JOptionPane.showMessageDialog(null,
                            "Lỗi khi thay đổi thông tin. SW=" + Integer.toHexString(response.getSW()),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }

            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Lỗi khi thay đổi thông tin: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ================== BALANCE / POINT / TIER / VOUCHER ==================
    private long getBalanceFromCard() throws CardException {
        CommandAPDU cmd = new CommandAPDU(0x00, 0x17, 0x00, 0x00);
        ResponseAPDU resp = channel.transmit(cmd);
        if (resp.getSW() != 0x9000) throw new CardException("Get balance failed");
        String s = new String(resp.getData(), StandardCharsets.UTF_8).trim();
        if (s.isEmpty()) return 0;
        return Long.parseLong(s);
    }

    private void setBalanceToCard(long value) throws CardException {
        setBalanceToCard(value, 0x02);
    }

    private void setBalanceToCard(long value, int logType) throws CardException {
        String balanceStr = String.valueOf(value);
        byte[] balanceBytes = balanceStr.getBytes(StandardCharsets.UTF_8);

        long nowSec = System.currentTimeMillis() / 1000L;
        byte[] ts = new byte[]{
                (byte)((nowSec >> 24) & 0xFF),
                (byte)((nowSec >> 16) & 0xFF),
                (byte)((nowSec >> 8) & 0xFF),
                (byte)(nowSec & 0xFF)
        };

        byte[] data = new byte[balanceBytes.length + 1 + 4];
        System.arraycopy(balanceBytes, 0, data, 0, balanceBytes.length);
        data[balanceBytes.length] = (byte)0x7C;
        System.arraycopy(ts, 0, data, balanceBytes.length + 1, 4);

        CommandAPDU cmd = new CommandAPDU(0x00, 0x16, logType & 0xFF, 0x00, data);
        ResponseAPDU resp = channel.transmit(cmd);
        if (resp.getSW() != 0x9000) throw new CardException("Set balance failed");
    }

    private int getPointsFromCard() throws CardException {
        CommandAPDU cmd = new CommandAPDU(0x00, 0x13, 0x00, 0x00);
        ResponseAPDU resp = channel.transmit(cmd);
        if (resp.getSW() != 0x9000) throw new CardException("Get points failed");
        String s = new String(resp.getData(), StandardCharsets.UTF_8).trim();
        if (s.isEmpty()) return 0;
        return Integer.parseInt(s);
    }

    private void setPointsToCard(int value) throws CardException {
        byte[] data = String.valueOf(value).getBytes(StandardCharsets.UTF_8);
        CommandAPDU cmd = new CommandAPDU(0x00, 0x12, 0x00, 0x00, data);
        ResponseAPDU resp = channel.transmit(cmd);
        if (resp.getSW() != 0x9000) throw new CardException("Set points failed");
    }

    private int getTierFromCard() throws CardException {
        CommandAPDU cmd = new CommandAPDU(0x00, 0x14, 0x00, 0x00);
        ResponseAPDU resp = channel.transmit(cmd);
        if (resp.getSW() != 0x9000) throw new CardException("Get tier failed");
        return resp.getData()[0];
    }

    private void setTierOnCard(int tier) throws CardException {
        CommandAPDU cmd = new CommandAPDU(0x00, 0x1A, (byte) tier, 0x00);
        ResponseAPDU resp = channel.transmit(cmd);
        if (resp.getSW() != 0x9000) throw new CardException("Set tier failed");
    }

    private int getVoucherLevel() throws CardException {
        CommandAPDU cmd = new CommandAPDU(0x00, 0x19, 0x00, 0x00);
        ResponseAPDU resp = channel.transmit(cmd);
        if (resp.getSW() != 0x9000) throw new CardException("Get voucher failed");
        return resp.getData()[0] & 0xFF;
    }

    private void setVoucherLevel(int level) throws CardException {
        CommandAPDU cmd = new CommandAPDU(0x00, 0x18, level, 0x00);
        ResponseAPDU resp = channel.transmit(cmd);
        if (resp.getSW() != 0x9000) throw new CardException("Set voucher failed");
    }

    // ================== NẠP TIỀN – DIALOG MỚI ==================
//    private void topUpMoney() {
//        if (!isConnected || channel == null) {
//            responseField.setText("Bạn phải kết nối với thẻ trước!");
//            return;
//        }
//
//        JPanel panel = new JPanel(new GridBagLayout());
//        panel.setBackground(LIGHT_BG);
//        GridBagConstraints gbc = new GridBagConstraints();
//        gbc.insets = new Insets(5,5,5,5);
//        gbc.anchor = GridBagConstraints.WEST;
//
//        gbc.gridx=0; gbc.gridy=0;
//        panel.add(createLabel("Nhập số tiền nạp (VNĐ):"), gbc);
//
//        JTextField inputField = new JTextField();
//        gbc.gridx=1; gbc.fill=GridBagConstraints.HORIZONTAL; gbc.weightx=1.0;
//        panel.add(inputField, gbc);
//
//        int opt = JOptionPane.showConfirmDialog(this, panel, "Nạp tiền", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
//        if (opt != JOptionPane.OK_OPTION) {
//            responseField.setText("Đã hủy nạp tiền.");
//            return;
//        }
//
//        String input = inputField.getText();
//        if (input == null) {
//            responseField.setText("Đã hủy nạp tiền.");
//            return;
//        }
//
//        input = input.trim();
//        if (input.isEmpty()) {
//            JOptionPane.showMessageDialog(this, "Số tiền không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
//            return;
//        }
//
//        try {
//            long amount = Long.parseLong(input);
//            if (amount <= 0) {
//                JOptionPane.showMessageDialog(this, "Số tiền phải > 0!", "Lỗi", JOptionPane.ERROR_MESSAGE);
//                return;
//            }
//
//            long current = getBalanceFromCard();
//            long updated = current + amount;
//            setBalanceToCard(updated);
//
//            responseField.setText("Nạp tiền thành công. Số dư mới: " + updated + " VNĐ");
//        } catch (NumberFormatException ex) {
//            JOptionPane.showMessageDialog(this, "Số tiền không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
//        } catch (CardException ex) {
//            JOptionPane.showMessageDialog(this, "Lỗi thẻ: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
//        }
//    }

    private void topUpMoney() {
        if (!isConnected || channel == null) {
            responseField.setText("Bạn phải kết nối với thẻ trước!");
            return;
        }

        // ===== PANEL CHÍNH =====
        JPanel panel = new JPanel(new BorderLayout(10, 15));
        panel.setBackground(LIGHT_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Chọn số tiền nạp");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(PRIMARY_PURPLE);
        panel.add(title, BorderLayout.NORTH);

        // ===== GRID CÁC BLOCK NẠP NHANH =====
        JPanel grid = new JPanel(new GridLayout(2, 2, 12, 12));
        grid.setBackground(LIGHT_BG);

        long[] quickAmounts = {
                100_000L,
                200_000L,
                500_000L,
                1_000_000L
        };

        Color[] colors = {
                ACCENT_PURPLE,
                PRIMARY_PURPLE,
                SUCCESS_COLOR,
                new Color(52, 152, 219)
        };

        JTextField inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210,210,210)),
                BorderFactory.createEmptyBorder(6,8,6,8)
        ));

        for (int i = 0; i < quickAmounts.length; i++) {
            long amount = quickAmounts[i];

            JPanel card = createSelectCard(
                    formatMoneyNoSign(amount) + " VNĐ",
                    "Nạp nhanh",
                    colors[i % colors.length]
            );

            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            card.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    inputField.setText(String.valueOf(amount));
                }
            });

            grid.add(card);
        }

        panel.add(grid, BorderLayout.CENTER);

        // ===== PANEL NHẬP TAY =====
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(LIGHT_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(createLabel("Nhập số tiền khác (VNĐ):"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        inputPanel.add(inputField, gbc);

        panel.add(inputPanel, BorderLayout.SOUTH);

        // ===== HIỂN THỊ POPUP =====
        int opt = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Nạp tiền",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (opt != JOptionPane.OK_OPTION) {
            responseField.setText("Đã hủy nạp tiền.");
            return;
        }

        String input = inputField.getText().trim();
        if (input.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập số tiền!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            long amount = Long.parseLong(input);
            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "Số tiền phải lớn hơn 0!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            long current = getBalanceFromCard();
            long updated = current + amount;

            // LOG_TOPUP = 0x02 (giữ logic cũ)
            setBalanceToCard(updated, 0x02);

            responseField.setText("Nạp tiền thành công: +" +
                    formatMoneyNoSign(amount) +
                    " VNĐ | Số dư mới: " +
                    formatMoneyNoSign(updated) + " VNĐ");

            JOptionPane.showMessageDialog(
                    this,
                    "Nạp tiền thành công!\nSố dư mới: " + formatMoneyNoSign(updated) + " VNĐ",
                    "Thành công",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Số tiền không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        } catch (CardException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi thẻ: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================== CỬA HÀNG ==================
    // ==== CỬA HÀNG DẠNG Ô VUÔNG NHIỀU MÀU ====
    private void openStore() {
        if (!isConnected || channel == null) {
            responseField.setText("Bạn phải kết nối với thẻ trước!");
            return;
        }

        // Panel chính
        JPanel mainPanel = new JPanel(new BorderLayout(10, 15));
        mainPanel.setBackground(LIGHT_BG);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Chọn sản phẩm muốn mua");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(PRIMARY_PURPLE);
        mainPanel.add(title, BorderLayout.NORTH);

        // Grid 3x2 cho 6 sản phẩm
        JPanel grid = new JPanel(new GridLayout(3, 2, 12, 12));
        grid.setBackground(LIGHT_BG);

        // Mỗi ô 1 màu giống dãy CHỨC NĂNG
        Color[] colors = new Color[]{
                ACCENT_PURPLE,          // Áo thun
                PRIMARY_PURPLE,         // Quần jean
                SUCCESS_COLOR,          // Thắt lưng
                new Color(52,152,219),  // Mũ
                new Color(230,126,34),  // Găng tay
                new Color(41,128,185)   // Giày
        };

        final JPanel[] cards = new JPanel[products.length];
        final int[] selectedIndex = {-1};

        for (int i = 0; i < products.length; i++) {
            Product p = products[i];
            String titleText = p.name;
            String priceText = formatPrice(p.price);

            JPanel card = createSelectCard(titleText, priceText, colors[i % colors.length]);
            int index = i;

            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            card.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    // cập nhật chọn
                    selectedIndex[0] = index;
                    for (int j = 0; j < cards.length; j++) {
                        setCardSelected(cards[j], j == index);
                    }
                }
            });

            cards[i] = card;
            grid.add(card);
        }

        mainPanel.add(grid, BorderLayout.CENTER);

        int option = JOptionPane.showConfirmDialog(
                this,
                mainPanel,
                "Cửa hàng",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (option != JOptionPane.OK_OPTION) {
            responseField.setText("Đã đóng cửa hàng.");
            return;
        }

        if (selectedIndex[0] < 0) {
            JOptionPane.showMessageDialog(this, "Bạn chưa chọn sản phẩm!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            handlePurchase(products[selectedIndex[0]]);  // GIỮ LOGIC CŨ
        } catch (CardException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi thẻ: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Tính giá cuối cùng theo thứ tự:
     * 1. Giảm theo hạng
     * 2. Giảm theo voucher
     */
    private long calculateFinalPrice(long basePrice, int tier, int voucherLv) {

        // ===== TIER DISCOUNT =====
        double tierDiscount = tier * 0.05;
        if (tierDiscount > 0.20) tierDiscount = 0.20;

        // ===== VOUCHER DISCOUNT =====
        double voucherDiscount;
        switch (voucherLv) {
            case 1 -> voucherDiscount = 0.10;
            case 2 -> voucherDiscount = 0.15;
            case 3 -> voucherDiscount = 0.20;
            case 4 -> voucherDiscount = 0.25;
            case 5 -> voucherDiscount = 0.30;
            default -> voucherDiscount = 0.0;
        }

        // ===== TÍNH GIÁ =====
        long afterTier = Math.round(basePrice * (1.0 - tierDiscount));
        return Math.round(afterTier * (1.0 - voucherDiscount));
    }

    private void handlePurchase(Product p) throws CardException {
        long balance = getBalanceFromCard();
        int tier = getTierFromCard();
        int voucherLv = getVoucherLevel();

        long finalPrice = calculateFinalPrice(p.price, tier, voucherLv);

        // ===== TÍNH RIÊNG ĐỂ HIỂN THỊ =====
        double tierDiscount = Math.min(tier * 0.05, 0.20);
        long priceAfterTier = Math.round(p.price * (1.0 - tierDiscount));

        double voucherDiscount = switch (voucherLv) {
            case 1 -> 0.10;
            case 2 -> 0.15;
            case 3 -> 0.20;
            case 4 -> 0.25;
            case 5 -> 0.30;
            default -> 0.0;
        };

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Giá gốc: " + formatPrice(p.price) +
                        "\nGiảm theo hạng: " + (int)(tierDiscount * 100) + "%" +
                        "\nGiá sau giảm hạng: " + formatPrice(priceAfterTier) +
                        "\nGiảm voucher: " + (int)(voucherDiscount * 100) + "%" +
                        "\n--------------------------------" +
                        "\nGiá thanh toán: " + formatPrice(finalPrice) +
                        "\nSố dư hiện tại: " + formatPrice(balance) +
                        "\n\nXác nhận mua?",
                "Xác nhận mua",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (confirm != JOptionPane.OK_OPTION) {
            responseField.setText("Hủy mua hàng.");
            return;
        }

        if (balance < finalPrice) {
            JOptionPane.showMessageDialog(this, "Không đủ tiền!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            responseField.setText("Thanh toán thất bại - không đủ tiền.");
            return;
        }

        // ===== TRỪ TIỀN =====
        long newBalance = balance - finalPrice;
        setBalanceToCard(newBalance, 0x03); // LOG_MUA_HÀNG

        // ===== CỘNG ĐIỂM (LUÔN +100) =====
        int newPoints = getPointsFromCard() + 50;
        setPointsToCard(newPoints);

        // ===== DÙNG XONG VOUCHER → RESET =====
        if (voucherLv > 0) {
            setVoucherLevel(0);
        }

        responseField.setText("Mua thành công " + p.name +
                ". Số dư còn: " + formatMoneyNoSign(newBalance) +
                " VNĐ, điểm: " + newPoints);

        JOptionPane.showMessageDialog(this, "Mua thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    // ================== ĐỔI ĐIỂM LẤY VOUCHER (UI Ô VUÔNG) ==================
    private void exchangePoints() {
        if (!isConnected || channel == null) {
            responseField.setText("Bạn phải kết nối với thẻ trước!");
            return;
        }

        // Tên, điểm, level voucher
        String[] saleOptions = {
                "Voucher giảm 10%",
                "Voucher giảm 15%",
                "Voucher giảm 20%",
                "Voucher giảm 25%",
                "Voucher giảm 30%"
        };

        int[] costPoints = {100, 200, 300, 500, 1000};
        int[] voucherLevels = {1, 2, 3, 4, 5};

        // Panel chính
        JPanel mainPanel = new JPanel(new BorderLayout(10, 15));
        mainPanel.setBackground(LIGHT_BG);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Chọn voucher muốn đổi");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(PRIMARY_PURPLE);
        mainPanel.add(title, BorderLayout.NORTH);

        // Grid 3x2 card
        JPanel grid = new JPanel(new GridLayout(3, 2, 12, 12));
        grid.setBackground(LIGHT_BG);

        // Mỗi ô 1 màu
        Color[] colors = new Color[]{
                ACCENT_PURPLE,                 // 10%
                PRIMARY_PURPLE,                // 15%
                SUCCESS_COLOR,                 // 20%
                new Color(52, 152, 219),       // 25%
                new Color(230, 126, 34)        // 30%
        };

        final JPanel[] cards = new JPanel[saleOptions.length];
        final int[] selected = {-1};

        for (int i = 0; i < saleOptions.length; i++) {
            String titleText = saleOptions[i];
            String subText = "(" + costPoints[i] + " điểm)";

            JPanel card = createSelectCard(titleText, subText, colors[i % colors.length]);
            int index = i;

            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            card.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    selected[0] = index;
                    for (int j = 0; j < cards.length; j++) {
                        setCardSelected(cards[j], j == index);
                    }
                }
            });

            cards[i] = card;
            grid.add(card);
        }

        mainPanel.add(grid, BorderLayout.CENTER);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                mainPanel,
                "Đổi điểm",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (confirm != JOptionPane.OK_OPTION) {
            responseField.setText("Đã hủy đổi điểm.");
            return;
        }

        if (selected[0] < 0) {
            JOptionPane.showMessageDialog(this, "Bạn chưa chọn voucher!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int idx = selected[0];
        int cost = costPoints[idx];
        int level = voucherLevels[idx];

        try {
            int currentPoints = getPointsFromCard();
            if (currentPoints < cost) {
                JOptionPane.showMessageDialog(
                        this,
                        "Điểm của bạn không đủ (" + currentPoints + " điểm).\n" +
                                "Cần " + cost + " điểm để đổi voucher này.",
                        "Không đủ điểm",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            setPointsToCard(currentPoints - cost);
            setVoucherLevel(level);

            responseField.setText("Đổi voucher thành công! Điểm còn lại: " + (currentPoints - cost));
            JOptionPane.showMessageDialog(
                    this,
                    "Bạn đã đổi được " + saleOptions[idx] + " cho lần mua tiếp theo!",
                    "Thành công",
                    JOptionPane.INFORMATION_MESSAGE
            );

            // Cập nhật lại thông tin hiển thị (nếu đang mở)
            readCardData();

        } catch (Exception e) {
            responseField.setText("Lỗi đổi điểm: " + e.getMessage());
        }
    }

    // ================== NÂNG HẠNG ==================
    // ==== NÂNG HẠNG DẠNG Ô VUÔNG ====
    private void openUpgradeShop() {
        if (!isConnected || channel == null) {
            responseField.setText("Bạn phải kết nối với thẻ trước!");
            return;
        }

        JPanel mainPanel = new JPanel(new BorderLayout(10, 15));
        mainPanel.setBackground(LIGHT_BG);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Chọn gói nâng hạng");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(PRIMARY_PURPLE);
        mainPanel.add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 2, 12, 12));
        grid.setBackground(LIGHT_BG);

        // 4 màu khác nhau
        Color[] colors = new Color[]{
                ACCENT_PURPLE,                 // Bạc
                new Color(241,196,15),         // Vàng
                new Color(155,89,182),         // Bạch kim
                new Color(52,152,219)          // Kim cương
        };

        final JPanel[] cards = new JPanel[tierPacks.length];
        final int[] selectedIndex = {-1};

        for (int i = 0; i < tierPacks.length; i++) {
            TierPack pack = tierPacks[i];
            String titleText = pack.name;
            String priceText = formatPrice(pack.price);

            JPanel card = createSelectCard(titleText, priceText, colors[i % colors.length]);
            int index = i;

            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            card.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    selectedIndex[0] = index;
                    for (int j = 0; j < cards.length; j++) {
                        setCardSelected(cards[j], j == index);
                    }
                }
            });

            cards[i] = card;
            grid.add(card);
        }

        mainPanel.add(grid, BorderLayout.CENTER);

        int opt = JOptionPane.showConfirmDialog(
                this,
                mainPanel,
                "Nâng hạng",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (opt != JOptionPane.OK_OPTION) {
            responseField.setText("Hủy nâng hạng.");
            return;
        }

        if (selectedIndex[0] < 0) {
            JOptionPane.showMessageDialog(this, "Bạn chưa chọn gói!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int idx = selectedIndex[0];

        try {
            TierPack pack = tierPacks[idx];
            int currentTier = getTierFromCard();
            long balance = getBalanceFromCard();

            if (currentTier >= pack.tier) {
                JOptionPane.showMessageDialog(this,
                        "Bạn đang ở hạng " + currentTier + " rồi.\nKhông được mua gói thấp hơn hoặc bằng.",
                        "Không hợp lệ",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (balance < pack.price) {
                JOptionPane.showMessageDialog(this, "Không đủ tiền để nâng hạng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Nâng từ hạng " + currentTier + " lên " + pack.name +
                            "\nGiá: " + pack.price +
                            "\nSố dư hiện tại: " + balance +
                            "\n\nXác nhận?",
                    "Xác nhận nâng hạng",
                    JOptionPane.OK_CANCEL_OPTION
            );

            if (confirm != JOptionPane.OK_OPTION) return;

            long newBalance = balance - pack.price;
            // LOG_UPGRADE
            setBalanceToCard(newBalance, 0x05);

            // tính hết hạn 30 ngày
            long now = System.currentTimeMillis() / 1000;
            long expire = now + 30L * 24 * 60 * 60;

            byte[] expiryBytes = new byte[]{
                    (byte) ((expire >> 24) & 0xFF),
                    (byte) ((expire >> 16) & 0xFF),
                    (byte) ((expire >> 8) & 0xFF),
                    (byte) (expire & 0xFF)
            };

            CommandAPDU setTierCmd = new CommandAPDU(
                    0x00,
                    0x1A,
                    pack.tier,
                    0x00,
                    expiryBytes
            );

            ResponseAPDU respTier = channel.transmit(setTierCmd);

            if (respTier.getSW() != 0x9000) {
                JOptionPane.showMessageDialog(this,
                        "Lỗi ghi tier hoặc thời hạn! SW=" + Integer.toHexString(respTier.getSW()),
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            responseField.setText("Nâng hạng thành công! Hạng mới: " + pack.tier +
                    ", số dư: " + newBalance);
            JOptionPane.showMessageDialog(
                    this,
                    "Nâng hạng thành công!",
                    "Thành công",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (CardException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi thẻ: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void unblockCard() {
        if (!isConnected || channel == null) {
            responseField.setText("Bạn phải kết nối với thẻ trước!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Thẻ đang bị khóa do nhập sai PIN nhiều lần.\n" +
                        "Bạn có chắc chắn muốn mở khóa thẻ không?\n\n" +
                        "Sau khi mở khóa, bạn sẽ phải đặt PIN mới.",
                "Mở khóa thẻ",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            responseField.setText("Đã hủy mở khóa thẻ.");
            return;
        }

        try {
            // Gửi APDU mở khóa (KHÔNG GỬI PIN)
            CommandAPDU unlockApdu = new CommandAPDU(
                    0x00,
                    INS_UNLOCK_CARD, // = 0x03
                    0x00,
                    0x00
            );

            ResponseAPDU response = channel.transmit(unlockApdu);

            if (response.getSW() != 0x9000) {
                JOptionPane.showMessageDialog(
                        this,
                        "Mở khóa thẻ thất bại! SW=" + Integer.toHexString(response.getSW()),
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            JOptionPane.showMessageDialog(
                    this,
                    "Mở khóa thẻ thành công!\nVui lòng đặt PIN mới.",
                    "Thành công",
                    JOptionPane.INFORMATION_MESSAGE
            );

            // 👉 GỌI NGAY ĐỔI PIN
            changePinAfterUnlock();

        } catch (CardException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Lỗi giao tiếp thẻ: " + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void changePinAfterUnlock() {
        JPasswordField newPinField = new JPasswordField();
        JPasswordField confirmPinField = new JPasswordField();

        Object[] message = {
                "Nhập PIN mới (6 chữ số):", newPinField,
                "Xác nhận PIN mới:", confirmPinField
        };

        int option = JOptionPane.showConfirmDialog(
                this,
                message,
                "Đặt PIN mới",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (option != JOptionPane.OK_OPTION) {
            responseField.setText("Chưa đặt PIN mới.");
            return;
        }

        String newPin = new String(newPinField.getPassword()).trim();
        String confirmPin = new String(confirmPinField.getPassword()).trim();

        if (!newPin.matches("\\d{6}")) {
            JOptionPane.showMessageDialog(this,
                    "PIN phải gồm đúng 6 chữ số!",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!newPin.equals(confirmPin)) {
            JOptionPane.showMessageDialog(this,
                    "PIN xác nhận không khớp!",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            CommandAPDU apdu = new CommandAPDU(
                    0x00,
                    INS_CHANGE_PIN_AFTER_UNLOCK, // = 0x21
                    0x00,
                    0x00,
                    newPin.getBytes(StandardCharsets.UTF_8)
            );

            ResponseAPDU response = channel.transmit(apdu);

            if (response.getSW() == 0x9000) {
                JOptionPane.showMessageDialog(
                        this,
                        "Đặt PIN mới thành công!",
                        "Thành công",
                        JOptionPane.INFORMATION_MESSAGE
                );
                responseField.setText("Đã mở khóa và đặt PIN mới thành công!");
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Đặt PIN mới thất bại! SW=" + Integer.toHexString(response.getSW()),
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE
                );
            }

        } catch (CardException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Lỗi giao tiếp thẻ: " + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void forgotPin() {
        if (!isConnected || channel == null) {
            JOptionPane.showMessageDialog(null, "Bạn phải kết nối với thẻ trước!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // ==== Popup nhập số điện thoại ====
        JTextField phoneField = new JTextField();
        JPanel panel = new JPanel(new GridLayout(2, 1, 5, 5));
        panel.setBackground(LIGHT_BG);
        panel.add(createLabel("Nhập số điện thoại đã đăng ký trên thẻ:"));
        panel.add(phoneField);

        int opt = JOptionPane.showConfirmDialog(
                null,
                panel,
                "Quên mã PIN",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (opt != JOptionPane.OK_OPTION) return;

        String enteredPhone = phoneField.getText().trim();
        if (enteredPhone.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Vui lòng nhập số điện thoại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // ==== Đọc dữ liệu thẻ để lấy SĐT ====
            CommandAPDU cmd = new CommandAPDU(0x00, 0x06, 0x00, 0x00);
            ResponseAPDU resp = channel.transmit(cmd);

            if (resp.getSW() != 0x9000) {
                JOptionPane.showMessageDialog(null, "Không đọc được dữ liệu thẻ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            byte[] data = resp.getData();
            int realLen = data.length;
            while (realLen > 0 && data[realLen - 1] == 0) realLen--;

            String raw = new String(data, 0, realLen, StandardCharsets.UTF_8);
            String[] fields = raw.split("\\|");
            if (fields.length < 5) {
                JOptionPane.showMessageDialog(null, "Dữ liệu thẻ không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String phoneOnCard = fields[4];

            // ==== So sánh số điện thoại ====
            if (!enteredPhone.equals(phoneOnCard)) {
                JOptionPane.showMessageDialog(null, "SĐT không khớp!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // ==== Xác nhận reset ====
            int confirm = JOptionPane.showConfirmDialog(
                    null,
                    "Xác nhận đặt lại mã PIN?\nPIN mới sẽ là: 000000",
                    "Reset PIN",
                    JOptionPane.OK_CANCEL_OPTION
            );
            if (confirm != JOptionPane.OK_OPTION) return;

            // ==== Gửi lệnh Reset PIN – INS = 0x20 ====
            CommandAPDU resetCmd = new CommandAPDU(0x00, 0x20, 0x00, 0x00);
            ResponseAPDU resetResp = channel.transmit(resetCmd);

            if (resetResp.getSW() == 0x9000) {

                JOptionPane.showMessageDialog(
                        null,
                        "Đặt lại PIN thành công!\n" +
                                "PIN tạm thời: 000000\n\n" +
                                "Vui lòng đổi PIN mới ngay.",
                        "Đổi mã PIN",
                        JOptionPane.INFORMATION_MESSAGE
                );

                // 👉 BẮT BUỘC GỌI ĐỔI PIN
                changePin();   // dùng lại popup đổi PIN hiện có

            } else {
                JOptionPane.showMessageDialog(
                        null,
                        "Lỗi reset PIN! SW=" + Integer.toHexString(resetResp.getSW()),
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE
                );
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Lỗi hệ thống: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private byte[] chooseAndReadFile(JLabel previewLabel) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn ảnh đại diện");
        fileChooser.setFileFilter(
                new javax.swing.filechooser.FileNameExtensionFilter(
                        "Image files", "jpg", "jpeg", "png", "gif", "bmp"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            try {
                byte[] data = Files.readAllBytes(selectedFile.toPath());

                // scale ảnh vào đúng label truyền vào
                int w = previewLabel.getWidth();
                int h = previewLabel.getHeight();
                if (w <= 0 || h <= 0) { w = 120; h = 160; }

                BufferedImage img = ImageIO.read(selectedFile);
                if (img != null) {
                    Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
                    previewLabel.setIcon(new ImageIcon(scaled));
                    previewLabel.revalidate();
                    previewLabel.repaint();
                } else {
                    JOptionPane.showMessageDialog(this, "Không đọc được file ảnh.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return null;
                }

                return data;
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Lỗi khi đọc file: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
        return null;
    }

    private byte[] chooseAndReadFile() {
        // dùng imageLabel (khởi tạo thẻ)
        return chooseAndReadFile(imageLabel);
    }

    private void sendImageData(byte[] fileData) {
        int maxDataLength = 255;
        try {
            for (int offset = 0; offset < fileData.length; offset += maxDataLength) {
                int length = Math.min(maxDataLength, fileData.length - offset);
                byte[] chunk = new byte[length];
                System.arraycopy(fileData, offset, chunk, 0, length);

                byte p1 = (offset + length >= fileData.length) ? (byte)0x01 : (byte)0x00;

                CommandAPDU sendImage = new CommandAPDU(0x00, 0x08, p1, 0x00, chunk);
                ResponseAPDU response = channel.transmit(sendImage);

                if (response.getSW() != 0x9000) {
                    responseField.setText("Lỗi tại khối " + offset / maxDataLength + ": SW=" + Integer.toHexString(response.getSW()));
                    return;
                }
            }
            responseField.setText("Gửi ảnh thành công!");
        } catch (Exception e) {
            responseField.setText("Lỗi khi gửi ảnh: " + e.getMessage());
        }
    }

    private void getImageFile(JLabel imageInfoLabel) {
        try {
            CommandAPDU sendImage = new CommandAPDU(0x00, 0x09, 0x00, 0x00);
            ResponseAPDU response = channel.transmit(sendImage);

            byte[] responseData = response.getData();
            if (responseData != null && responseData.length > 0) {
                ImageIcon imageIcon = new ImageIcon(responseData);

                int labelWidth = imageInfoLabel.getWidth();
                int labelHeight = imageInfoLabel.getHeight();
                if (labelWidth <= 0 || labelHeight <= 0) {
                    labelWidth = 100;
                    labelHeight = 150;
                }

                Image img = imageIcon.getImage();
                Image scaledImage = img.getScaledInstance(labelWidth, labelHeight, Image.SCALE_SMOOTH);
                imageInfoLabel.setIcon(new ImageIcon(scaledImage));
                imageInfoLabel.revalidate();
                imageInfoLabel.repaint();
            } else {
                responseField.setText("Lỗi: Dữ liệu hình ảnh không hợp lệ.");
            }
        } catch (Exception e) {
            responseField.setText("Lỗi: " + e.getMessage());
        }
    }

    // ================== LỊCH SỬ GIAO DỊCH (THANH TÍM NHẠT) ==================
    private void viewTransactionLogs() {
        if (!isConnected || channel == null) {
            responseField.setText("Bạn phải kết nối với thẻ trước!");
            return;
        }

        try {
            List<Integer> deltas = new ArrayList<>();
            List<String> types = new ArrayList<>();
            List<Long> times = new ArrayList<>();

            // Đọc tối đa 5 log từ thẻ (giữ logic cũ)
            for (int i = 0; i < 5; i++) {
                CommandAPDU cmd = new CommandAPDU(0x00, 0x15, i, 0x00);
                ResponseAPDU resp = channel.transmit(cmd);

                if (resp.getSW() != 0x9000) break;

                byte[] raw = resp.getData();
                if (raw.length < LOG_ENTRY_SIZE) continue;

                byte type = raw[0];
                char sign = (char) raw[1];

                if (type != 0x02 && type != 0x03 && type != 0x05)
                    continue;

                String digits = new String(raw, 2, 10).replace("\u0000", "");
                digits = digits.replaceFirst("^0+(?!$)", "");
                if (digits.equals("")) digits = "0";

                int delta = Integer.parseInt(digits);
                if (sign == '-') delta = -delta;

                long t =
                        ((raw[12] & 0xFFL) << 24) |
                                ((raw[13] & 0xFFL) << 16) |
                                ((raw[14] & 0xFFL) << 8) |
                                (raw[15] & 0xFFL);

                String typeName = switch (type) {
                    case 0x02 -> "Nạp tiền";
                    case 0x03 -> "Mua hàng";
                    case 0x05 -> "Nâng hạng";
                    default -> "Khác";
                };

                deltas.add(delta);
                types.add(typeName);
                times.add(t);
            }

            if (deltas.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Không có giao dịch.", "Thông báo", JOptionPane.PLAIN_MESSAGE);
                return;
            }

            long currentBalance = getBalanceFromCard();

            // Tính số dư sau mỗi giao dịch (giữ đúng logic cũ)
            List<Long> balances = new ArrayList<>();
            long runningBalance = currentBalance;
            for (int i = 0; i < deltas.size(); i++) runningBalance -= deltas.get(i);
            for (int i = deltas.size() - 1; i >= 0; i--) {
                runningBalance += deltas.get(i);
                balances.add(runningBalance);
            }
            Collections.reverse(balances);

            // ===== UI mới: list các thanh dài =====
            JPanel mainPanel = new JPanel(new BorderLayout(10, 15));
            mainPanel.setBackground(LIGHT_BG);
            mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

            JLabel title = new JLabel("Lịch sử giao dịch");
            title.setFont(new Font("Segoe UI", Font.BOLD, 16));
            title.setForeground(PRIMARY_PURPLE);
            mainPanel.add(title, BorderLayout.NORTH);

            JPanel listPanel = new JPanel();
            listPanel.setBackground(LIGHT_BG);
            listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

            // ===== HEADER CỘT =====
            JPanel header = new JPanel(new GridLayout(1, 5));
            header.setBackground(PRIMARY_PURPLE);
            header.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

            header.add(createHeaderLabel("STT"));
            header.add(createHeaderLabel("Thời gian"));
            header.add(createHeaderLabel("Loại giao dịch"));
            header.add(createHeaderLabel("Biến động"));
            header.add(createHeaderLabel("Số dư"));

            listPanel.add(header);
            listPanel.add(Box.createVerticalStrut(6));

            Color rowColor = new Color(235, 225, 245); // tím nhạt
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");

            for (int i = 0; i < deltas.size(); i++) {
                JPanel row = new JPanel(new GridLayout(1, 5));
                row.setBackground(rowColor);
                row.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

                JLabel sttLabel = new JLabel(String.valueOf(i + 1));
                JLabel timeLabel = new JLabel(sdf.format(new Date(times.get(i) * 1000)));
                JLabel typeLabel = new JLabel(types.get(i));
                JLabel deltaLabel = new JLabel(formatMoneyDelta(deltas.get(i)));
                JLabel balanceLabel = new JLabel(formatMoneyNoSign(balances.get(i)));

                // ===== TÔ MÀU BIẾN ĐỘNG =====
                if (deltas.get(i) >= 0) {
                    deltaLabel.setForeground(SUCCESS_COLOR); // xanh
                } else {
                    deltaLabel.setForeground(DANGER_COLOR);  // đỏ
                }

                sttLabel.setHorizontalAlignment(SwingConstants.CENTER);
                timeLabel.setHorizontalAlignment(SwingConstants.CENTER);
                typeLabel.setHorizontalAlignment(SwingConstants.CENTER);
                deltaLabel.setHorizontalAlignment(SwingConstants.CENTER);
                balanceLabel.setHorizontalAlignment(SwingConstants.CENTER);

                row.add(sttLabel);
                row.add(timeLabel);
                row.add(typeLabel);
                row.add(deltaLabel);
                row.add(balanceLabel);

                listPanel.add(row);
                listPanel.add(Box.createVerticalStrut(6));
            }

            JScrollPane scroll = new JScrollPane(listPanel);
            scroll.setBorder(null);
            mainPanel.add(scroll, BorderLayout.CENTER);

            JOptionPane.showMessageDialog(
                    this,
                    mainPanel,
                    "Lịch sử giao dịch",
                    JOptionPane.PLAIN_MESSAGE
            );

        } catch (Exception e) {
            responseField.setText("Lỗi xem log: " + e.getMessage());
        }
    }

    private JLabel createHeaderLabel(String text) {
        JLabel lb = new JLabel(text);
        lb.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lb.setForeground(Color.WHITE);
        lb.setHorizontalAlignment(SwingConstants.CENTER);
        return lb;
    }

    // ================== UTIL ==================
    private String formatMoneyDelta(long n) {
        String formatted = String.format("%,d", Math.abs(n));
        return (n >= 0 ? "+" : "-") + formatted;
    }

    private String formatMoneyNoSign(long n) {
        return String.format("%,d", n);
    }

    private String formatPrice(long n) {
        return String.format("%,d VNĐ", n);
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        if (len % 2 != 0) {
            s = "0" + s;
            len = s.length();
        }
        byte[] data = new byte[len/2];
        for (int i=0;i<len;i+=2) {
            data[i/2] = (byte)((Character.digit(s.charAt(i),16)<<4)
                    + Character.digit(s.charAt(i+1),16));
        }
        return data;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b: bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length()==1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString().toUpperCase();
    }
}
