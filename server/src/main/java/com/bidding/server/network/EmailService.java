package com.bidding.server.network;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailService {

    // 1. CHỖ NÀY ĐỂ BÁC NẠP ĐẠN (Thay bằng thông tin thật của bác)
    private static final String MY_EMAIL = "datngu823@gmail.com";
    private static final String APP_PASSWORD = "gzta bnqw jedw koiz";
    // Hàm đẻ ra mã OTP 6 số ngẫu nhiên (ví dụ: 048291)

    public static boolean sendOTP(String toEmail, String otpCode) {

        // Cấu hình thông số trạm phát sóng của Google (SMTP)
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        // Đưa Thẻ VIP cho Google kiểm tra
        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(MY_EMAIL, APP_PASSWORD);
                    }
                });

        try {
            // Soạn nội dung bức thư
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(MY_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));

            // Tiêu đề thư
            message.setSubject("Mã OTP Khôi Phục Mật Khẩu - Game Đấu Giá");

            // Nội dung thư
            String noiDung = "Chào dân chơi,\n\n"
                    + "Mã OTP để lấy lại mật khẩu của bạn là: " + otpCode + "\n\n"
                    + "Mã này có hiệu lực trong 5 phút. Vui lòng không share cho thằng nào khác!\n\n"
                    + "Trân trọng,\n"
                    + "Admin Lợi Đẹp Trai.";
            message.setText(noiDung);

            // BÓP CÒ! Gửi thư đi
            Transport.send(message);

            System.out.println("✅ Đã bắn OTP " + otpCode + " thành công tới email: " + toEmail);
            return true;

        } catch (MessagingException e) {
            System.err.println("❌ LỖI RỒI! Bắn xịt. Kiểm tra lại mạng hoặc App Password nhé.");
            e.printStackTrace();
            return false;
        }
    }

    // 3. HÀM MAIN ĐỂ BÁC TEST ĐỘC LẬP (Chạy thử trước khi ráp vào Server)
    public static void main(String[] args) {
        System.out.println("Đang kết nối tới trạm phát sóng Google...");
        // Bác thay email của bác (hoặc email thằng bạn) vào đây để test gửi thử
        boolean isSuccess = sendOTP("loifaz100@gmail.com", "888999");

        if (isSuccess) {
            System.out.println("Ngon lành! Mở mail ra check hàng đi bác!");
        }
    }
}