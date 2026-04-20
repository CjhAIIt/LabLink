import java.sql.*;
public class InspectSeedTargets {
  public static void main(String[] args) throws Exception {
    String url = "jdbc:mysql://localhost:3306/lab_recruitment?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true";
    try (Connection conn = DriverManager.getConnection(url, "root", "123456")) {
      System.out.println("[colleges]");
      try (PreparedStatement ps = conn.prepareStatement("SELECT id, college_code, college_name, admin_user_id, status FROM t_college WHERE deleted=0 ORDER BY id"); ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          System.out.println(rs.getLong(1)+"\t"+rs.getString(2)+"\t"+rs.getString(3)+"\tadmin_user_id="+rs.getString(4)+"\tstatus="+rs.getInt(5));
        }
      }
    }
  }
}
