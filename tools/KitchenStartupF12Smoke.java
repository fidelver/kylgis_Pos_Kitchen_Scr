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
public final class KitchenStartupF12Smoke extends Application {
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
   for(Window w:Window.getWindows()){
    if(w instanceof Stage && w.isShowing() && w!=KitchenScr.publicStage){splash=(Stage)w;break;}
   }
   assertCheck(splash!=null,"F12_STARTUP_SPLASH_VISIBLE");
   Stage startup=splash;
   PauseTransition closeSettings=new PauseTransition(Duration.millis(650));
   closeSettings.setOnFinished(e->{
    Stage config=null;
    for(Window w:Window.getWindows()){
     if(w instanceof Stage && w.isShowing() && w!=startup && w!=KitchenScr.publicStage){config=(Stage)w;break;}
    }
    try {assertCheck(config!=null,"F12_CONFIGURATION_STAGE_OPEN");}
    catch(Throwable ex){failed=true;ex.printStackTrace();}
    if(config!=null)config.close();
   });
   closeSettings.play();
   startup.getScene().getRoot().fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED,"","",KeyCode.F12,false,false,false,false));
   assertCheck(KitchenScr.publicStage!=null && KitchenScr.publicStage.isShowing(),"F12_RETURN_TO_KITCHEN");
   assertCheck(!startup.isShowing(),"F12_SPLASH_CLOSED");
  } catch(Throwable e){failed=true;e.printStackTrace();}
  finally{
   Platform.exit();
  }
 }
 public static void main(String[] args) {
  try{launch(args);}catch(Throwable e){failed=true;e.printStackTrace();}
  System.exit(failed?1:0);
 }
}
