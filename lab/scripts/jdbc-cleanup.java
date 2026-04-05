import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class jdbc_cleanup {

    public static void main(String[] args) throws Exception {
        CleanupArgs cleanupArgs = CleanupArgs.parse(args);
        Class.forName("com.mysql.cj.jdbc.Driver");

        try (Connection connection = DriverManager.getConnection(
                cleanupArgs.dbUrl,
                cleanupArgs.dbUsername,
                cleanupArgs.dbPassword
        )) {
            connection.setAutoCommit(false);
            try {
                List<Long> examIds = loadIds(connection,
                        "SELECT id FROM t_written_exam WHERE lab_id = ?",
                        cleanupArgs.labId);

                deleteByIds(connection, "DELETE FROM t_system_notification WHERE user_id IN ", cleanupArgs.userIds);
                deleteByIds(connection, "DELETE FROM t_lab_exit_application WHERE user_id IN ", cleanupArgs.userIds);
                deleteByIds(connection, "DELETE FROM t_lab_attendance WHERE user_id IN ", cleanupArgs.userIds);
                deleteByIds(connection, "DELETE FROM t_growth_assessment_result WHERE user_id IN ", cleanupArgs.userIds);
                deleteByIds(connection, "DELETE FROM t_equipment_borrow WHERE user_id IN ", cleanupArgs.userIds);
                deleteByIds(connection, "DELETE FROM t_delivery WHERE user_id IN ", cleanupArgs.userIds);
                deleteByIds(connection, "DELETE FROM t_written_exam_submission WHERE user_id IN ", cleanupArgs.userIds);

                if (cleanupArgs.labId != null) {
                    deleteWithSingleId(connection, "DELETE FROM t_equipment_borrow WHERE equipment_id IN (SELECT id FROM t_equipment WHERE lab_id = ?)", cleanupArgs.labId);
                    deleteWithSingleId(connection, "DELETE FROM t_equipment WHERE lab_id = ?", cleanupArgs.labId);
                    deleteWithSingleId(connection, "DELETE FROM t_delivery WHERE lab_id = ?", cleanupArgs.labId);
                    deleteWithSingleId(connection, "DELETE FROM t_written_exam_submission WHERE lab_id = ?", cleanupArgs.labId);
                    deleteWithSingleId(connection, "DELETE FROM t_lab_exit_application WHERE lab_id = ?", cleanupArgs.labId);
                    deleteWithSingleId(connection, "DELETE FROM t_lab_attendance WHERE lab_id = ?", cleanupArgs.labId);
                }

                deleteByIds(connection, "DELETE FROM t_written_exam_question WHERE exam_id IN ", examIds);
                deleteByIds(connection, "DELETE FROM t_written_exam WHERE id IN ", examIds);
                deleteByIds(connection, "DELETE FROM t_growth_practice_question WHERE id IN ", cleanupArgs.questionIds);
                deleteByIds(connection, "DELETE FROM t_user WHERE id IN ", cleanupArgs.userIds);

                if (cleanupArgs.labId != null) {
                    deleteWithSingleId(connection, "DELETE FROM t_lab WHERE id = ?", cleanupArgs.labId);
                }

                connection.commit();
                System.out.println("cleanup=ok");
                System.out.println("labId=" + cleanupArgs.labId);
                System.out.println("userIds=" + cleanupArgs.userIds);
                System.out.println("questionIds=" + cleanupArgs.questionIds);
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private static List<Long> loadIds(Connection connection, String sql, Long id) throws Exception {
        if (id == null) {
            return new ArrayList<>();
        }
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getLong(1));
                }
            }
        }
        return ids;
    }

    private static void deleteWithSingleId(Connection connection, String sql, Long id) throws Exception {
        if (id == null) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    private static void deleteByIds(Connection connection, String prefixSql, List<Long> ids) throws Exception {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        StringBuilder placeholders = new StringBuilder("(");
        for (int index = 0; index < ids.size(); index++) {
            if (index > 0) {
                placeholders.append(", ");
            }
            placeholders.append("?");
        }
        placeholders.append(")");

        try (PreparedStatement statement = connection.prepareStatement(prefixSql + placeholders)) {
            for (int index = 0; index < ids.size(); index++) {
                statement.setLong(index + 1, ids.get(index));
            }
            statement.executeUpdate();
        }
    }

    private static final class CleanupArgs {
        private final String dbUrl;
        private final String dbUsername;
        private final String dbPassword;
        private final Long labId;
        private final List<Long> userIds;
        private final List<Long> questionIds;

        private CleanupArgs(String dbUrl,
                            String dbUsername,
                            String dbPassword,
                            Long labId,
                            List<Long> userIds,
                            List<Long> questionIds) {
            this.dbUrl = dbUrl;
            this.dbUsername = dbUsername;
            this.dbPassword = dbPassword;
            this.labId = labId;
            this.userIds = userIds;
            this.questionIds = questionIds;
        }

        private static CleanupArgs parse(String[] args) {
            String dbUrl = "jdbc:mysql://localhost:3306/lab_recruitment?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true";
            String dbUsername = "root";
            String dbPassword = "cjh041217";
            Long labId = null;
            List<Long> userIds = new ArrayList<>();
            List<Long> questionIds = new ArrayList<>();

            List<String> arguments = Arrays.asList(args);
            for (int index = 0; index < arguments.size(); index++) {
                String argument = arguments.get(index);
                if (index + 1 >= arguments.size()) {
                    break;
                }
                String value = arguments.get(index + 1);
                if ("--db-url".equals(argument)) {
                    dbUrl = value;
                    index++;
                } else if ("--db-username".equals(argument)) {
                    dbUsername = value;
                    index++;
                } else if ("--db-password".equals(argument)) {
                    dbPassword = value;
                    index++;
                } else if ("--lab-id".equals(argument)) {
                    labId = Long.parseLong(value);
                    index++;
                } else if ("--user-id".equals(argument)) {
                    userIds.add(Long.parseLong(value));
                    index++;
                } else if ("--question-id".equals(argument)) {
                    questionIds.add(Long.parseLong(value));
                    index++;
                }
            }

            return new CleanupArgs(dbUrl, dbUsername, dbPassword, labId, userIds, questionIds);
        }
    }
}
