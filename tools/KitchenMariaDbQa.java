import java.io.File;
import java.sql.Timestamp;
import java.util.*;
import com.mx.kylgis.kitchenscr.forms.AppConfig;
import com.mx.kylgis.kitchenscr.hibernate.HibernateUtil;
import com.mx.kylgis.kitchenscr.utils.DataLogicKitchen;
import com.mx.kylgis.kitchenscr.dto.Orders;
public final class KitchenMariaDbQa {
 private static void check(boolean ok,String test) { if(!ok)throw new AssertionError(test);System.out.println("PASS "+test); }
 private static Orders order(String id,int index, int aux,Timestamp group) {
  Orders o=new Orders();o.setOrderid(id);o.setTicketid("QA-"+id);o.setQty(1);
  o.setDetails(aux>0?"Auxiliar-"+index:"Producto-"+index);
  o.setAttributes("");o.setNotes("QA");
  o.setDisplayid(1);o.setAuxiliary(aux);
  o.setOrdertime(new Timestamp(group.getTime()));o.setCompletetime(group);return o;
 }
 public static void main(String[] args) throws Exception {
  Properties p=new Properties();
  p.setProperty("db.driver","com.mysql.jdbc.Driver");
  p.setProperty("db.URL","jdbc:mysql://127.0.0.1:33319/kitchen_sintetica?useSSL=false&connectTimeout=2500&socketTimeout=2500");
  p.setProperty("db.user","root");p.setProperty("db.password","");
  p.setProperty("db.dialect","org.hibernate.dialect.MySQLDialect");
  p.setProperty("db.pool.min","1");p.setProperty("db.pool.max","2");
  p.setProperty("screen.allorders","false");p.setProperty("screen.displaynumber","1");
  p.setProperty("monitor.readonly","false");
  AppConfig.installRuntimeConfiguration(new File("/tmp/kylgis-qa-in-memory.properties"),p);
  check(HibernateUtil.testConnection(),"MARIADB_CONNECTION_C3P0");
  DataLogicKitchen dl=new DataLogicKitchen();
  List<Orders> originals=new ArrayList<>();
  for(int i=1;i<=8;i++) {
   Timestamp tm=Timestamp.valueOf(String.format("2026-10-19 12:%02d:00.000",i));
   originals.add(order("QA-"+i,i,0,tm));
   if(i==3) originals.add(order("QA-"+i,30,1,tm));
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
   List<Orders> third=dl.selectByOrderId("QA-3");
   check(third.size()==2,"AGRUPACION_TICKET_QA3");
   check(third.get(0).getDetails().equals("Producto-3") && third.get(1).getDetails().equals("Auxiliar-30"),"ORDEN_PRODUCTO_AUXILIAR");
   check(third.get(0).getCompletetime().equals(third.get(1).getCompletetime()),"MISMO_COMPLETETIME");
   dl.removeOrder(third.get(0).getCompletetime());
   check(dl.selectByOrderId("QA-3").isEmpty(),"FINALIZACION_ELIMINA_GRUPO_COMPLETETIME");
   for(Orders o:third)dl.createOrder(o);
   List<Orders> recovered=dl.selectByOrderId("QA-3");
   check(recovered.size()==2 && recovered.get(1).getDetails().equals("Auxiliar-30"),"RECUPERACION_Y_ORDEN");
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
   try { dl.removeAllOrdersDisplay(); } catch(Exception x) { System.err.println("QA_CLEANUP_ERROR "+x); }
   HibernateUtil.shutdown();
  }
  System.out.println("KITCHEN_MARIADB_QA_SUCCESS");
 }
}
