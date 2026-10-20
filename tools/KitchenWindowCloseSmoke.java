import com.mx.kylgis.kitchenscr.KitchenScr;
import com.mx.kylgis.kitchenscr.forms.AppConfig;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import java.io.File;
import java.util.Properties;
public final class KitchenWindowCloseSmoke extends Application {
 private static volatile boolean failed;
 private static void assertCheck(boolean ok,String name){
  if(!ok)throw new AssertionError(name);
  System.out.println("PASS "+name);
 }
 @Override public void start(Stage primary){
  try {
   Properties p=new Properties();
   p.setProperty("db.driver","com.mysql.jdbc.Driver");
   p.setProperty("db.URL","jdbc:mysql://127.0.0.1:33319/kitchen_sintetica?useSSL=false&connectTimeout=2500&socketTimeout=2500");
   p.setProperty("db.user","root");p.setProperty("db.password","");
   p.setProperty("db.dialect","org.hibernate.dialect.MySQLDialect");
   p.setProperty("db.pool.min","1");p.setProperty("db.pool.max","2");
   p.setProperty("screen.allorders","true");p.setProperty("screen.displaynumber","1");
   p.setProperty("monitor.readonly","true");p.setProperty("recall.historycount","10");
   AppConfig.installRuntimeConfiguration(new File("/tmp/kitchen-java21-f12-inmemory.properties"),p);
   new KitchenScr().start(primary);
   Stage splash=null;
   for(Window win:Window.getWindows()){
    if(win instanceof Stage && win.isShowing() && win!=KitchenScr.publicStage){splash=(Stage)win;break;}
   }
   assertCheck(splash!=null,"CLOSE_STARTUP_SPLASH");
   Stage startup=splash;
   PauseTransition action=new PauseTransition(Duration.millis(400));
   action.setOnFinished(e -> {
    try {
     System.out.println("PASS CLOSE_X_REQUEST_DISPATCHED");
     startup.fireEvent(new javafx.stage.WindowEvent(startup,javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST));
     failed=true;
     System.err.println("FAIL CLOSE_X_DID_NOT_EXIT");
     Platform.exit();
    }catch(Throwable ex){failed=true;ex.printStackTrace();Platform.exit();}
   });
   action.play();
   return;
  } catch(Throwable e){failed=true;e.printStackTrace();}
  finally {
   // No salir antes de disparar el evento diferido de cierre.
   if(failed) Platform.exit();
  }
 }
 public static void main(String[] args) {
  try{launch(args);}catch(Throwable e){failed=true;e.printStackTrace();}
  System.exit(failed?1:0);
 }
}
