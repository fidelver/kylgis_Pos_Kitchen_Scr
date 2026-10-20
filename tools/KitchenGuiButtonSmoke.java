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
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.event.Event;
import javafx.scene.control.Button;
import com.mx.kylgis.kitchenscr.utils.DataLogicKitchen;
public final class KitchenGuiButtonSmoke extends Application {
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
   p.setProperty("monitor.readonly","false");p.setProperty("recall.historycount","10");
   AppConfig.installRuntimeConfiguration(new File("/tmp/kitchen-java21-gui-inmemory.properties"),p);
   if(!HibernateUtil.testConnection())throw new AssertionError("LAB_UNAVAILABLE");
   FXMLLoader loader=new FXMLLoader(KitchenscrController.class.getResource("kitchenscr.fxml"));
   Parent root=loader.load();
   controller=loader.getController();
   stage.setTitle("Kitchen Java21 QA synthetic ONLY");
   stage.setScene(new Scene(root,1024,768));
   stage.show();
   root.applyCss();root.layout();
   controller.setScene(stage.getScene());
   stage.getScene().getRoot().fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED,"1","1",KeyCode.DIGIT1,false,false,false,false));
   Button complete=(Button)root.lookup("#completed");
   if(complete==null || complete.isDisabled())throw new AssertionError("COMPLETE_BUTTON_NOT_READY");
   complete.fire();
   if(!new DataLogicKitchen().selectByOrderId("BTN-J21-1").isEmpty())throw new AssertionError("COMPLETE_DB_NOT_REMOVED");
   System.out.println("PASS GUI_COMPLETE_REAL_BUTTON");
   Button recall=(Button)root.lookup("#recall");
   if(recall==null || !recall.isVisible())throw new AssertionError("RECALL_BUTTON_NOT_AVAILABLE");
   recall.fire();
   if(new DataLogicKitchen().selectByOrderId("BTN-J21-1").size()!=2)throw new AssertionError("RECALL_BUTTON_DB_NOT_RESTORED");
   System.out.println("PASS GUI_RECALL_REAL_BUTTON");
  } catch(Throwable e) {
   failed=true;
   e.printStackTrace();
   System.err.println("FAIL GUI_BUTTON_ACTIONS");
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
