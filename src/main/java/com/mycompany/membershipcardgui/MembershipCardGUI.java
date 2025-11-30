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
import java.util.List;

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
    private JButton addPointsButton = new JButton("Tích điểm");
    private JButton exchangePointsButton = new JButton("Đổi điểm");
    private JButton unblockCartButton = new JButton("Mở khoá thẻ");
    private JButton verifybtn = new JButton("Kiểm tra pin");
    private JButton viewLogButton = new JButton("Xem lịch sử giao dịch");

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
        styleButton(addPointsButton, new Color(230, 126, 34));
        styleButton(exchangePointsButton, new Color(155, 89, 182));
        styleButton(unblockCartButton, new Color(41, 128, 185));
        styleButton(getPublicKeyButton, new Color(22, 160, 133));
        styleButton(signDataButton, new Color(127, 140, 141));
        styleButton(viewLogButton, new Color(52, 73, 94));

        // ❗ KHÔNG thêm editButton và changePinButton vào panel này
        // Hai nút đó chỉ xuất hiện trong readCardData()

        memberPanel.add(initCardButton);
        memberPanel.add(readCardButton);
        memberPanel.add(addPointsButton);
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

        addPointsButton.addActionListener(e -> addPoints());
        exchangePointsButton.addActionListener(e -> exchangePoints());
        unblockCartButton.addActionListener(e -> unblockCard());
        verifybtn.addActionListener(e -> verifyPin());

        getPublicKeyButton.addActionListener(e -> getPublicKey());
        signDataButton.addActionListener(e -> signData());
        viewLogButton.addActionListener(e -> viewTransactionLogs());

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
            while (true) { // Vòng lặp kiểm tra mã PIN
                JPanel pinPanel = new JPanel(new GridLayout(3, 2, 5, 5));
                JPasswordField passwordField = new JPasswordField();
                pinPanel.add(new JLabel("Nhập mã PIN:"));
                pinPanel.add(passwordField);

                int option = JOptionPane.showConfirmDialog(null, pinPanel, "Xác thực mã PIN",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

                if (option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION) {
                    responseField.setText("Bạn đã hủy nhập mã PIN.");
                    return false;
                }

                String pin = new String(passwordField.getPassword()).trim();
                if (pin.isEmpty()) {
                    responseField.setText("Bạn chưa nhập mã PIN!");
                    JOptionPane.showMessageDialog(null, "Bạn chưa nhập mã PIN!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    continue; // Lặp lại hộp thoại
                }

                // Gửi lệnh kiểm tra mã PIN
                byte[] pinBytes = pin.getBytes(StandardCharsets.UTF_8);
                CommandAPDU verifyPinCommand = new CommandAPDU(0x00, 0x02, 0x00, 0x00, pinBytes);
                ResponseAPDU verifyResponse = channel.transmit(verifyPinCommand);
                byte[] responseBytes = verifyResponse.getBytes();
                int sw24 = ((responseBytes[responseBytes.length - 3] & 0xFF) << 16)
                        | ((responseBytes[responseBytes.length - 2] & 0xFF) << 8)
                        | (responseBytes[responseBytes.length - 1] & 0xFF);

                if (sw24 == 0x019000) {
                    responseField.setText("Xác thực mã PIN thành công!");
//                    JOptionPane.showMessageDialog(null, "Xác thực thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    return true;
                } else if (sw24 == 0x009000) {
                    responseField.setText("Mã PIN không đúng!");
                    JOptionPane.showMessageDialog(null, "Mã PIN không đúng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                } else {
                    responseField.setText("Thẻ đã bị khóa.");
                    JOptionPane.showMessageDialog(null, "Thẻ đã bị khóa!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        } catch (CardException ex) {
            responseField.setText("Lỗi xác thực mã PIN: " + ex.getMessage());
            JOptionPane.showMessageDialog(null, "Lỗi xác thực mã PIN: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
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

        // Các thành phần giao diện
        imageInfoLabel = new JLabel();
        imageInfoLabel.setPreferredSize(new Dimension(100, 150));
        imageInfoLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        // Căn chỉnh ảnh
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2; // Mở rộng ảnh ra 2 cột
        gbc.fill = GridBagConstraints.CENTER;
        infoPanel.add(imageInfoLabel, gbc);

        // Căn chỉnh các trường thông tin
        JLabel maKHLabel = new JLabel("Mã KH:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        infoPanel.add(maKHLabel, gbc);

        getMaKH = new JTextField();
        getMaKH.setEditable(false);
        gbc.gridx = 1;
        infoPanel.add(getMaKH, gbc);

        JLabel nameLabel = new JLabel("Họ và Tên:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        infoPanel.add(nameLabel, gbc);

        getName = new JTextField();
        getName.setEditable(false);
        gbc.gridx = 1;
        infoPanel.add(getName, gbc);

        JLabel dobLabel = new JLabel("Ngày Sinh (dd/MM/yyyy):");
        gbc.gridx = 0;
        gbc.gridy = 3;
        infoPanel.add(dobLabel, gbc);

        getDob = new JTextField();
        getDob.setEditable(false);
        gbc.gridx = 1;
        infoPanel.add(getDob, gbc);

        JLabel genderLabel = new JLabel("Giới Tính:");
        gbc.gridx = 0;
        gbc.gridy = 4;
        infoPanel.add(genderLabel, gbc);

        getGender = new JTextField();
        getGender.setEditable(false);
        gbc.gridx = 1;
        infoPanel.add(getGender, gbc);

        JLabel pointsLabel = new JLabel("Tích điểm:");
        gbc.gridx = 0;
        gbc.gridy = 5;
        infoPanel.add(pointsLabel, gbc);

        getPoints = new JTextField();
        getPoints.setEditable(false);
        gbc.gridx = 1;
        infoPanel.add(getPoints, gbc);

        JLabel tierLabel = new JLabel("Hạng thành viên:");
        gbc.gridx = 0;
        gbc.gridy = 6;
        infoPanel.add(tierLabel, gbc);

        JTextField tierField = new JTextField();
        tierField.setEditable(false);
        gbc.gridx = 1;
        infoPanel.add(tierField, gbc);


        // Tạo một JPanel để chứa hai nút ngang hàng
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10)); // Căn giữa hai nút
        buttonPanel.add(changePinButton);
        buttonPanel.add(editButton);

        // Căn chỉnh các nút vào vị trí cuối cùng
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2; // Hai nút sẽ chiếm 2 cột
        gbc.fill = GridBagConstraints.CENTER;
        infoPanel.add(buttonPanel, gbc);

//        try {
//            // Gửi lệnh đọc dữ liệu
//            CommandAPDU readCommand = new CommandAPDU(0x00, 0x06, 0x00, 0x00, 256);
//            ResponseAPDU response = channel.transmit(readCommand);
//
//            if (response.getSW() == 0x9000) { // Kiểm tra trạng thái SW
//                byte[] data = response.getData();
//                String rawData = new String(data, StandardCharsets.UTF_8).trim();
//
//                // Tách dữ liệu bằng ký tự '|'
//                String[] fields = rawData.split("\\|");
//                if (fields.length >= 5) { // Đảm bảo có đủ trường (cập nhật cho điểm)
//                    String maKH = fields[0];
//                    String fullName = fields[1];
//                    String birthDate = fields[2];
//                    String gender = fields[3];
//                    String points = fields[4];
//
//                    getMaKH.setText(maKH);
//                    getName.setText(fullName);
//                    getDob.setText(birthDate);
//                    getGender.setText(gender);
//                    getPoints.setText(points);
//
//                    // LẤY TIER TỪ THẺ
//                    CommandAPDU getTierCmd = new CommandAPDU(0x00, 0x14, 0x00, 0x00);
//                    ResponseAPDU tierResp = channel.transmit(getTierCmd);
//
//                    if (tierResp.getSW() == 0x9000) {
//                        byte tierValue = tierResp.getData()[0];
//                        String tierName;
//
//                        switch (tierValue) {
//                            case 0: tierName = "Basic"; break;
//                            case 1: tierName = "Silver"; break;
//                            case 2: tierName = "Gold"; break;
//                            case 3: tierName = "Platinum"; break;
//                            case 4: tierName = "Diamond"; break;
//                            default: tierName = "Unknown"; break;
//                        }
//
//                        tierField.setText(tierName);
//                    }
//
//                    // Cập nhật ảnh
//                    getImageFile(imageInfoLabel);
//
//                    responseField.setText("Đọc dữ liệu thẻ thành công!");
//                    JOptionPane.showConfirmDialog(null, infoPanel, "Thông tin thẻ", JOptionPane.CLOSED_OPTION, JOptionPane.PLAIN_MESSAGE);
//                } else {
//                    responseField.setText("Dữ liệu không đầy đủ hoặc sai định dạng!");
//                }
//            } else {
//                responseField.setText("Lỗi từ thẻ: SW=" + Integer.toHexString(response.getSW()));
//            }
//        } catch (CardException ex) {
//            responseField.setText("Lỗi đọc thẻ: " + ex.getMessage());
//        }
        try {
            // Gửi lệnh đọc dữ liệu - KHÔNG set Le = 256 nữa
            CommandAPDU readCommand = new CommandAPDU(0x00, 0x06, 0x00, 0x00);
            ResponseAPDU response = channel.transmit(readCommand);

            if (response.getSW() == 0x9000) {

                // Lấy đúng số byte thẻ trả về (không lấy rác)
                byte[] data = response.getData();

                // Loại bỏ các byte 0x00 hoặc rác AES cuối buffer
                int realLen = data.length;
                while (realLen > 0 && data[realLen - 1] == 0x00) {
                    realLen--;
                }

                // Chuyển thành chuỗi sạch
                String rawData = new String(data, 0, realLen, StandardCharsets.UTF_8);

                // Tách theo ký tự '|'
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

                    // LẤY TIER TỪ THẺ
                    CommandAPDU getTierCmd = new CommandAPDU(0x00, 0x14, 0x00, 0x00);
                    ResponseAPDU tierResp = channel.transmit(getTierCmd);

                    if (tierResp.getSW() == 0x9000) {
                        byte tierValue = tierResp.getData()[0];
                        String tierName;

                        switch (tierValue) {
                            case 0: tierName = "Basic"; break;
                            case 1: tierName = "Silver"; break;
                            case 2: tierName = "Gold"; break;
                            case 3: tierName = "Platinum"; break;
                            case 4: tierName = "Diamond"; break;
                            default: tierName = "Unknown"; break;
                        }

                        tierField.setText(tierName);
                    }

                    // LẤY ẢNH (sau khi info sạch)
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

            // Kiểm tra dữ liệu nhập
            if (oldPin.isEmpty() || newPin.isEmpty() || confirmPin.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Vui lòng nhập đầy đủ thông tin!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                continue;
            }
            if (newPin.length() > 6) {
                JOptionPane.showMessageDialog(null, "Mã PIN mới không được quá 6 ký tự.", "Lỗi", JOptionPane.ERROR_MESSAGE);
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

    private void addPoints() {
        if (!isConnected || channel == null) {
            responseField.setText("Bạn phải kết nối với thẻ trước!");
            return;
        }

        // Hiển thị danh sách lựa chọn mức giá với checkbox
        String[] priceOptions = {"Dưới 100k(Cộng 10 điểm)", "100k - 200k(Cộng 20 điểm)", "200k - 300k(Cộng 30 điểm)", "300k-500k(Cộng 50 điểm)", "Trên 500k(Cộng 100 điểm)"};
        int[] pointsArray = {10, 20, 30, 50, 100}; // Điểm tương ứng với từng mức giá

        JPanel panel = new JPanel(new GridLayout(priceOptions.length, 1));
        JCheckBox[] checkBoxes = new JCheckBox[priceOptions.length];

        for (int i = 0; i < priceOptions.length; i++) {
            checkBoxes[i] = new JCheckBox(priceOptions[i]);
            panel.add(checkBoxes[i]);
        }

        int result = JOptionPane.showConfirmDialog(
                null,
                panel,
                "Chọn mức giá:",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            responseField.setText("Bạn đã hủy việc cộng điểm.");
            return;
        }

        // Tính tổng điểm cần cộng dựa trên lựa chọn
        int totalPointsToAdd = 0;
        for (int i = 0; i < checkBoxes.length; i++) {
            if (checkBoxes[i].isSelected()) {
                totalPointsToAdd += pointsArray[i];
            }
        }

        if (totalPointsToAdd == 0) {
            responseField.setText("Bạn chưa chọn mức giá nào.");
            return;
        }

        try {
            // Gửi lệnh APDU 0x13 để lấy điểm hiện tại
            CommandAPDU getPointsCommand = new CommandAPDU(0x00, 0x13, 0x00, 0x00);
            ResponseAPDU response = channel.transmit(getPointsCommand);

            if (response.getSW1() == 0x90 && response.getSW2() == 0x00) {
                // Lấy điểm hiện tại từ dữ liệu phản hồi
                byte[] data = response.getData();
                int currentPoints = Integer.parseInt(new String(data, StandardCharsets.UTF_8).trim());

                // Cộng điểm mới vào điểm hiện tại
                int newPoints = currentPoints + totalPointsToAdd;

                if (newPoints >= 0) {
                    // Gửi lệnh APDU 0x12 để cập nhật điểm
                    byte[] newPointsBytes = String.valueOf(newPoints).getBytes(StandardCharsets.UTF_8);
                    CommandAPDU updatePointsCommand = new CommandAPDU(0x00, 0x12, 0x00, 0x00, newPointsBytes);
                    ResponseAPDU updateResponse = channel.transmit(updatePointsCommand);

                    if (updateResponse.getSW1() == 0x90 && updateResponse.getSW2() == 0x00) {
                        responseField.setText("Cộng điểm thành công. Điểm hiện tại: " + newPoints);
                        readCardData();
                    } else {
                        responseField.setText("Lỗi khi cập nhật điểm. SW: " + Integer.toHexString(updateResponse.getSW()));
                    }
                } else {
                    responseField.setText("Số điểm không đủ để trừ.");
                }

            } else {
                responseField.setText("Lỗi khi lấy điểm hiện tại. SW: " + Integer.toHexString(response.getSW()));
            }
        } catch (NumberFormatException e) {
            responseField.setText("Lỗi định dạng dữ liệu điểm.");
        } catch (Exception e) {
            responseField.setText("Lỗi khi thực hiện lệnh: " + e.getMessage());
        }
    }

    private void exchangePoints() {
        if (!isConnected || channel == null) {
            responseField.setText("Bạn phải kết nối với thẻ trước!");
            return;
        }

        // Hiển thị danh sách lựa chọn mức giá với checkbox
        String[] saleOptions = {"Sale 10% (100 điểm)", "Sale 15% (200 điểm)", "Sale 20% (300 điểm)", "Sale 25% (500 điểm)", "Sale 30% (1000 điểm)"};
        int[] pointsArray = {-100, -200, -300, -500, -1000}; // Điểm âm tương ứng với từng mức giá

        JPanel exchangePanel = new JPanel(new GridLayout(saleOptions.length, 1));
        JCheckBox[] checkBoxes = new JCheckBox[saleOptions.length];

        for (int i = 0; i < saleOptions.length; i++) {
            checkBoxes[i] = new JCheckBox(saleOptions[i]);
            exchangePanel.add(checkBoxes[i]);
        }

        int result = JOptionPane.showConfirmDialog(
                null,
                exchangePanel,
                "Chọn mức giá:",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            responseField.setText("Bạn đã hủy việc trừ điểm.");
            return;
        }

        // Tính tổng điểm cần trừ dựa trên lựa chọn
        int totalPointsToSubtract = 0;
        for (int i = 0; i < checkBoxes.length; i++) {
            if (checkBoxes[i].isSelected()) {
                totalPointsToSubtract += pointsArray[i];
            }
        }

        if (totalPointsToSubtract == 0) {
            responseField.setText("Bạn chưa chọn mức giá nào.");
            return;
        }

        try {
            // Gửi lệnh APDU 0x13 để lấy điểm hiện tại
            CommandAPDU getPointsCommand = new CommandAPDU(0x00, 0x13, 0x00, 0x00);
            ResponseAPDU response = channel.transmit(getPointsCommand);

            if (response.getSW1() == 0x90 && response.getSW2() == 0x00) {
                // Lấy điểm hiện tại từ dữ liệu phản hồi
                byte[] data = response.getData();
                int currentPoints = Integer.parseInt(new String(data, StandardCharsets.UTF_8).trim());

                // Trừ điểm mới từ điểm hiện tại
                int newPoints = currentPoints + totalPointsToSubtract;

                if (newPoints >= 0) {
                    // Gửi lệnh APDU 0x12 để cập nhật điểm
                    byte[] newPointsBytes = String.valueOf(newPoints).getBytes(StandardCharsets.UTF_8);
                    CommandAPDU updatePointsCommand = new CommandAPDU(0x00, 0x12, 0x00, 0x00, newPointsBytes);
                    ResponseAPDU updateResponse = channel.transmit(updatePointsCommand);

                    if (updateResponse.getSW1() == 0x90 && updateResponse.getSW2() == 0x00) {
                        responseField.setText("Trừ điểm thành công. Điểm hiện tại: " + newPoints);
                        readCardData();
                    } else {
                        responseField.setText("Lỗi khi cập nhật điểm. SW: " + Integer.toHexString(updateResponse.getSW()));
                    }
                } else {
                    responseField.setText("Điểm không đủ để trừ.");
                }

            } else {
                responseField.setText("Lỗi khi lấy điểm hiện tại. SW: " + Integer.toHexString(response.getSW()));
            }
        } catch (NumberFormatException e) {
            responseField.setText("Lỗi định dạng dữ liệu điểm.");
        } catch (Exception e) {
            responseField.setText("Lỗi khi thực hiện lệnh: " + e.getMessage());
        }
    }

    private void unblockCard() {
        if (!isConnected || channel == null) {
            responseField.setText("Bạn phải kết nối với thẻ trước!");
            JOptionPane.showMessageDialog(null, "Bạn phải kết nối với thẻ trước!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Đường dẫn ảnh
        String imagePath = "C:\\Users\\ADMIIN\\OneDrive\\Pictures\\Ảnh\\success-icon-512x512-qdg1isa0.png";
        // Tạo ImageIcon từ ảnh
        ImageIcon originalIcon = new ImageIcon(imagePath);
        // Lấy đối tượng Image từ ImageIcon
        Image image = originalIcon.getImage();
        // Thay đổi kích thước ảnh (ví dụ: chiều rộng 100px, chiều cao 100px)
        Image resizedImage = image.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        // Tạo ImageIcon mới từ ảnh đã thay đổi kích thước
        ImageIcon resizedIcon = new ImageIcon(resizedImage);
        try {
            // Tạo APDU command
            CommandAPDU verifyPinCommand = new CommandAPDU(0x00, 0x03, 0x00, 0x00);
            ResponseAPDU response = channel.transmit(verifyPinCommand);

            // Đọc dữ liệu trả về
            int sw1 = response.getSW1();
            int sw2 = response.getSW2();

            if (sw1 == 0x90 && sw2 == 0x00) {
                responseField.setText("Mở khoá thẻ thành công!");
                JOptionPane.showMessageDialog(null, "Mở khoá thẻ thành công!", "Thành công", JOptionPane.CLOSED_OPTION, resizedIcon);
            } else {
                responseField.setText(String.format("Unblock Failed! SW1: 0x%02X, SW2: 0x%02X", sw1, sw2));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            responseField.setText("Status: Error occurred!");
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

    private void viewTransactionLogs() {
        if (!isConnected || channel == null) {
            responseField.setText("Bạn phải kết nối với thẻ trước!");
            return;
        }

        String[] columns = {"STT", "Nội dung", "Số dư"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);

        try {
            // 1. Lấy log từ thẻ
            java.util.List<String> logsList = new java.util.ArrayList<>();

            // P1 = 0 là log mới nhất, 1 là log tiếp theo...
            for (int i = 0; i < 5; i++) {
                CommandAPDU cmd = new CommandAPDU(0x00, 0x15, i, 0x00);
                ResponseAPDU resp = channel.transmit(cmd);

                if (resp.getSW() != 0x9000) {
                    break; // hết log hoặc lỗi
                }

                byte[] logBytes = resp.getData();
                String raw = new String(logBytes, StandardCharsets.UTF_8);

                // Loại NULL do JavaCard fill
                raw = raw.replace("\u0000", "");

                if (raw.length() == 0) continue;
                if (raw.charAt(0) != '+' && raw.charAt(0) != '-') continue;

                logsList.add(raw);
            }

            if (logsList.isEmpty()) {
                responseField.setText("Không có lịch sử giao dịch.");
                return;
            }

            // logsList hiện đang: mới nhất -> cũ nhất
            // Đảo lại: cũ nhất -> mới nhất (để tính toán theo thời gian)
            java.util.Collections.reverse(logsList);

            // 2. Parse delta (thay đổi điểm) từ từng log
            java.util.List<Integer> deltas = new java.util.ArrayList<>();
            for (String rawLog : logsList) {
                char sign = rawLog.charAt(0);   // '+' hoặc '-'
                String number = rawLog.substring(1);

                // Bỏ 0 ở đầu: "000000100" -> "100"
                number = number.replaceFirst("^0+(?!$)", "");
                if (number.equals("")) number = "0";

                int delta = Integer.parseInt(number);
                if (sign == '-') delta = -delta;

                deltas.add(delta);
            }

            // 3. Lấy điểm hiện tại trên thẻ (INS 0x13)
            CommandAPDU getPointsCommand = new CommandAPDU(0x00, 0x13, 0x00, 0x00);
            ResponseAPDU respPoints = channel.transmit(getPointsCommand);

            if (respPoints.getSW() != 0x9000) {
                responseField.setText("Lỗi khi lấy điểm hiện tại. SW="
                        + Integer.toHexString(respPoints.getSW()));
                return;
            }

            int currentPoints = Integer.parseInt(
                    new String(respPoints.getData(), StandardCharsets.UTF_8).trim()
            );

            // 4. Tính tổng tất cả delta trong 5 log
            int sumDelta = 0;
            for (int d : deltas) {
                sumDelta += d;
            }

            // Số dư trước giao dịch đầu tiên
            int base = currentPoints - sumDelta;

            // 5. Tính số dư sau từng giao dịch (theo thứ tự cũ -> mới)
            java.util.List<Object[]> finalRows = new java.util.ArrayList<>();
            int balance = base;

            for (int i = 0; i < logsList.size(); i++) {
                int delta = deltas.get(i);
                balance += delta;

                String displayChange = (delta > 0 ? "+" : "") + delta;

                finalRows.add(new Object[]{
                        null,            // STT tạm (sẽ set sau)
                        displayChange,   // Nội dung (thay đổi điểm)
                        balance          // Số dư
                });
            }

            // 6. Đảo list để hiển thị: mới nhất -> cũ nhất
            java.util.Collections.reverse(finalRows);

            // Set STT và add vào model
            for (int i = 0; i < finalRows.size(); i++) {
                finalRows.get(i)[0] = i + 1;
                model.addRow(finalRows.get(i));
            }

            JTable table = new JTable(model);
            JScrollPane scrollPane = new JScrollPane(table);

            JOptionPane.showMessageDialog(
                    null,
                    scrollPane,
                    "Lịch sử giao dịch",
                    JOptionPane.PLAIN_MESSAGE
            );

        } catch (Exception e) {
            responseField.setText("Lỗi xem lịch sử: " + e.getMessage());
        }
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