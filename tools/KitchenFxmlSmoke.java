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
import javafx.stage.Stage;
public final class KitchenFxmlSmoke extends Application {
 public void start(Stage stage) throws Exception {
  Properties p = new Properties();
  p.setProperty("db.driver","com.mysql.jdbc.Driver");
  p.setProperty("db.URL","jdbc:mysql://127.0.0.1:33319/kitchen_sintetica?useSSL=false&connectTimeout=2500&socketTimeout=2500");
  p.setProperty("db.user","root"); p.setProperty("db.password","");
  p.setProperty("db.dialect","org.hibernate.dialect.MySQLDialect");
  p.setProperty("db.pool.min","1"); p.setProperty("db.pool.max","2");
  p.setProperty("screen.allorders","true"); p.setProperty("screen.displaynumber","1");
  p.setProperty("monitor.readonly","true"); p.setProperty("recall.historycount","10");
  AppConfig.installRuntimeConfiguration(new File("/tmp/kitchen-java21-fxml-smoke-inmemory.properties"),p);
  if(!HibernateUtil.testConnection())throw new AssertionError("LAB_CONNECTION");
  FXMLLoader loader=new FXMLLoader(KitchenscrController.class.getResource("kitchenscr.fxml"));
  Parent root=loader.load();
  KitchenscrController controller=loader.getController();
  stage.setScene(new Scene(root,1024,768));
  root.applyCss();root.layout();
  System.out.println("PASS FXML_LOAD_8_PANELS controls="+root.lookupAll(".list-view").size());
  controller.shutdown();HibernateUtil.shutdown();
  Platform.exit();
 }
 public static void main(String[] args){launch(args);}
}
