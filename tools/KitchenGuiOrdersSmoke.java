import com.mx.kylgis.kitchenscr.forms.AppConfig;
import com.mx.kylgis.kitchenscr.hibernate.HibernateUtil;
import com.mx.kylgis.kitchenscr.KitchenscrController;
import java.io.File;
import java.util.Properties;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
public final class KitchenGuiOrdersSmoke extends Application {
 private static volatile boolean failed = false;
 public void start(Stage stage) {
  KitchenscrController controller=null;
  try {
   Properties p=new Properties();
   p.setProperty("db.driver","com.mysql.jdbc.Driver");
   p.setProperty("db.URL","jdbc:mysql://127.0.0.1:33319/kitchen_sintetica?useSSL=false&connectTimeout=2500&socketTimeout=2500");
   p.setProperty("db.user","root");p.setProperty("db.password","");
   p.setProperty("db.dialect","org.hibernate.dialect.MySQLDialect");
   p.setProperty("db.pool.min","1");p.setProperty("db.pool.max","2");
   p.setProperty("screen.allorders","true");p.setProperty("screen.displaynumber","1");
   p.setProperty("monitor.readonly","true");p.setProperty("recall.historycount","10");
   AppConfig.installRuntimeConfiguration(new File("/tmp/kitchen-java21-gui-inmemory.properties"),p);
   if(!HibernateUtil.testConnection())throw new AssertionError("LAB_UNAVAILABLE");
   FXMLLoader loader=new FXMLLoader(KitchenscrController.class.getResource("kitchenscr.fxml"));
   Parent root=loader.load();
   controller=loader.getController();
   stage.setTitle("Kitchen Java21 QA synthetic ONLY");
   stage.setScene(new Scene(root,1024,768));
   stage.show();
   root.applyCss();root.layout();
   int visible=0;
   for(int i=0;i<8;i++){
    Label label=(Label)root.lookup("#order"+i+"id");
    ListView<?> lines=(ListView<?>)root.lookup("#order"+i+"items");
    if(label!=null && lines!=null && label.getText()!=null && !label.getText().isBlank() && !lines.getItems().isEmpty())visible++;
    System.out.println("GUI_SLOT_"+i+" header="+(label==null?"MISSING":label.getText())+" lines="+(lines==null?-1:lines.getItems().size()));
   }
   if(Boolean.getBoolean("kylgis.qa.forceFailure"))throw new AssertionError("INTENTIONAL_QA_FAILURE");
   if(visible!=8)throw new AssertionError("GUI_8_ORDERS_VISIBLE actual="+visible);
   System.out.println("PASS GUI_EIGHT_ORDER_PANELS_RENDERED");
  } catch(Throwable e) {
   failed=true;
   e.printStackTrace();
   System.err.println("FAIL GUI_8_ORDERS");
  } finally {
   try {
    if(controller!=null)controller.shutdown();
   } catch(Throwable e) {failed=true;e.printStackTrace();}
   try {
    HibernateUtil.shutdown();
   } catch(Throwable e) {failed=true;e.printStackTrace();}
   try {stage.close();} catch(Throwable e){failed=true;e.printStackTrace();}
   Platform.exit();
  }
 }
 public static void main(String[] args) {
  try {
   launch(args);
  } catch(Throwable e) {
   failed=true;
   e.printStackTrace();
  } finally {
   System.exit(failed ? 1 : 0);
  }
 }
}
