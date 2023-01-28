package com.cxj.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DB {
    public static Map<String, Set<Long>> perms = new HashMap<>();
    public static Connection connection;

    static {
        perms.put("py", new HashSet<>());
        perms.put("js", new HashSet<>());
        perms.put("sh", new HashSet<>());
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:data.sqlite");
            Statement statement = connection.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS PERMS " +
                    "(ID INTEGER PRIMARY KEY autoincrement NOT NULL," +
                    "COMMAND_TYPE TEXT NOT NULL," +
                    "USER_ID BIGINT NOT NULL)";
            statement.executeUpdate(sql);
            String findAllPerms = "SELECT * from PERMS";
            ResultSet rs = statement.executeQuery(findAllPerms);
            while(rs.next()) {
                int id = rs.getInt("ID");
                String commandType = rs.getString("COMMAND_TYPE");
                long userId = rs.getLong("USER_ID");

                if(!perms.containsKey(commandType)) {
                    perms.put(commandType, new HashSet<>());
                }
                perms.get(commandType).add(userId);
            }
            statement.close();
        }catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public static boolean existPerm(String commandType, long userId) {
        try {
            Statement statement = connection.createStatement();
            String sql = "SELECT * FROM PERMS WHERE COMMAND_TYPE='" + commandType +"' and USER_ID=" + userId;
            ResultSet resultSet = statement.executeQuery(sql);
            if(resultSet.next()) {
                return true;
            }
            resultSet.close();
            statement.close();
            return false;
        }catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        return false;
    }
    public static void addPerm(String commandType, long userId) {
        if(existPerm(commandType, userId)) {
            return;
        }
        perms.get(commandType).add(userId);
        try {
            Statement statement = connection.createStatement();
            String sql = "INSERT INTO PERMS(COMMAND_TYPE, USER_ID) VALUES ('" + commandType + "', " + userId + ");";
            statement.executeUpdate(sql);
            statement.close();
        }catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
    public static void removePerm(String commandType, long userId) {
        if(!existPerm(commandType, userId)) {
            return;
        }
        perms.get(commandType).remove(userId);
        try {
            Statement statement = connection.createStatement();
            String sql = "DELETE FROM PERMS WHERE COMMAND_TYPE='" + commandType +"' and USER_ID=" + userId;
            statement.executeUpdate(sql);
            statement.close();
        }catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
}
