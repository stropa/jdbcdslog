package org.jdbcdslog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenericLoggingProxy implements InvocationHandler {

    static Logger logger = LoggerFactory.getLogger(GenericLoggingProxy.class);

    static List methodsBlackList = Arrays.asList("getAutoCommit", "getCatalog", "getTypeMap"
            , "clearWarnings", "setAutoCommit", "getFetchSize", "setFetchSize", "commit");

    String sql = null;

    Object target = null;



    public GenericLoggingProxy(Object target) {
        this.target = target;
    }

    public GenericLoggingProxy(Object target, String sql) {
        this.target = target;
        this.sql = sql;
    }

    public Object getTarget() {
        return this.target;
    }

    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {

        try {
            Object r = method.invoke(target, args);
            if (method.getName().equals("prepareCall") || method.getName().equals("prepareStatement"))
                r = wrap(r, (String) args[0]);
            else
                r = wrap(r, null);
            return r;
        } catch (Throwable t) {
            LogUtils.handleException(t, ConnectionLogger.getLogger()
                    , LogUtils.createLogEntry(method, null, null, null));
        }
        return null;
    }

    private Object wrap(Object target, String sql) throws Exception {
        if (target instanceof Connection) {
            Connection con = (Connection) target;
            if (ConnectionLogger.isInfoEnabled())
                ConnectionLogger.info("connect to URL " + con.getMetaData().getURL() + " for user "
                        + con.getMetaData().getUserName());
            return wrapByGenericProxy(target, Connection.class, sql);
        }
        if (target instanceof CallableStatement)
            return wrapByCallableStatementProxy(target, sql);
        if (target instanceof PreparedStatement)
            return wrapByPreparedStatementProxy(target, sql);
        if (target instanceof Statement)
            return wrapByStatementProxy(target);
        if (target instanceof ResultSet)
            return ResultSetLoggingProxy.wrapByResultSetProxy((ResultSet) target);
        return target;
    }

    private Object wrapByStatementProxy(Object r) {
        Object wrapped = Proxy.newProxyInstance(r.getClass().getClassLoader(), new Class[]{Statement.class},
                new StatementLoggingProxy((Statement) r));
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("sql", sql);
        return wrapBy(wrapped, new Class[]{PreparedStatement.class}, getProxyClassesFor("Statement"), context);
    }

    private Object wrapByPreparedStatementProxy(Object target, String sql) {
        Object wrapped = Proxy.newProxyInstance(target.getClass().getClassLoader(), new Class[]{PreparedStatement.class},
                new PreparedStatementLoggingProxy((PreparedStatement) target, sql));
        // one more level
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("sql", sql);
        return wrapBy(wrapped, new Class[]{PreparedStatement.class}, getProxyClassesFor("PreparedStatement"), context);
    }

    private List<Class> getProxyClassesFor(String type) {
        return ConfigurationParameters.proxyClassesForTypes.get(type);

    }

    private Object wrapByCallableStatementProxy(Object r, String sql) {
        Object wrapped = Proxy.newProxyInstance(r.getClass().getClassLoader(), new Class[]{CallableStatement.class},
                new CallableStatementLoggingProxy((CallableStatement) r, sql));
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("sql", sql);
        return wrapBy(wrapped, new Class[]{PreparedStatement.class}, getProxyClassesFor("CallableStatement"), context);
    }

    static Object wrapByGenericProxy(Object r, Class interf, String sql) {
        return Proxy.newProxyInstance(r.getClass().getClassLoader(), new Class[] { interf },
                new GenericLoggingProxy(r, sql));
    }

    static Object wrapBy(Object target, Class[] interfaces, List<Class> proxyClasses, Map<String, Object> context) {
        if (proxyClasses == null) return target;
        Object wrapped = target;
        for (Class<?> proxyClass : proxyClasses) {
            try {
                Object instance = null;
                Constructor constructor = proxyClass.getConstructor(Object.class, Map.class);
                if (constructor != null) {
                    instance = constructor.newInstance(target, context);
                } else {
                    constructor = proxyClass.getConstructor(Object.class);
                    if (constructor != null) {
                        instance = constructor.newInstance(target);
                    }
                }
                if (instance == null) {
                    logger.error("Cannot find suitable constructor for class " + proxyClass.getName());
                    return target;
                }

                wrapped = Proxy.newProxyInstance(target.getClass().getClassLoader(), interfaces,
                        (InvocationHandler) instance);
            } catch (Exception e) {
                logger.error("Failed to wrap " + target + " with " + proxyClass.getName() + " proxy", e);
                return target;
            }
        }
        return wrapped;
    }

}
