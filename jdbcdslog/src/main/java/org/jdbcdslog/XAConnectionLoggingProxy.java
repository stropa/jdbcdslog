package org.jdbcdslog;

import javax.sql.XAConnection;
import java.lang.reflect.Proxy;

public class XAConnectionLoggingProxy {

    public static XAConnection wrap(XAConnection con) {
        return (XAConnection) Proxy.newProxyInstance(con.getClass().getClassLoader()
                , new Class[]{XAConnection.class}, new GenericLoggingProxy(con));
    }

}
