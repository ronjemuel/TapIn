package tapin;

import java.sql.SQLException;
import tapin.service.AuthService;
import tapin.ui.Style;

/** Run against the stopped isolated server, never by stopping a production server. */
public final class ConnectionFailureTest {
    public static void main(String[] args)throws Exception{
        try{new AuthService().login("teacher","TapInTeacher!2026".toCharArray(),"TEACHER");}
        catch(SQLException expected){
            String message=Style.message(expected);
            if(!message.contains("Cannot connect to MySQL"))throw new AssertionError(message);
            System.out.println("PASS database connection failure gives XAMPP/configuration guidance");return;
        }
        throw new AssertionError("This check expects the isolated server to be stopped.");
    }
}
