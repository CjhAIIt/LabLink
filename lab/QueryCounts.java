import java.sql.*;
public class QueryCounts {
  public static void main(String[] args) throws Exception {
    String url = "jdbc:mysql://localhost:3306/lab_recruitment?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true";
    try (Connection conn = DriverManager.getConnection(url, "root", "123456")) {
      String[] sqls = new String[] {
        "SELECT COUNT(*) FROM t_user",
        "SELECT COUNT(*) FROM t_college",
        "SELECT COUNT(*) FROM t_lab",
        "SELECT COUNT(*) FROM t_lab_member WHERE deleted = 0 AND status = 'active' AND member_role='lab_admin'"
      };
      for (String sql : sqls) {
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
          rs.next();
          System.out.println(sql + " => " + rs.getLong(1));
        }
      }
    }
  }
}
