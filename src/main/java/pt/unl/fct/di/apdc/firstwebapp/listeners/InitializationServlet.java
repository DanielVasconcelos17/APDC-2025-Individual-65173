package pt.unl.fct.di.apdc.firstwebapp.listeners;

import java.util.logging.Logger;


import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.apache.commons.codec.digest.DigestUtils;
import pt.unl.fct.di.apdc.firstwebapp.types.ProfileState;
import pt.unl.fct.di.apdc.firstwebapp.types.ProfileType;
import pt.unl.fct.di.apdc.firstwebapp.types.Role;

//Esta classe tem a responsabilidade de registar o "root" no datastore quando a app dá deploy
@WebListener
public class InitializationServlet implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(InitializationServlet.class.getName());

    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    private static final String ROOT_USERNAME = "root";
    private static final String ROOT_PASSWORD = "RootP@ssword1";
    private static final String ROOT_EMAIL = "root.admin@fct.unl.pt";
    private static final String ROOT_FULLNAME = "Root Administrator";
    private static final String ROOT_PHONE = "+351000000000";
    private static final String ROOT_PROFILE = ProfileType.PRIVATE.getType();


    @Override
    public void contextInitialized(ServletContextEvent sce) {
        this.createRootUser();
    }
    private void createRootUser(){
        Transaction txn = datastore.newTransaction();
        try{
            Key userKey = datastore.newKeyFactory().setKind("User").newKey(ROOT_USERNAME);
            Entity user = txn.get(userKey);
            if(user == null){
                Entity rootUser = Entity.newBuilder(userKey)
                        .set("user_pwd", DigestUtils.sha512Hex(ROOT_PASSWORD))
                        .set("user_email", ROOT_EMAIL)
                        .set("user_name", ROOT_FULLNAME)
                        .set("user_phone", ROOT_PHONE)
                        .set("user_profile", ROOT_PROFILE)
                        .set("user_role", Role.ADMIN.getType())
                        .set("user_state", ProfileState.ACTIVATE.getType())
                        .set("user_creation_time", Timestamp.now())
                        .build();
                txn.put(rootUser);
                txn.commit();
                LOG.info("User Root Created With Success: " + ROOT_USERNAME);
            }
        }catch (DatastoreException e){
            txn.rollback();
            LOG.severe("Error in Creation of User Root");
        }finally {
            if(txn.isActive())
                txn.rollback();
        }
    }
}
