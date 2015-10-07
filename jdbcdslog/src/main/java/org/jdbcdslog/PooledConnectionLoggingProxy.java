package org.jdbcdslog;

import javax.sql.PooledConnection;
import java.lang.reflect.Proxy;

public class PooledConnectionLoggingProxy {

    public static PooledConnection wrap(PooledConnection con) {
        return (PooledConnection) Proxy.newProxyInstance(con.getClass().getClassLoader()
                , new Class[]{PooledConnection.class}, new GenericLoggingProxy(con));
    }

}
