package do_sqlserver;

import java.io.IOException;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import java.util.Properties;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.RubyString;


import data_objects.RubyType;
import data_objects.drivers.AbstractDriverDefinition;
import data_objects.util.JDBCUtil;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.joda.time.DateTime;

public class SqlServerDriverDefinition extends AbstractDriverDefinition {

    public final static String URI_SCHEME = "sqlserver";
    // . will be replaced with : in Connection.java before connection
    public final static String JDBC_URI_SCHEME = "jtds.sqlserver";
    public final static String RUBY_MODULE_NAME = "SqlServer";
    private final static String UTF8_ENCODING = "UTF-8";
    public final static String JDBC_DRIVER = "net.sourceforge.jtds.jdbc.Driver";

    public SqlServerDriverDefinition() {
        super(URI_SCHEME, JDBC_URI_SCHEME, RUBY_MODULE_NAME, JDBC_DRIVER);
    }

    @Override
    public boolean supportsJdbcGeneratedKeys() {
        return true;
    }

    @Override
    public boolean supportsJdbcScrollableResultSets() {
        return true;
    }
    @Override
    public boolean supportsConnectionEncodings()
    {
        return true;
    }

    @Override
    public void setEncodingProperty(Properties props, String encodingName) {
        props.put("charset", encodingName);
    }

    @Override
    public java.sql.Connection getConnectionWithEncoding(Ruby runtime,
            IRubyObject connection, String url, Properties props) throws SQLException {
        java.sql.Connection conn;
        try  {
            conn = DriverManager.getConnection(url, props);
        } catch (SQLException eex) {
            Pattern p = Pattern.compile("Could not find a Java charset equivalent to DB charset (.+).");
            Matcher m = p.matcher(eex.getMessage());

            if (m.matches()) {
                // re-attempt connection, but this time with UTF-8
                // set as the encoding
                runtime.getWarnings().warn(String.format(
                        "Encoding %s is not a known Ruby encoding for %s\n",
                        m.group(1), RUBY_MODULE_NAME));
                setEncodingProperty(props, UTF8_ENCODING);
                API.setInstanceVariable(connection,
                        "@encoding", runtime.newString(UTF8_ENCODING));
                conn = DriverManager.getConnection(url, props);
            } else {
                throw eex;
            }
        }
        return conn;
    }

    private String replace(String sql, Object param)
    {
        return sql.replaceFirst("[?]", param.toString());
    }

    private String replace(String sql, String param)
    {
        return sql.replaceFirst("[?]", "'" + param + "'");
    }

    @Override
    public String statementToString(Statement s) {
        try {
            Class<?> psClazz = Class.forName("net.sourceforge.jtds.jdbc.JtdsPreparedStatement");
            Class<?> piClazz = Class.forName("net.sourceforge.jtds.jdbc.ParamInfo");
            Field sqlField = psClazz.getDeclaredField("sql");
            sqlField.setAccessible(true);
            String sql = sqlField.get(s).toString();
            Field paramsField = psClazz.getDeclaredField("parameters");
            paramsField.setAccessible(true);
            Field jdbcTypeField = piClazz.getDeclaredField("jdbcType");
            jdbcTypeField.setAccessible(true);
            Field valueField = piClazz.getDeclaredField("value");
            valueField.setAccessible(true);

            // Appended by jTDS Driver appends to support returning generated
            // keys. Strip for debugging output.
            sql = sql.replace(" SELECT SCOPE_IDENTITY() AS ID", "");
            sql = sql.replace(" SELECT @@IDENTITY AS ID", "");

            Object[] params = (Object[]) paramsField.get(s);
            for (Object param : params) {
                int jdbcType = jdbcTypeField.getInt(param);
                Object value = valueField.get(param);

                switch (jdbcType) {
                    case Types.CHAR:
                    case Types.LONGVARCHAR:
                    case Types.VARCHAR:
                        sql = replace(sql, value.toString());
                    default:
                        sql = replace(sql, value);
                }
            }
            return sql;
        }
        catch(Exception e) {
            // just fall to the toString of the PreparedStatement
            return s.toString();
        }
    }

    // for execution of session initialization SQL statements
    private void exec(Connection conn, String sql)
            throws SQLException {
        Statement s = null;
        try {
            s = conn.createStatement();
            s.execute(sql);
        } finally {
            JDBCUtil.close(s);
        }
    }

}