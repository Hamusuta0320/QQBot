package com.cxj.plugins.commandExecutor;

import com.cxj.config.GlobalConfig;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Processor {
    public static String getConfigDir(String configDir, String OS) {
        if(configDir == null) {
            if(OS.startsWith("Windows")) {
                configDir = System.getenv("USERPROFILE");
            } else {
                configDir = System.getenv("HOME");
            }
        }
        return configDir;
    }
    public static String runPyByRuntime(String code, String configDir) throws Exception {
        String id = UUID.randomUUID().toString().replace("-", "");
        String OS = System.getProperty("os.name");
        String filename = id + ".py";
        configDir = getConfigDir(configDir, OS);
        Path of = Path.of(configDir, filename);
        try (FileWriter fileWriter = new FileWriter(of.toFile())) {
            fileWriter.write(code);
            fileWriter.flush();
        }
        Process process;
        String r;
        try{
            process = Runtime.getRuntime().exec(String.format("python %s", filename), null, new File(configDir));
            r = readResult(process, 30, "py");
        } finally {
            Files.delete(of);
        }
        return r;
    }


    public static String runJsByRuntime(String code, String configDir) throws Exception {
        String id = UUID.randomUUID().toString().replace("-", "");
        String OS = System.getProperty("os.name");
        String filename = id + ".js";
        configDir = getConfigDir(configDir, OS);
        Path of = Path.of(configDir, filename);
        try (FileWriter fileWriter = new FileWriter(of.toFile())) {
            fileWriter.write(code);
            fileWriter.flush();
        }

        Process process;
        String result;
        try {
            process = Runtime.getRuntime().exec(String.format("node %s", filename), null, new File(configDir));
            result = readResult(process, 30, "js");
        } finally {
            Files.delete(of);
        }
        return result;
    }

    public static String runShellByRuntime(String code, String configDir) throws Exception {
        String id = UUID.randomUUID().toString().replace("-", "");
        String OS = System.getProperty("os.name");
        String executable;
        String filename;
        if(OS.startsWith("Windows")) {
            filename = id + ".bat";
            executable = "";
        } else {
            filename = id + ".sh";
            executable = "sh";
        }
        configDir = getConfigDir(configDir, OS);
        Path of = Path.of(configDir, filename);
        try (FileWriter fileWriter = new FileWriter(of.toFile())) {
            fileWriter.write(code);
            fileWriter.flush();
        }
        Process process;
        String result;
        try {
            process = Runtime.getRuntime().exec(String.format("%s %s", executable, of), null, new File(configDir));
            result = readResult(process, 30, "sh");
        }finally {
            Files.delete(of);
        }
        return result;
    }

    public static String readResult(Process process, long timeout, String commandType) throws Exception {
//        Charset use = System.getProperty("os.name").startsWith("Windows") ? Charset.forName("GBK") : StandardCharsets.UTF_8;
        Charset use = StandardCharsets.UTF_8;

        if(Objects.equals(commandType, "sh") && GlobalConfig.INSTANCE.getOS().startsWith("Windows")) {
            use = Charset.forName("GBK");
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream(), use));
        StringBuilder stringBuffer = new StringBuilder();
        BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream(), use));
        StringBuilder errBuffer = new StringBuilder();

        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<?> submit = service.submit(() -> {
            String line;
            try {
                while ((line = in.readLine()) != null) {
                    System.out.println("输出:" + line);
                    stringBuffer.append(line).append("\n");
                }
                while ((line = err.readLine()) != null) {
                    System.out.println("错误:" + line);
                    errBuffer.append(line).append("\n");
                }
                System.out.println("read over");
            } catch (Exception e) {
                errBuffer.append(e.getMessage()).append("\n");
            }
            System.out.println("read over2");
        });

        try {
            submit.get(timeout == 0 ? 5 : timeout, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            if(process.isAlive()) {
                process.destroyForcibly();
                System.out.println("destroy");
            }
        } finally {
            System.out.println("wait close");
            service.shutdown();
//            in.close();
//            err.close();
            System.out.println("close");
        }
        System.out.println("return data");
        // TODO: 2022/11/7 这里不能超过5000
        if(!errBuffer.isEmpty()) {
            throw new Exception(errBuffer.toString().trim());
        } else if(!stringBuffer.isEmpty()) {
            return stringBuffer.toString().trim();
        }
        return "该命令没有输出";
    }
}
