import java.sql.*;
import org.hibernate.*;
import org.hibernate.cfg.Configuration;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
public class LegacyRuntimeSmoke {
 public static void main(String[] args) throws Exception {
  Class.forName("org.hsqldb.jdbcDriver");
  try(Connection conn = DriverManager.getConnection("jdbc:hsqldb:mem:kylgis_kitchen_java21_smoke","sa","")) {
   try (Statement st=conn.createStatement()) {
    st.execute("CREATE TABLE smoke (id INTEGER PRIMARY KEY, value VARCHAR(20))");
    st.execute("INSERT INTO smoke VALUES (1,'ok')");
    try (ResultSet rs=st.executeQuery("SELECT value FROM smoke WHERE id=1")) {
     if (!rs.next() || !"ok".equals(rs.getString(1))) throw new AssertionError("JDBC");
    }
   }
  }
  System.out.println("JDBC_HSQL_SMOKE_OK");
  Class<?> mysql = Class.forName("com.mysql.jdbc.Driver");
  Driver mysqlDriver=(Driver)mysql.getDeclaredConstructor().newInstance();
  if(!mysqlDriver.acceptsURL("jdbc:mysql://127.0.0.1:3306/nonexistent")) throw new AssertionError("MYSQL_DRIVER");
  System.out.println("MYSQL_5_DRIVER_LOAD_OK_NO_CONNECTION");
  javassist.ClassPool pool=javassist.ClassPool.getDefault();
  javassist.CtClass generated=pool.makeClass("kylgis.smoke.Generated");
  generated.addMethod(javassist.CtNewMethod.make("public int answer(){ return 42; }", generated));
  byte[] b=generated.toBytecode();
  if(b.length==0) throw new AssertionError("Javassist");
  System.out.println("JAVASSIST_BYTECODE_SMOKE_OK");
  Configuration cfg=new Configuration();
  cfg.setProperty("hibernate.connection.driver_class","org.hsqldb.jdbcDriver");
  cfg.setProperty("hibernate.connection.url","jdbc:hsqldb:mem:kylgis_kitchen_java21_smoke");
  cfg.setProperty("hibernate.connection.username","sa");
  cfg.setProperty("hibernate.connection.password","");
  cfg.setProperty("hibernate.dialect","org.hibernate.dialect.HSQLDialect");

  cfg.addAnnotatedClass(com.mx.kylgis.kitchenscr.dto.Orders.class);
  ServiceRegistry sr=new StandardServiceRegistryBuilder().applySettings(cfg.getProperties()).build();
  try {
   SessionFactory sf=cfg.buildSessionFactory(sr);
   try {
    Session session=sf.openSession();
    try { Object result=session.createSQLQuery("SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS").setMaxResults(1).uniqueResult();
     if(result==null) throw new AssertionError("HIBERNATE_SQL");
    } finally { session.close(); }
    System.out.println("HIBERNATE_4_SQL_SMOKE_OK");
   } finally { sf.close(); }
  } finally { StandardServiceRegistryBuilder.destroy(sr); }
 }
}
