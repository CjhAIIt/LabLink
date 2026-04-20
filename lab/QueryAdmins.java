import java.sql.*;
public class QueryAdmins {
  public static void main(String[] args) throws Exception {
    String url = "jdbc:mysql://localhost:3306/lab_recruitment?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true";
    try (Connection conn = DriverManager.getConnection(url, "root", "123456")) {
      System.out.println("[system_admins]");
      try (PreparedStatement ps = conn.prepareStatement(
          "SELECT id, username, real_name, role, lab_id, college, email, status FROM t_user WHERE deleted = 0 AND role IN ('admin','super_admin') ORDER BY FIELD(role,'super_admin','admin'), id")) {
        try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            System.out.println(rs.getLong("id") + "\t" + rs.getString("username") + "\t" + rs.getString("real_name") + "\t" + rs.getString("role") + "\tlab=" + rs.getString("lab_id") + "\tcollege=" + rs.getString("college") + "\temail=" + rs.getString("email") + "\tstatus=" + rs.getInt("status"));
          }
        }
      }
      System.out.println("[lab_admin_members]");
      try (PreparedStatement ps = conn.prepareStatement(
          "SELECT m.id AS member_id, m.lab_id, l.lab_name, u.id AS user_id, u.username, u.real_name, u.role, u.email, m.status, m.member_role " +
          "FROM t_lab_member m " +
          "JOIN t_user u ON u.id = m.user_id " +
          "LEFT JOIN t_lab l ON l.id = m.lab_id " +
          "WHERE m.deleted = 0 AND u.deleted = 0 AND m.status = 'active' AND m.member_role = 'lab_admin' " +
          "ORDER BY m.lab_id, m.id")) {
        try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            System.out.println("lab=" + rs.getLong("lab_id") + "\t" + rs.getString("lab_name") + "\tuser=" + rs.getLong("user_id") + "\t" + rs.getString("username") + "\t" + rs.getString("real_name") + "\trole=" + rs.getString("role") + "\temail=" + rs.getString("email"));
          }
        }
      }
    }
  }
}
