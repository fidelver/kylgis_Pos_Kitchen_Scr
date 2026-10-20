import java.io.File;
import java.sql.Timestamp;
import java.sql.*;
import java.util.*;
import com.mx.kylgis.kitchenscr.forms.AppConfig;
import com.mx.kylgis.kitchenscr.hibernate.HibernateUtil;
import com.mx.kylgis.kitchenscr.utils.DataLogicKitchen;
import com.mx.kylgis.kitchenscr.dto.Orders;
public final class KitchenMariaDbQa {
 private static final String EXPECTED_URL = "jdbc:mysql://127.0.0.1:33319/kitchen_sintetica?useSSL=false&connectTimeout=2500&socketTimeout=2500";
 private static final String PREFIX = "QA-J21-20261019-";
 private static final String CONTAINER = "kylgis-kitchen-java21-qa-20261019";
 private static void ensureIsolation() throws Exception {
  Process p = new ProcessBuilder("podman","inspect","--format","{{.Name}} {{.ImageName}} {{index .Config.Labels \"kylgis.qa.scope\"}}",CONTAINER).redirectErrorStream(true).start();
  String info = new String(p.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).trim();
  check(p.waitFor()==0 && info.contains(CONTAINER) && info.contains("mariadb") && info.endsWith("kitchen-java21-synthetic-only"),"CONTENEDOR_IDENTIDAD_VERIFICADA");
  ensureContainerOnly();
  try(Connection conn=DriverManager.getConnection(EXPECTED_URL,"root","");Statement st=conn.createStatement()){
   try(ResultSet rs=st.executeQuery("SELECT DATABASE(), @@hostname, @@port")){
    check(rs.next() && "kitchen_sintetica".equals(rs.getString(1)) && rs.getInt(3)==3306,"BD_IDENTIDAD_VERIFICADA");
   }
   try(PreparedStatement q=conn.prepareStatement("SELECT COUNT(*) FROM orders WHERE ORDERID LIKE ?")){
    q.setString(1,PREFIX+"%");
    try(ResultSet rs=q.executeQuery()){check(rs.next() && rs.getInt(1)==0,"PREFIJO_QA_DISPONIBLE");}
   }
  }
 }
 private static void cleanupOwnOrders() throws Exception {
  ensureContainerOnly();
  try(Connection conn=DriverManager.getConnection(EXPECTED_URL,"root","")){
   check("kitchen_sintetica".equals(conn.getCatalog()),"BD_LIMPIEZA_IDENTIDAD");
   try(PreparedStatement q=conn.prepareStatement("DELETE FROM orders WHERE ORDERID IN (?,?,?,?,?,?,?,?)")){
    for(int i=1;i<=8;i++)q.setString(i,PREFIX+i);int n=q.executeUpdate();
    check(n<=9,"LIMPIEZA_SOLO_FILAS_QA");
   }
  }
 }
 private static void ensureContainerOnly() throws Exception {
  Process port = new ProcessBuilder("podman","port",CONTAINER,"3306/tcp").redirectErrorStream(true).start();
  String binding = new String(port.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).trim();
  check(port.waitFor()==0 && binding.equals("127.0.0.1:33319"),"CONTENEDOR_LOOPBACK_PUERTO_EXACTO");
 }

 private static void check(boolean ok,String test) { if(!ok)throw new AssertionError(test);System.out.println("PASS "+test); }
 private static Orders order(String id,int index, int aux,Timestamp group) {
  Orders o=new Orders();o.setOrderid(id);o.setTicketid(id);o.setQty(1);
  o.setDetails(aux>0?"Auxiliar-"+index:"Producto-"+index);
  o.setAttributes("");o.setNotes("QA");
  o.setDisplayid(1);o.setAuxiliary(aux);
  o.setOrdertime(new Timestamp(group.getTime()));o.setCompletetime(group);return o;
 }
 public static void main(String[] args) throws Exception {
  Properties p=new Properties();
  p.setProperty("db.driver","com.mysql.jdbc.Driver");
  p.setProperty("db.URL",EXPECTED_URL);
  p.setProperty("db.user","root");p.setProperty("db.password","");
  p.setProperty("db.dialect","org.hibernate.dialect.MySQLDialect");
  p.setProperty("db.pool.min","1");p.setProperty("db.pool.max","2");
  p.setProperty("screen.allorders","false");p.setProperty("screen.displaynumber","1");
  p.setProperty("monitor.readonly","false");
  ensureIsolation();
  AppConfig.installRuntimeConfiguration(new File("/tmp/kylgis-qa-in-memory.properties"),p);
  check(HibernateUtil.testConnection(),"MARIADB_CONNECTION_C3P0");
  DataLogicKitchen dl=new DataLogicKitchen();
  List<Orders> originals=new ArrayList<>();
  for(int i=1;i<=8;i++) {
   Timestamp tm=Timestamp.valueOf(String.format("2026-10-19 12:%02d:00.000",i));
   originals.add(order(PREFIX+i,i,0,tm));
   if(i==3) originals.add(order(PREFIX+i,30,1,tm));
  }
  try {
   for(Orders o:originals)dl.createOrder(o);
   List<Orders> all=dl.selectAllOrders();
   check(all.size()==9,"RECEPCION_NUEVE_LINEAS_OCHO_ORDENES");
   check(dl.readDistinctOrders().size()==8,"OCHO_ORDENES_DISTINTAS");
   AppConfig.getInstance().setProperty("monitor.readonly","true");
   boolean blocked=false;
   try { dl.removeOrder(Timestamp.valueOf("2026-10-19 12:01:00")); }
   catch (IllegalStateException expected) { blocked=expected.getMessage().contains("solo lectura"); }
   check(blocked,"MODO_SOLO_LECTURA_BLOQUEA_FINALIZACION");
   AppConfig.getInstance().setProperty("monitor.readonly","false");
   List<Orders> third=dl.selectByOrderId(PREFIX+"3");
   check(third.size()==2,"AGRUPACION_TICKET_QA3");
   check(third.get(0).getDetails().equals("Producto-3") && third.get(1).getDetails().equals("Auxiliar-30"),"ORDEN_PRODUCTO_AUXILIAR");
   check(third.get(0).getCompletetime().equals(third.get(1).getCompletetime()),"MISMO_COMPLETETIME");
   dl.removeOrder(third.get(0).getCompletetime());
   check(dl.selectByOrderId(PREFIX+"3").isEmpty(),"FINALIZACION_ELIMINA_GRUPO_COMPLETETIME");
   for(Orders o:third)dl.createOrder(o);
   List<Orders> recovered=dl.selectByOrderId(PREFIX+"3");
   check(recovered.size()==2 && recovered.get(1).getDetails().equals("Auxiliar-30"),"REINSERCION_DE_DATOS_Y_ORDEN");
   // Fallo de red simulado sólo sobre el contenedor de QA, nunca DB externas.
   Process pause=new ProcessBuilder("podman","pause","kylgis-kitchen-java21-qa-20261019").inheritIO().start();
   check(pause.waitFor()==0,"AISLAMIENTO_CONTENEDOR_PAUSADO");
   boolean disconnected;
   try { disconnected=!HibernateUtil.testConnection(); }
   finally {
     Process resume=new ProcessBuilder("podman","unpause","kylgis-kitchen-java21-qa-20261019").inheritIO().start();
     check(resume.waitFor()==0,"CONTENEDOR_REANUDADO");
   }
   check(disconnected,"PERDIDA_DE_CONEXION_DETECTADA");
   check(HibernateUtil.rebuildSessionFactory(),"RECONEXION_HIBERNATE");
   check(dl.selectAllOrders().size()==9,"LECTURA_TRAS_RECONEXION");
  }finally {
   try { cleanupOwnOrders(); } catch(Exception x) { System.err.println("QA_CLEANUP_ERROR "+x); throw x; }
   HibernateUtil.shutdown();
  }
  System.out.println("KITCHEN_MARIADB_QA_SUCCESS");
 }
}
