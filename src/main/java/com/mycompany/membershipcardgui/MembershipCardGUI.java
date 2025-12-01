/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.membershipcardgui;


import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.smartcardio.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;

import com.formdev.flatlaf.FlatLightLaf;


public class MembershipCardGUI extends JFrame {

    private byte[] fileData;
    private boolean isConnected = false;
    private boolean isCardBlocked = false;
    private Card card = null;
    private CardChannel channel = null;
    private static int counter = 1;

    private JLabel imageLabel;
    private JTextField filePathField;
    private JTextField getBalanceField;

    private JFrame frame;
    private JPanel apduPanel, infoPanel, memberPanel, addMemberPanel, buttonPanel;
    private JTextField responseField, getMaKH, getName, getDob, getGender, getPoints;
    private JPasswordField pinField;
    private JTextField makhField, nameField, dobField;
    private JComboBox<String> genderComboBox;
    private JButton browseButton;
    private JLabel imageInfoLabel;
    // Thêm các nút chức năng
    private JButton initCardButton = new JButton("Khởi tạo thẻ");
    private JButton readCardButton = new JButton("Đọc dữ liệu thẻ");
    private JButton changePinButton = new JButton("Thay đổi mã PIN");
    private JButton editButton = new JButton("Sửa Thông Tin");
    private JButton topUpButton = new JButton("Nạp tiền");
    private JButton storeButton = new JButton("Cửa hàng");
    private JButton upgradeTierButton = new JButton("Nâng hạng");
    private JButton exchangePointsButton = new JButton("Đổi điểm");
    private JButton unblockCartButton = new JButton("Mở khoá thẻ");
    private JButton verifybtn = new JButton("Kiểm tra pin");
    private JButton viewLogButton = new JButton("Xem lịch sử giao dịch");
    private static final int LOG_ENTRY_SIZE = 12;

    private JButton getPublicKeyButton = new JButton("Get Public Key");
    private JButton signDataButton = new JButton("Sign Data");

    public static void main(String[] args) {
        FlatLightLaf.setup(); // 🔥 Bật giao diện mượt như IntelliJ
        SwingUtilities.invokeLater(MembershipCardGUI::new);
    }

    public MembershipCardGUI() {

        // === FRAME CHÍNH ===
        frame = new JFrame("Giao Diện Thẻ Thành Viên Cửa Hàng");
        frame.setSize(950, 550);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(15, 15));
        frame.getContentPane().setBackground(new Color(240, 242, 245));

        // ======================================================
        // =============== PANEL TRÁI – KẾT NỐI THẺ =============
        // ======================================================

        apduPanel = new JPanel();
        apduPanel.setLayout(new BoxLayout(apduPanel, BoxLayout.Y_AXIS));
        apduPanel.setBorder(BorderFactory.createTitledBorder("🔗 KẾT NỐI THẺ"));
        apduPanel.setBackground(Color.WHITE);

        // === Nút Connect / Disconnect ===
        JButton connectButton = new JButton("Connect");
        JButton disconnectButton = new JButton("Disconnect");

        styleButton(connectButton, new Color(41, 128, 185));
        styleButton(disconnectButton, new Color(192, 57, 43));

        connectButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        disconnectButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        apduPanel.add(Box.createVerticalStrut(10));
        apduPanel.add(connectButton);
        apduPanel.add(Box.createVerticalStrut(10));
        apduPanel.add(disconnectButton);
        apduPanel.add(Box.createVerticalStrut(20));

        // === Phần phản hồi ===
        JPanel responsePanel = new JPanel(new BorderLayout());
        responsePanel.setBackground(Color.WHITE);

        JLabel respLabel = new JLabel("📥 Phản hồi:");
        respLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        responseField = new JTextField();
        responseField.setEditable(false);
        responseField.setPreferredSize(new Dimension(0, 40));

        responsePanel.add(respLabel, BorderLayout.NORTH);
        responsePanel.add(responseField, BorderLayout.CENTER);

        apduPanel.add(responsePanel);

        frame.add(apduPanel, BorderLayout.WEST);


        // ======================================================
        // =============== PANEL PHẢI – CÁC CHỨC NĂNG ===========
        // ======================================================

        memberPanel = new JPanel();
        memberPanel.setBorder(BorderFactory.createTitledBorder("⚙️ CÁC CHỨC NĂNG"));
        memberPanel.setLayout(new GridLayout(10, 1, 12, 12));
        memberPanel.setBackground(Color.WHITE);

        // === Style tất cả các nút ===
        styleButton(initCardButton, new Color(52, 152, 219));
        styleButton(readCardButton, new Color(39, 174, 96));
        styleButton(topUpButton, new Color(230, 126, 34));
        styleButton(storeButton, new Color(46, 204, 113));
        styleButton(upgradeTierButton, new Color(142, 68, 173));
        styleButton(exchangePointsButton, new Color(155, 89, 182));
        styleButton(unblockCartButton, new Color(41, 128, 185));
        styleButton(getPublicKeyButton, new Color(22, 160, 133));
        styleButton(signDataButton, new Color(127, 140, 141));
        styleButton(viewLogButton, new Color(52, 73, 94));

        memberPanel.add(initCardButton);
        memberPanel.add(readCardButton);
        memberPanel.add(topUpButton);       // Nạp tiền
        memberPanel.add(storeButton);       // Cửa hàng
        memberPanel.add(upgradeTierButton); // Nâng hạng
        memberPanel.add(exchangePointsButton);
        memberPanel.add(unblockCartButton);
        memberPanel.add(getPublicKeyButton);
        memberPanel.add(signDataButton);
        memberPanel.add(viewLogButton);

        frame.add(memberPanel, BorderLayout.CENTER);


        // ======================================================
        // =============== GÁN SỰ KIỆN – GIỮ NGUYÊN =============
        // ======================================================

        connectButton.addActionListener(e -> connectToCard());
        disconnectButton.addActionListener(e -> disconnectFromCard());

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

        getPublicKeyButton.addActionListener(e -> getPublicKey());
        signDataButton.addActionListener(e -> signData());
        viewLogButton.addActionListener(e -> viewTransactionLogs());
        topUpButton.addActionListener(e -> topUpMoney());
        storeButton.addActionListener(e -> openStore());
        upgradeTierButton.addActionListener(e -> openUpgradeShop());


        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }


    private void styleButton(JButton btn, Color bg) {
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(220, 45));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }


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
                responseField.setText("Chờ kết nối với thẻ...");
                if (terminal.waitForCardPresent(10000)) {
                    card = terminal.connect("*");
                    channel = card.getBasicChannel();
                    isConnected = true;
                    responseField.setText("Kết nối thành công!");

                    // Tự động chọn AID sau khi kết nối thành công
                    selectApplet(); // Chọn applet với AID cố định
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
            String aid = "112233445500"; // AID cố định
            byte[] aidBytes = hexStringToByteArray(aid);

            // Lệnh SELECT AID
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


    private boolean verifyPin() {
        try {
            while (true) { // nhập lại nếu sai
                // Tạo form nhập PIN
                JPanel pinPanel = new JPanel(new GridLayout(2, 2, 5, 5));
                JPasswordField passwordField = new JPasswordField();
                pinPanel.add(new JLabel("Nhập mã PIN (6 số):"));
                pinPanel.add(passwordField);

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

                // Kiểm tra dữ liệu nhập
                if (!pin.matches("\\d{6}")) {
                    JOptionPane.showMessageDialog(
                            null,
                            "Mã PIN phải gồm đúng 6 chữ số!",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE
                    );
                    continue;
                }

                // Gửi lệnh kiểm tra PIN
                byte[] pinBytes = pin.getBytes(StandardCharsets.UTF_8);
                CommandAPDU verifyPinCommand = new CommandAPDU(0x00, 0x02, 0x00, 0x00, pinBytes);
                ResponseAPDU verifyResponse = channel.transmit(verifyPinCommand);

                // Kiểm tra lỗi APDU
                if (verifyResponse.getSW() != 0x9000) {
                    JOptionPane.showMessageDialog(
                            null,
                            "Lỗi từ thẻ! SW=" + Integer.toHexString(verifyResponse.getSW()),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return false;
                }

                // Nhận dữ liệu trả về: [status, remain]
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

                byte status = data[0];               // 0=sai, 1=đúng, 2=khóa
                byte remain = (data.length > 1) ? data[1] : 0; // số lần còn lại

                // ====== XỬ LÝ KẾT QUẢ ======
                if (status == (byte) 0x01) {
                    // PIN đúng
                    responseField.setText("Xác thực mã PIN thành công!");
                    return true;

                } else if (status == (byte) 0x00) {
                    // PIN sai nhưng chưa khóa
                    String msg = "Mã PIN không đúng!";
                    if (remain > 0) {
                        msg += "\nBạn còn " + remain + " lần thử trước khi thẻ bị khóa.";
                    }

                    responseField.setText(msg);
                    JOptionPane.showMessageDialog(
                            null,
                            msg,
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE
                    );
                    // QUAY LẠI WHILE → cho nhập lại
                    continue;

                } else if (status == (byte) 0x02) {
                    // Thẻ đã bị khóa
                    responseField.setText("Thẻ đã bị khóa do nhập sai PIN quá nhiều lần.");
                    JOptionPane.showMessageDialog(
                            null,
                            "Thẻ đã bị khóa do nhập sai PIN quá nhiều lần!",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return false;
                }

                // Trạng thái lạ → lỗi
                JOptionPane.showMessageDialog(
                        null,
                        "Lỗi: trạng thái PIN không xác định (" + status + ")",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE
                );
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


    private void initializeCard() throws IOException {
        if (!isConnected || channel == null) {
            responseField.setText("Bạn phải kết nối với thẻ trước!");
            JOptionPane.showMessageDialog(null, "Bạn phải kết nối với thẻ trước!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Panel thêm thành viên
        JPanel addMemberPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        JPanel addMemberPanel1 = new JPanel(new GridLayout(2, 1, 5, 5));
        JPanel addMemberPanel2 = new JPanel(new GridLayout(5, 2, 5, 5));

        addMemberPanel.setBorder(BorderFactory.createTitledBorder("Khởi tạo thẻ"));

        // Tạo các thành phần giao diện
        imageLabel = new JLabel();
        imageLabel.setPreferredSize(new Dimension(100, 150));
        imageLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        browseButton = new JButton("Tải ảnh lên");

        filePathField = new JTextField(10);
        filePathField.setEditable(false);

        // Thêm ảnh và đường dẫn ảnh
        addMemberPanel1.add(imageLabel, BorderLayout.CENTER);
        addMemberPanel1.add(browseButton);
        addMemberPanel1.setPreferredSize(new Dimension(150, 200)); // Tăng kích thước ảnh
        browseButton.setPreferredSize(new Dimension(200, 30)); // Tăng kích thước nút
        addMemberPanel1.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        browseButton.addActionListener(e -> {
            fileData = chooseAndReadFile();
        });

        addMemberPanel2.add(new JLabel("Pin:"));
        pinField = new JPasswordField();
        addMemberPanel2.add(pinField);

        addMemberPanel2.add(new JLabel("Mã KH:"));

        // Sinh mã KH tự động
        String autoMaKH = "CT" + counter; // Ví dụ: CT1, CT2, ...
        makhField = new JTextField(autoMaKH);
        makhField.setEditable(false); // Không cho phép người dùng sửa mã tự động
        addMemberPanel2.add(makhField);

        addMemberPanel2.add(new JLabel("Họ và Tên:"));
        nameField = new JTextField();
        addMemberPanel2.add(nameField);

        addMemberPanel2.add(new JLabel("Ngày Sinh (dd/MM/yyyy):"));
        dobField = new JTextField();
        addMemberPanel2.add(dobField);

        addMemberPanel2.add(new JLabel("Giới Tính:"));
        String[] genders = {"Nam", "Nữ"};
        genderComboBox = new JComboBox<>(genders);
        addMemberPanel2.add(genderComboBox);

        addMemberPanel.add(addMemberPanel1, BorderLayout.WEST);
        addMemberPanel.add(addMemberPanel2, BorderLayout.CENTER);

        while (true) { // Vòng lặp để giữ hộp thoại mở nếu có lỗi
            // Hiển thị hộp thoại
            int option = JOptionPane.showConfirmDialog(null, addMemberPanel, "Thêm thành viên", JOptionPane.OK_CANCEL_OPTION);

            if (option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION) {
                responseField.setText("Đã hủy thao tác khởi tạo thẻ.");
                return;
            }

            try {
                String maKH = makhField.getText().trim();
                String name = nameField.getText().trim();
                String dob = dobField.getText().trim();
                String gender = (String) genderComboBox.getSelectedItem();
                String pin = new String(pinField.getPassword()).trim();
                if (!pin.matches("\\d{6}")) {
                    JOptionPane.showMessageDialog(null,
                            "Mã PIN phải là 6 chữ số!",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    continue; // quay lại nhập
                }
                // Kiểm tra dữ liệu đầu vào
                if (name.isEmpty() || dob.isEmpty() || pin.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Vui lòng điền đầy đủ thông tin!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    continue; // Lặp lại hộp thoại
                }
                if (!dob.matches("\\d{2}/\\d{2}/\\d{4}")) {
                    JOptionPane.showMessageDialog(null, "Ngày sinh không đúng định dạng dd/MM/yyyy.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    continue;
                }

                // Tạo chuỗi dữ liệu
                String data = String.join("|", pin, maKH, name, dob, gender);
                byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);

                // Kiểm tra kích thước dữ liệu
                if (dataBytes.length > 255) {
                    JOptionPane.showMessageDialog(null, "Dữ liệu quá lớn để lưu vào thẻ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    continue; // Lặp lại hộp thoại
                }

                // Gửi lệnh ghi dữ liệu vào thẻ
                CommandAPDU writeCommand = new CommandAPDU(0x00, 0x01, 0x00, 0x00, dataBytes);
                ResponseAPDU response = channel.transmit(writeCommand);

                int sw1 = response.getSW1();
                int sw2 = response.getSW2();
                if (sw1 == 0x90 && sw2 == 0x00) {
                    if (fileData != null) {
                        // Gửi dữ liệu ảnh nếu có
                        sendImageData(fileData);
                    }
                    responseField.setText("Khởi tạo thẻ thành công!");
                    JOptionPane.showMessageDialog(null, "Khởi tạo thẻ thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);

                    counter++; // Tăng bộ đếm sau khi thành công
                    return; // Thoát vòng lặp khi thành công
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

    private void readCard() {
        if (!isConnected || channel == null) {
            responseField.setText("Bạn phải kết nối với thẻ trước!");
            return;
        }

        if (!verifyPin()) {
            return; // Nếu mã PIN không được xác thực, dừng thực hiện
        } else {
            readCardData();
        }
    }

    private void readCardData() {
        // Tạo panel chính (thông tin thẻ) nhưng KHÔNG thêm vào giao diện ban đầu
        infoPanel = new JPanel();
        infoPanel.setBorder(BorderFactory.createTitledBorder("Thông tin thẻ"));
        infoPanel.setLayout(new GridBagLayout()); // Sử dụng GridBagLayout để căn chỉnh đẹp hơn
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Thêm khoảng cách giữa các thành phần

        // ====== 1. CÁC THÀNH PHẦN GIAO DIỆN ======
        imageInfoLabel = new JLabel();
        imageInfoLabel.setPreferredSize(new Dimension(100, 150));
        imageInfoLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        // ẢNH
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.CENTER;
        infoPanel.add(imageInfoLabel, gbc);

        // Mã KH
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel maKHLabel = new JLabel("Mã KH:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        infoPanel.add(maKHLabel, gbc);

        getMaKH = new JTextField();
        getMaKH.setEditable(false);
        gbc.gridx = 1;
        infoPanel.add(getMaKH, gbc);

        // Họ tên
        JLabel nameLabel = new JLabel("Họ và Tên:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        infoPanel.add(nameLabel, gbc);

        getName = new JTextField();
        getName.setEditable(false);
        gbc.gridx = 1;
        infoPanel.add(getName, gbc);

        // Ngày sinh
        JLabel dobLabel = new JLabel("Ngày Sinh (dd/MM/yyyy):");
        gbc.gridx = 0;
        gbc.gridy = 3;
        infoPanel.add(dobLabel, gbc);

        getDob = new JTextField();
        getDob.setEditable(false);
        gbc.gridx = 1;
        infoPanel.add(getDob, gbc);

        // Giới tính
        JLabel genderLabel = new JLabel("Giới Tính:");
        gbc.gridx = 0;
        gbc.gridy = 4;
        infoPanel.add(genderLabel, gbc);

        getGender = new JTextField();
        getGender.setEditable(false);
        gbc.gridx = 1;
        infoPanel.add(getGender, gbc);

        // Số dư
        JLabel balanceLabel = new JLabel("Số dư (VNĐ):");
        gbc.gridx = 0;
        gbc.gridy = 5;
        infoPanel.add(balanceLabel, gbc);

        getBalanceField = new JTextField();
        getBalanceField.setEditable(false);
        gbc.gridx = 1;
        infoPanel.add(getBalanceField, gbc);

        // Điểm
        JLabel pointsLabel = new JLabel("Tích điểm:");
        gbc.gridx = 0;
        gbc.gridy = 6;
        infoPanel.add(pointsLabel, gbc);

        getPoints = new JTextField();
        getPoints.setEditable(false);
        gbc.gridx = 1;
        infoPanel.add(getPoints, gbc);

        // Hạng
        JLabel tierLabel = new JLabel("Hạng thành viên:");
        gbc.gridx = 0;
        gbc.gridy = 7;
        infoPanel.add(tierLabel, gbc);

        JTextField tierField = new JTextField();
        tierField.setEditable(false);
        gbc.gridx = 1;
        infoPanel.add(tierField, gbc);

        // Thời hạn còn lại
        JLabel expireLabel = new JLabel("Thời hạn hạng còn lại:");
        gbc.gridx = 0;
        gbc.gridy = 8;
        infoPanel.add(expireLabel, gbc);

        JTextField expireField = new JTextField();
        expireField.setEditable(false);
        gbc.gridx = 1;
        gbc.gridy = 8;
        infoPanel.add(expireField, gbc);

        // Nút Đổi PIN + Sửa thông tin
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.add(changePinButton);
        buttonPanel.add(editButton);

        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.CENTER;
        infoPanel.add(buttonPanel, gbc);

        // ====== 2. ĐỌC DỮ LIỆU TỪ THẺ ======
        try {
            // Lệnh đọc info (Mã KH, tên, ngày sinh, giới tính, điểm)
            CommandAPDU readCommand = new CommandAPDU(0x00, 0x06, 0x00, 0x00);
            ResponseAPDU response = channel.transmit(readCommand);

            if (response.getSW() == 0x9000) {
                byte[] data = response.getData();

                int realLen = data.length;
                while (realLen > 0 && data[realLen - 1] == 0x00) {
                    realLen--;
                }

                String rawData = new String(data, 0, realLen, StandardCharsets.UTF_8);
                String[] fields = rawData.split("\\|");

                if (fields.length >= 5) {
                    String maKH = fields[0];
                    String fullName = fields[1];
                    String birthDate = fields[2];
                    String gender = fields[3];
                    String points = fields[4];

                    getMaKH.setText(maKH);
                    getName.setText(fullName);
                    getDob.setText(birthDate);
                    getGender.setText(gender);
                    getPoints.setText(points);

                    long balance = getBalanceFromCard();
                    getBalanceField.setText(formatMoneyNoSign(balance) + " VNĐ");

                    // ==== 2.1. ĐỌC TIER TỪ THẺ ====
                    CommandAPDU getTierCmd = new CommandAPDU(0x00, 0x14, 0x00, 0x00);
                    ResponseAPDU tierResp = channel.transmit(getTierCmd);

                    byte tierValue = 0;
                    if (tierResp.getSW() == 0x9000 && tierResp.getData().length == 1) {
                        tierValue = tierResp.getData()[0];
                    }

                    String tierName = switch (tierValue) {
                        case 0 -> "Basic";
                        case 1 -> "Silver";
                        case 2 -> "Gold";
                        case 3 -> "Platinum";
                        case 4 -> "Diamond";
                        default -> "Unknown";
                    };
                    tierField.setText(tierName);

                    // ==== 2.2. ĐỌC THỜI HẠN HẠNG ====
                    CommandAPDU getExpireCmd = new CommandAPDU(0x00, 0x1B, 0x00, 0x00);
                    ResponseAPDU expireResp = channel.transmit(getExpireCmd);

                    long expireTime = 0;
                    if (expireResp.getSW() == 0x9000 && expireResp.getData().length == 4) {
                        byte[] exp = expireResp.getData();
                        expireTime =
                                ((exp[0] & 0xFFL) << 24) |
                                        ((exp[1] & 0xFFL) << 16) |
                                        ((exp[2] & 0xFFL) << 8)  |
                                        (exp[3] & 0xFFL);
                    }

                    // ==== 2.3. TÍNH NGÀY CÒN LẠI ====
                    long nowSec = System.currentTimeMillis() / 1000;
                    long remainSec = expireTime - nowSec;

                    String remainText;
                    if (expireTime == 0 || tierValue == 0) {
                        // Basic hoặc chưa mua gói
                        remainText = "Không giới hạn / Chưa mua gói";
                    } else if (remainSec <= 0) {
                        remainText = "ĐÃ HẾT HẠN";
                    } else {
                        long days = remainSec / (24 * 3600);
                        if (days <= 0) {
                            remainText = "< 1 ngày";
                        } else {
                            remainText = days + " ngày";
                        }
                    }
                    expireField.setText(remainText);

                    // ==== 2.4. TỰ ĐỘNG HẠ VỀ BASIC KHI HẾT HẠN ====
                    if (expireTime != 0 && remainSec <= 0 && tierValue > 0) {
                        try {
                            // Reset tier về 0 (Basic) + expiry = 0
                            byte[] zeroExpiry = new byte[]{0, 0, 0, 0};
                            CommandAPDU resetTierCmd = new CommandAPDU(
                                    0x00,
                                    0x1A,
                                    0x00,      // P1 = 0 -> Basic
                                    0x00,
                                    zeroExpiry
                            );
                            ResponseAPDU resetTierResp = channel.transmit(resetTierCmd);

                            // Reset voucher về 0
                            CommandAPDU resetVoucherCmd = new CommandAPDU(0x00, 0x18, 0x00, 0x00);
                            ResponseAPDU resetVoucherResp = channel.transmit(resetVoucherCmd);

                            if (resetTierResp.getSW() == 0x9000) {
                                tierField.setText("Basic");
                                expireField.setText("ĐÃ HẾT HẠN");

                                JOptionPane.showMessageDialog(
                                        null,
                                        "Gói hội viên đã hết hạn.\nThẻ tự động chuyển về hạng BASIC.",
                                        "Thông báo",
                                        JOptionPane.INFORMATION_MESSAGE
                                );
                            }

                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(
                                    null,
                                    "Lỗi khi reset hạng: " + e.getMessage(),
                                    "Lỗi",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        }
                    }

                    // ==== 2.5. LẤY ẢNH ====
                    getImageFile(imageInfoLabel);

                    responseField.setText("Đọc dữ liệu thẻ thành công!");
                    JOptionPane.showConfirmDialog(
                            null,
                            infoPanel,
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

        } catch (CardException ex) {
            responseField.setText("Lỗi đọc thẻ: " + ex.getMessage());
        }
    }


    private void changePin() {
        if (!isConnected || channel == null) {
            responseField.setText("Bạn phải kết nối với thẻ trước!");
            JOptionPane.showMessageDialog(null, "Bạn phải kết nối với thẻ trước!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        while (true) { // Lặp lại cho đến khi nhập đúng
            // Tạo panel nhập PIN
            JPanel pinPanel = new JPanel(new GridLayout(3, 2, 5, 5));
            JPasswordField oldPinField = new JPasswordField();
            JPasswordField newPinField = new JPasswordField();
            JPasswordField confirmPinField = new JPasswordField();

            pinPanel.add(new JLabel("Mã PIN cũ:"));
            pinPanel.add(oldPinField);
            pinPanel.add(new JLabel("Mã PIN mới:"));
            pinPanel.add(newPinField);
            pinPanel.add(new JLabel("Xác nhận mã PIN mới:"));
            pinPanel.add(confirmPinField);

            int option = JOptionPane.showConfirmDialog(null, pinPanel, "Thay đổi mã PIN", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION) {
                responseField.setText("Hủy thao tác thay đổi mã PIN.");
                return;
            }

            String oldPin = new String(oldPinField.getPassword()).trim();
            String newPin = new String(newPinField.getPassword()).trim();
            String confirmPin = new String(confirmPinField.getPassword()).trim();

            // PIN cũ phải 6 số
            if (!oldPin.matches("\\d{6}")) {
                JOptionPane.showMessageDialog(null,
                        "Mã PIN cũ phải là 6 chữ số!",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                continue;
            }
            // PIN mới phải 6 số
            if (!newPin.matches("\\d{6}")) {
                JOptionPane.showMessageDialog(null,
                        "Mã PIN mới phải là 6 chữ số!",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                continue;
            }
            if (!newPin.equals(confirmPin)) {
                JOptionPane.showMessageDialog(null, "Mã PIN mới và xác nhận không trùng khớp.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                continue;
            }

            // Chuẩn bị dữ liệu và gửi lệnh APDU
            try {
                String changePinData = String.join("|", oldPin, newPin);
                byte[] dataBytes = changePinData.getBytes(StandardCharsets.UTF_8);

                CommandAPDU changePinCommand = new CommandAPDU(0x00, 0x04, 0x00, 0x00, dataBytes);
                ResponseAPDU response = channel.transmit(changePinCommand);

                if (response.getSW1() == 0x90 && response.getSW2() == 0x00) {
                    responseField.setText("Mã PIN đã được thay đổi thành công.");
                    JOptionPane.showMessageDialog(null, "Mã PIN đã được thay đổi thành công.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    return; // Thoát vòng lặp khi thành công
                } else {
                    String errorMessage = String.format("Lỗi khi thay đổi mã PIN. SW: %04X", response.getSW());
                    JOptionPane.showMessageDialog(null, errorMessage, "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Lỗi khi thay đổi mã PIN: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

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

        while (true) { // Lặp lại cho đến khi nhập đúng
            // Tạo panel nhập thông tin mới
            JPanel infoPanel = new JPanel(new GridLayout(3, 2, 5, 5));
            JTextField nameFieldNew = new JTextField();
            JTextField dobFieldNew = new JTextField();
            JComboBox<String> genderComboBoxNew = new JComboBox<>(new String[]{"Nam", "Nữ"});

            infoPanel.add(new JLabel("Họ và Tên:"));
            infoPanel.add(nameFieldNew);
            infoPanel.add(new JLabel("Ngày Sinh (dd/MM/yyyy):"));
            infoPanel.add(dobFieldNew);
            infoPanel.add(new JLabel("Giới Tính:"));
            infoPanel.add(genderComboBoxNew);

            int option = JOptionPane.showConfirmDialog(null, infoPanel, "Thay đổi thông tin", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION) {
                responseField.setText("Hủy thao tác thay đổi thông tin.");
                return;
            }

            String name = nameFieldNew.getText().trim();
            String dob = dobFieldNew.getText().trim();
            String gender = (String) genderComboBoxNew.getSelectedItem();

            // Kiểm tra dữ liệu nhập
            if (name.isEmpty() || dob.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Vui lòng nhập đầy đủ thông tin.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                continue;
            }
            if (!dob.matches("\\d{2}/\\d{2}/\\d{4}")) {
                JOptionPane.showMessageDialog(null, "Ngày sinh không đúng định dạng dd/MM/yyyy.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                continue;
            }

            try {
                String changeInfoData = String.join("|", name, dob, gender);
                byte[] dataBytes = changeInfoData.getBytes(StandardCharsets.UTF_8);

                CommandAPDU changeInfoCommand = new CommandAPDU(0x00, 0x05, 0x00, 0x00, dataBytes);
                ResponseAPDU response = channel.transmit(changeInfoCommand);

                if (response.getSW1() == 0x90 && response.getSW2() == 0x00) {
                    responseField.setText("Thông tin đã được thay đổi thành công.");
                    JOptionPane.showMessageDialog(null, "Thông tin đã được thay đổi thành công.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    readCardData(); // Cập nhật dữ liệu
                    return;
                } else {
                    String errorMessage = String.format("Lỗi khi thay đổi thông tin. SW: %04X", response.getSW());
                    JOptionPane.showMessageDialog(null, errorMessage, "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Lỗi khi thay đổi thông tin: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // BALANCE: đọc -> long
    private long getBalanceFromCard() throws CardException {
        CommandAPDU cmd = new CommandAPDU(0x00, 0x17, 0x00, 0x00); // INS_GET_BALANCE = 0x17 -> sửa lại cho đúng
        ResponseAPDU resp = channel.transmit(cmd);
        if (resp.getSW() != 0x9000) throw new CardException("Get balance failed");
        String s = new String(resp.getData(), StandardCharsets.UTF_8).trim();
        if (s.isEmpty()) return 0;
        return Long.parseLong(s);
    }

    // MẶC ĐỊNH
    private void setBalanceToCard(long value) throws CardException {
        setBalanceToCard(value, 0x02);
    }

    private void setBalanceToCard(long value, int logType) throws CardException {
        String balanceStr = String.valueOf(value);
        byte[] balanceBytes = balanceStr.getBytes(StandardCharsets.UTF_8);

        long nowSec = System.currentTimeMillis() / 1000L;
        byte[] ts = new byte[] {
                (byte)((nowSec >> 24) & 0xFF),
                (byte)((nowSec >> 16) & 0xFF),
                (byte)((nowSec >> 8) & 0xFF),
                (byte)(nowSec & 0xFF)
        };

        // [balanceDigits][0x7C][4 bytes timestamp]
        byte[] data = new byte[balanceBytes.length + 1 + 4];
        System.arraycopy(balanceBytes, 0, data, 0, balanceBytes.length);
        data[balanceBytes.length] = (byte)0x7C; // delimiter
        System.arraycopy(ts, 0, data, balanceBytes.length + 1, 4);

        CommandAPDU cmd = new CommandAPDU(0x00, 0x16, logType & 0xFF, 0x00, data);
        ResponseAPDU resp = channel.transmit(cmd);
        if (resp.getSW() != 0x9000) throw new CardException("Set balance failed");
    }

    // POINTS: đọc
    private int getPointsFromCard() throws CardException {
        CommandAPDU cmd = new CommandAPDU(0x00, 0x13, 0x00, 0x00); // INS_GET_SODIEM
        ResponseAPDU resp = channel.transmit(cmd);
        if (resp.getSW() != 0x9000) throw new CardException("Get points failed");
        String s = new String(resp.getData(), StandardCharsets.UTF_8).trim();
        if (s.isEmpty()) return 0;
        return Integer.parseInt(s);
    }

    private void setPointsToCard(int value) throws CardException {
        byte[] data = String.valueOf(value).getBytes(StandardCharsets.UTF_8);
        CommandAPDU cmd = new CommandAPDU(0x00, 0x12, 0x00, 0x00, data); // P1 = 0, không log
        ResponseAPDU resp = channel.transmit(cmd);
        if (resp.getSW() != 0x9000) throw new CardException("Set points failed");
    }

    // TIER: đọc
    private int getTierFromCard() throws CardException {
        CommandAPDU cmd = new CommandAPDU(0x00, 0x14, 0x00, 0x00); // INS_GET_TIER
        ResponseAPDU resp = channel.transmit(cmd);
        if (resp.getSW() != 0x9000) throw new CardException("Get tier failed");
        return resp.getData()[0];
    }

    // TIER: ghi
    private void setTierOnCard(int tier) throws CardException {
        CommandAPDU cmd = new CommandAPDU(0x00, 0x1A, (byte) tier, 0x00); // INS_SET_TIER_PACK, P1 = tier
        ResponseAPDU resp = channel.transmit(cmd);
        if (resp.getSW() != 0x9000) throw new CardException("Set tier failed");
    }

    // VOUCHER: get level 0..5
    private int getVoucherLevel() throws CardException {
        CommandAPDU cmd = new CommandAPDU(0x00, 0x19, 0x00, 0x00); // INS_GET_VOUCHER
        ResponseAPDU resp = channel.transmit(cmd);
        if (resp.getSW() != 0x9000) throw new CardException("Get voucher failed");
        return resp.getData()[0] & 0xFF;
    }

    // VOUCHER: set level 0..5
    private void setVoucherLevel(int level) throws CardException {
        CommandAPDU cmd = new CommandAPDU(0x00, 0x18, level, 0x00); // INS_SET_VOUCHER
        ResponseAPDU resp = channel.transmit(cmd);
        if (resp.getSW() != 0x9000) throw new CardException("Set voucher failed");
    }

    private void topUpMoney() {
        if (!isConnected || channel == null) {
            responseField.setText("Bạn phải kết nối với thẻ trước!");
            return;
        }

        String input = JOptionPane.showInputDialog(this, "Nhập số tiền nạp (VNĐ):", "Nạp tiền", JOptionPane.PLAIN_MESSAGE);
        if (input == null) {
            responseField.setText("Đã hủy nạp tiền.");
            return;
        }

        input = input.trim();
        if (input.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Số tiền không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            long amount = Long.parseLong(input);
            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "Số tiền phải > 0!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            long current = getBalanceFromCard();
            long updated = current + amount;
            setBalanceToCard(updated); // P1 = 0x02 (nạp tiền)

            responseField.setText("Nạp tiền thành công. Số dư mới: " + updated + " VNĐ");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Số tiền không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        } catch (CardException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi thẻ: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static class Product {
        String name;
        long price; // VNĐ

        Product(String n, long p) {
            name = n;
            price = p;
        }
    }

    private Product[] products = new Product[]{
            new Product("Áo thun", 100_000L),
            new Product("Quần jean", 500_000L),
            new Product("Thắt lưng", 300_000L),
            new Product("Mũ", 400_000L),
            new Product("Găng tay", 200_000L),
            new Product("Giày sneaker", 1_500_000L)
    };

    private void openStore() {
        if (!isConnected || channel == null) {
            responseField.setText("Bạn phải kết nối với thẻ trước!");
            return;
        }

        String[] names = new String[products.length];
        for (int i = 0; i < products.length; i++) {
            names[i] = products[i].name + " - " + formatPrice(products[i].price);
        }

        JList<String> list = new JList<>(names);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        int option = JOptionPane.showConfirmDialog(
                this,
                new JScrollPane(list),
                "Cửa hàng",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (option != JOptionPane.OK_OPTION) {
            responseField.setText("Đã đóng cửa hàng.");
            return;
        }

        int idx = list.getSelectedIndex();
        if (idx < 0) {
            JOptionPane.showMessageDialog(this, "Chưa chọn sản phẩm!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            handlePurchase(products[idx]);
        } catch (CardException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi thẻ: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handlePurchase(Product p) throws CardException {
        long balance = getBalanceFromCard();
        int tier = getTierFromCard();   // 0..4
        int voucherLv = getVoucherLevel();   // 0..5

        // Giảm theo tier: mỗi cấp 5%
        double tierDiscount = tier * 0.05; // Basic=0, Silver=0.05,...
        if (tierDiscount > 0.20) tierDiscount = 0.20; // tối đa 20%

        double voucherDiscount = 0.0;
        switch (voucherLv) {
            case 1:
                voucherDiscount = 0.10;
                break;
            case 2:
                voucherDiscount = 0.15;
                break;
            case 3:
                voucherDiscount = 0.20;
                break;
            case 4:
                voucherDiscount = 0.25;
                break;
            case 5:
                voucherDiscount = 0.30;
                break;
            default:
                voucherDiscount = 0.0;
                break; // 0 = không có
        }

        double totalDiscount = tierDiscount + voucherDiscount;
        if (totalDiscount > 0.7) totalDiscount = 0.7;

        long finalPrice = (long) Math.round(p.price * (1.0 - totalDiscount));

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Giá gốc: " + p.price +
                        "\nGiảm giá: " + (int) (totalDiscount * 100) + "%" +
                        "\nGiá thanh toán: " + finalPrice +
                        "\nSố dư hiện tại: " + balance +
                        "\n\nXác nhận mua?",
                "Xác nhận mua",
                JOptionPane.OK_CANCEL_OPTION
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

        // Trừ tiền, cộng điểm:
        long newBalance = balance - finalPrice;
        // 0x03 = LOG_PURCHASE
        setBalanceToCard(newBalance, 0x03);

        int currentPoints = getPointsFromCard();
        int earned = (int) (p.price / 500_000) * 100;
        int newPoints = currentPoints + earned;
        // 0x06 = LOG_POINT (tích điểm)
        setPointsToCard(newPoints);

        // Voucher dùng xong thì xóa
        if (voucherLv > 0) {
            setVoucherLevel(0);
        }

        responseField.setText("Mua thành công " + p.name +
                ". Số dư còn: " + newBalance + " VNĐ, điểm: " + newPoints);
        JOptionPane.showMessageDialog(this, "Mua thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exchangePoints() {
        if (!isConnected || channel == null) {
            responseField.setText("Bạn phải kết nối với thẻ trước!");
            return;
        }

        // Các mức voucher
        String[] saleOptions = {
                "Voucher giảm 10% (100 điểm)",
                "Voucher giảm 15% (200 điểm)",
                "Voucher giảm 20% (300 điểm)",
                "Voucher giảm 25% (500 điểm)",
                "Voucher giảm 30% (1000 điểm)"
        };

        int[] costPoints = {100, 200, 300, 500, 1000};
        int[] voucherLevels = {1, 2, 3, 4, 5}; // tương ứng 10%,15%,20%,25%,30%

        // Tạo Radio button group
        JPanel panel = new JPanel(new GridLayout(saleOptions.length, 1));
        ButtonGroup group = new ButtonGroup();
        JRadioButton[] radios = new JRadioButton[saleOptions.length];

        for (int i = 0; i < saleOptions.length; i++) {
            radios[i] = new JRadioButton(saleOptions[i]);
            group.add(radios[i]);
            panel.add(radios[i]);
        }

        int confirm = JOptionPane.showConfirmDialog(
                null,
                panel,
                "Chọn voucher muốn đổi",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (confirm != JOptionPane.OK_OPTION) {
            responseField.setText("Đã hủy đổi điểm.");
            return;
        }

        // Xác định voucher được chọn
        int selected = -1;
        for (int i = 0; i < radios.length; i++) {
            if (radios[i].isSelected()) {
                selected = i;
                break;
            }
        }

        if (selected == -1) {
            responseField.setText("Bạn chưa chọn voucher nào.");
            return;
        }

        int cost = costPoints[selected];

        try {
            int currentPoints = getPointsFromCard();
            int newPoints = currentPoints - cost;

            if (newPoints < 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Điểm của bạn không đủ (" + currentPoints + " điểm)\n"
                                + "Bạn cần " + cost + " điểm.",
                        "Không đủ điểm",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            // Trừ điểm (log là đổi điểm -> voucher)
            setPointsToCard(newPoints);

            // Ghi loại voucher lên thẻ (1..5)
            setVoucherLevel(voucherLevels[selected]);

            responseField.setText("Đổi voucher thành công! Điểm còn: " + newPoints);
            JOptionPane.showMessageDialog(
                    this,
                    "Bạn đã đổi được voucher giảm giá cho lần mua tiếp theo!",
                    "Thành công",
                    JOptionPane.INFORMATION_MESSAGE
            );

            readCardData();

        } catch (Exception e) {
            responseField.setText("Lỗi đổi điểm: " + e.getMessage());
        }
    }


    private static class TierPack {
        String name;
        int tier;
        long price;

        TierPack(String n, int t, long p) {
            name = n;
            tier = t;
            price = p;
        }
    }

    private TierPack[] tierPacks = new TierPack[]{
            new TierPack("Bạc (-5%)", 1, 300_000),
            new TierPack("Vàng (-10%)", 2, 700_000),
            new TierPack("Bạch Kim (-15%)", 3, 1_200_000),
            new TierPack("Kim Cương (-20%)", 4, 2_000_000)
    };

    private void openUpgradeShop() {
        if (!isConnected || channel == null) {
            responseField.setText("Bạn phải kết nối với thẻ trước!");
            return;
        }

        String[] options = new String[tierPacks.length];
        for (int i = 0; i < tierPacks.length; i++) {
            options[i] = tierPacks[i].name + " - " + tierPacks[i].price + " VNĐ";
        }

        JList<String> list = new JList<>(options);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        int opt = JOptionPane.showConfirmDialog(
                this,
                new JScrollPane(list),
                "Nâng hạng",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (opt != JOptionPane.OK_OPTION) {
            responseField.setText("Hủy nâng hạng.");
            return;
        }

        int idx = list.getSelectedIndex();
        if (idx < 0) {
            JOptionPane.showMessageDialog(this, "Chưa chọn gói!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

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
        // 0x05 = LOG_UPGRADE
            setBalanceToCard(newBalance, 0x05);

        // ===== TÍNH THỜI GIAN HẾT HẠN (30 NGÀY) =====
            long now = System.currentTimeMillis() / 1000;     // giây
            long expire = now + 30L * 24 * 60 * 60;           // cộng 30 ngày

            byte[] expiryBytes = new byte[] {
                    (byte)((expire >> 24) & 0xFF),
                    (byte)((expire >> 16) & 0xFF),
                    (byte)((expire >> 8) & 0xFF),
                    (byte)(expire & 0xFF)
            };

            // ===== GỬI XUỐNG THẺ =====
            CommandAPDU setTierCmd = new CommandAPDU(
                    0x00,
                    0x1A,         // INS_SET_TIER_PACK
                    pack.tier,    // P1
                    0x00,         // P2
                    expiryBytes   // dữ liệu
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
            JOptionPane.showMessageDialog(null, "Bạn phải kết nối với thẻ trước!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Form nhập PIN để mở khóa
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JPasswordField passwordField = new JPasswordField();

        panel.add(new JLabel("Nhập mã PIN để mở khóa:"));
        panel.add(passwordField);

        int option = JOptionPane.showConfirmDialog(
                null,
                panel,
                "Mở khóa thẻ",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (option != JOptionPane.OK_OPTION) {
            responseField.setText("Hủy mở khóa thẻ.");
            return;
        }

        String pin = new String(passwordField.getPassword()).trim();
        if (!pin.matches("\\d{6}")) {
            JOptionPane.showMessageDialog(null, "PIN phải gồm 6 chữ số!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            byte[] pinBytes = pin.getBytes(StandardCharsets.UTF_8);
            // Gửi PIN để mở khóa
            CommandAPDU cmd = new CommandAPDU(0x00, 0x03, 0x00, 0x00, pinBytes);
            ResponseAPDU resp = channel.transmit(cmd);

            if (resp.getSW() != 0x9000) {
                JOptionPane.showMessageDialog(
                        null,
                        "Lỗi từ thẻ! SW=" + Integer.toHexString(resp.getSW()),
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            byte[] data = resp.getData();
            byte status = data[0];  // 1=thành công, 0=sai PIN, 2=đã khóa

            if (status == 1) {
                JOptionPane.showMessageDialog(null, "Mở khóa thẻ thành công!", "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);
                responseField.setText("Mở khóa thẻ thành công!");
            } else if (status == 0) {
                JOptionPane.showMessageDialog(null, "PIN không đúng! Không thể mở khóa.", "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                responseField.setText("PIN mở khóa sai.");
            } else {
                JOptionPane.showMessageDialog(null, "Thẻ vẫn đang bị khóa!", "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                responseField.setText("Thẻ vẫn khóa.");
            }

        } catch (Exception e) {
            responseField.setText("Lỗi mở khóa thẻ: " + e.getMessage());
        }
    }

    // Hàm chọn file ảnh từ hệ thống
    private byte[] chooseAndReadFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn ảnh đại diện");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Image files", "jpg", "png", "gif", "bmp"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            filePathField.setText(selectedFile.getAbsolutePath());
            try {
                // Đọc toàn bộ dữ liệu của file
                byte[] fileData = Files.readAllBytes(selectedFile.toPath());

                // Hiển thị ảnh trên JLabel
                ImageIcon imageIcon = new ImageIcon(selectedFile.getAbsolutePath());
                Image image = imageIcon.getImage().getScaledInstance(imageLabel.getWidth(), imageLabel.getHeight(), Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(image)); // Đặt ảnh vào JLabel

                return fileData; // Trả về dữ liệu file
            } catch (IOException e) {
                responseField.setText("Lỗi khi đọc file: " + e.getMessage());
            }
        }
        return null; // Trả về null nếu không đọc được file hoặc hủy chọn
    }


    //Hàm gửi file đến applet
    private void sendImageData(byte[] fileData) {
        int maxDataLength = 255; // Kích thước tối đa của dữ liệu trong một APDU

        try {
            for (int offset = 0; offset < fileData.length; offset += maxDataLength) {
                int length = Math.min(maxDataLength, fileData.length - offset);
                byte[] chunk = new byte[length];
                System.arraycopy(fileData, offset, chunk, 0, length);

                // Xác định P1: 0x00 nếu còn khối, 0x01 nếu là khối cuối cùng
                byte p1 = (offset + length >= fileData.length) ? (byte) 0x01 : (byte) 0x00;

                // Tạo và gửi CommandAPDU
                CommandAPDU sendImage = new CommandAPDU(0x00, 0x08, p1, 0x00, chunk);
                ResponseAPDU response = channel.transmit(sendImage);

                // Kiểm tra phản hồi
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
            // Gửi lệnh để nhận dữ liệu ảnh từ thẻ
            CommandAPDU sendImage = new CommandAPDU(0x00, 0x09, 0x00, 0x00);
            ResponseAPDU response = channel.transmit(sendImage);

            byte[] responseData = response.getData();
            if (responseData != null) {
                // Chuyển đổi mảng byte thành ImageIcon
                ImageIcon imageIcon = new ImageIcon(responseData);

                // Lấy kích thước của JLabel
                int labelWidth = imageInfoLabel.getWidth();
                int labelHeight = imageInfoLabel.getHeight();

                // Nếu JLabel chưa có kích thước (trong trường hợp chưa được vẽ), ta lấy một giá trị mặc định hoặc
                // thay đổi khi JLabel đã có kích thước cụ thể.
                if (labelWidth <= 0 || labelHeight <= 0) {
                    // Cung cấp kích thước mặc định cho label nếu chưa có kích thước
                    labelWidth = 100;  // Ví dụ kích thước mặc định cho chiều rộng
                    labelHeight = 150; // Ví dụ kích thước mặc định cho chiều cao
                }

                // Lấy ảnh và thay đổi kích thước sao cho vừa với JLabel
                Image img = imageIcon.getImage();
                Image scaledImage = img.getScaledInstance(labelWidth, labelHeight, Image.SCALE_SMOOTH);

                // Đặt icon vào JLabel với ảnh đã thay đổi kích thước
                imageInfoLabel.setIcon(new ImageIcon(scaledImage));

                // Đảm bảo JLabel được cập nhật lại
                imageInfoLabel.revalidate();
                imageInfoLabel.repaint();
            } else {
                responseField.setText("Lỗi: Dữ liệu hình ảnh không hợp lệ.");
            }
        } catch (Exception e) {
            responseField.setText("Lỗi: " + e.getMessage());
        }
    }

    private void getPublicKey() {
        try {
            // Lấy Modulus (N)
            byte[] modulusCommand = new byte[]{(byte) 0x00, (byte) 0x10, (byte) 0x01, (byte) 0x00};
            ResponseAPDU modulusResponse = channel.transmit(new CommandAPDU(modulusCommand));

            if (modulusResponse.getSW() == 0x9000) {
                String modulus = bytesToHex(modulusResponse.getData());

                responseField.setText("Modulus: " + modulus);
            } else {
                responseField.setText("Error retrieving public key.");
            }
        } catch (Exception ex) {
            responseField.setText("Error retrieving public key.");
            ex.printStackTrace();
        }
    }

    private void signData() {
        try {
            // Nhập dữ liệu cần ký
            String dataToSign = JOptionPane.showInputDialog("Enter data to sign:");
            String pinInput = JOptionPane.showInputDialog("Enter PIN:");

            if (dataToSign == null || dataToSign.isEmpty() || pinInput == null || pinInput.isEmpty()) {
                responseField.setText("No data or PIN provided.");
                return;
            }

            // Chuyển đổi dữ liệu và PIN thành byte
            byte[] dataBytes = dataToSign.getBytes(StandardCharsets.UTF_8);
            byte[] pinBytes = pinInput.getBytes(StandardCharsets.UTF_8);

            // Ghép mã PIN và dữ liệu bằng ký tự phân tách '|'
            ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
            dataStream.write(pinBytes);
            dataStream.write((byte) 0x7C); // Ký tự phân tách '|'
            dataStream.write(dataBytes);
            byte[] combinedData = dataStream.toByteArray();

            // Tạo lệnh APDU để gửi dữ liệu tới thẻ
            ByteArrayOutputStream commandStream = new ByteArrayOutputStream();
            commandStream.write((byte) 0x00); // CLA
            commandStream.write((byte) 0x11); // INS (Mã lệnh cho signHandle())
            commandStream.write((byte) 0x00); // P1
            commandStream.write((byte) 0x00); // P2
            commandStream.write((byte) combinedData.length); // Lc (Độ dài dữ liệu)
            commandStream.write(combinedData); // Dữ liệu ghép

            // Truyền lệnh tới thẻ và nhận phản hồi
            ResponseAPDU response = channel.transmit(new CommandAPDU(commandStream.toByteArray()));

            if (response.getSW() == 0x9000) {
                // Nếu phản hồi thành công, chuyển đổi chữ ký thành dạng hex
                String signature = bytesToHex(response.getData());
                responseField.setText("Signature: " + signature);
            } else {
                // Nếu có lỗi, hiển thị mã lỗi
                responseField.setText("Error: " + Integer.toHexString(response.getSW()));
            }
        } catch (Exception ex) {
            responseField.setText("Error signing data.");
            ex.printStackTrace();
        }
    }

//    private void viewTransactionLogs() {
//        if (!isConnected || channel == null) {
//            responseField.setText("Bạn phải kết nối với thẻ trước!");
//            return;
//        }
//
//        String[] columns = {"STT", "Loại giao dịch", "Số thay đổi", "Số dư sau giao dịch"};
//        DefaultTableModel model = new DefaultTableModel(columns, 0);
//
//        try {
//            final int LOG_ENTRY_SIZE = 12;
//
//            java.util.List<Integer> deltas = new ArrayList<>();
//            java.util.List<String> types = new ArrayList<>();
//
//            // 1. Đọc log từ thẻ: index 0 = mới nhất
//            for (int i = 0; i < 5; i++) {
//                CommandAPDU cmd = new CommandAPDU(0x00, 0x15, i, 0x00);
//                ResponseAPDU resp = channel.transmit(cmd);
//                if (resp.getSW() != 0x9000) break;
//
//                byte[] raw = resp.getData();
//                if (raw.length < LOG_ENTRY_SIZE) continue;
//
//                byte type = raw[0];
//                char sign = (char) raw[1];
//
//                // chỉ nhận log TIỀN
//                if (type != 0x02 && type != 0x03 && type != 0x05)
//                    continue;
//
//                String digits = new String(raw, 2, LOG_ENTRY_SIZE - 2).replace("\u0000", "");
//                digits = digits.replaceFirst("^0+(?!$)", "");
//                if (digits.equals("")) digits = "0";
//
//                int delta = Integer.parseInt(digits);
//                if (sign == '-') delta = -delta;
//
//                String typeName = switch (type) {
//                    case 0x02 -> "Nạp tiền";
//                    case 0x03 -> "Mua hàng";
//                    case 0x05 -> "Nâng hạng";
//                    default -> "Khác";
//                };
//
//                deltas.add(delta);
//                types.add(typeName);
//            }
//
//            // Không có log
//            if (deltas.isEmpty()) {
//                JOptionPane.showMessageDialog(null, "Không có giao dịch.", "Thông báo", JOptionPane.PLAIN_MESSAGE);
//                return;
//            }
//
//            // 2. Lấy số dư hiện tại
//            long currentBalance = getBalanceFromCard();
//
//            // 3. Tính lại số dư theo thứ tự CŨ -> MỚI
//            ArrayList<Object[]> rebuiltRows = new ArrayList<>();
//            long runningBalance = currentBalance;
//
//            // đảo thứ tự log: index cuối cùng là giao dịch CŨ nhất
//            for (int i = 0; i < deltas.size(); i++) {
//                runningBalance -= deltas.get(i);  // khôi phục về số dư trước giao dịch
//            }
//
//            // chạy lại từ giao dịch CŨ đến MỚI
//            for (int i = deltas.size() - 1; i >= 0; i--) {
//                runningBalance += deltas.get(i);
//                rebuiltRows.add(new Object[]{
//                        null,
//                        types.get(i),
//                        formatMoneyDelta(deltas.get(i)),
//                        formatMoneyNoSign(runningBalance)
//                });
//            }
//
//            // 4. Hiển thị MỚI nhất lên đầu
//            int stt = 1;
//            for (int i = rebuiltRows.size() - 1; i >= 0; i--) {
//                Object[] row = rebuiltRows.get(i);
//                row[0] = stt++;
//                model.addRow(row);
//            }
//
//            JTable table = new JTable(model);
//            JOptionPane.showMessageDialog(null, new JScrollPane(table), "Lịch sử giao dịch", JOptionPane.PLAIN_MESSAGE);
//
//        } catch (Exception e) {
//            responseField.setText("Lỗi xem log: " + e.getMessage());
//        }
//    }

    private void viewTransactionLogs() {
        if (!isConnected || channel == null) {
            responseField.setText("Bạn phải kết nối với thẻ trước!");
            return;
        }

        String[] columns = {"STT", "Thời gian", "Loại GD", "Số thay đổi", "Số dư sau GD"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);

        try {
            final int LOG_ENTRY_SIZE = 16;

            List<Integer> deltas = new ArrayList<>();
            List<String> types = new ArrayList<>();
            List<Long> times = new ArrayList<>();

            // ====== 1. Đọc log từ thẻ =======
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

                // amount: 10 byte
                String digits = new String(raw, 2, 10).replace("\u0000", "");
                digits = digits.replaceFirst("^0+(?!$)", "");
                if (digits.equals("")) digits = "0";

                int delta = Integer.parseInt(digits);
                if (sign == '-') delta = -delta;

                // timestamp
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
                JOptionPane.showMessageDialog(null, "Không có giao dịch.", "Thông báo", JOptionPane.PLAIN_MESSAGE);
                return;
            }

            // ===== 2. Lấy số dư hiện tại =====
            long currentBalance = getBalanceFromCard();

            // ===== 3. Tính lại số dư theo thứ tự cũ -> mới =====
            List<Long> balances = new ArrayList<>();

            long runningBalance = currentBalance;

            for (int i = 0; i < deltas.size(); i++) {
                runningBalance -= deltas.get(i);
            }

            for (int i = deltas.size() - 1; i >= 0; i--) {
                runningBalance += deltas.get(i);
                balances.add(runningBalance);
            }

            // đảo lại: newest trên đầu
            Collections.reverse(balances);

            // ===== 4. Thêm vào bảng =====
            int stt = 1;
            for (int i = 0; i < deltas.size(); i++) {

                String timeStr = new java.text.SimpleDateFormat("HH:mm:ss dd/MM/yyyy")
                        .format(new java.util.Date(times.get(i) * 1000));

                model.addRow(new Object[]{
                        stt++,
                        timeStr,
                        types.get(i),
                        formatMoneyDelta(deltas.get(i)),
                        formatMoneyNoSign(balances.get(i))
                });
            }

            JTable table = new JTable(model);
            JOptionPane.showMessageDialog(null, new JScrollPane(table), "Lịch sử giao dịch", JOptionPane.PLAIN_MESSAGE);

        } catch (Exception e) {
            responseField.setText("Lỗi xem log: " + e.getMessage());
        }
    }


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
            // Nếu độ dài lẻ, thêm '0' vào đầu
            s = "0" + s;
            len = s.length();
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString().toUpperCase();
    }

}